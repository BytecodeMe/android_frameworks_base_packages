/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class BatteryController extends BroadcastReceiver {
	private static final String TAG = "StatusBar.BatteryController";
	private static final int CHARGING_TEXT_DISABLE = 0;
	private static final int CHARGING_TEXT_SHOW = 1;
	private static final int CHARGING_TEXT_ALTERNATE = 2;

	private Context mContext;
	private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
	private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();
	private TextView mBatteryText;
	private ArrayList<BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<BatteryStateChangeCallback>();

	public interface BatteryStateChangeCallback {
		public void onBatteryLevelChanged(int level, boolean pluggedIn);
	}

	private int mOldLevel = 0;
	private boolean mOldPlugged = false;
	private boolean mAnimating = false;
	private boolean mThreadExited = true;

	public BatteryController(Context context) {
		mContext = context;

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(Intent.ACTION_BATTERY_ICON_CHANGED);
		context.registerReceiver(this, filter);
	}

	public void addIconView(ImageView v) {
		mIconViews.add(v);
	}

	public void addLabelView(TextView v) {
		mLabelViews.add(v);
	}

	public void clearViews() {
		mIconViews.clear();
		mLabelViews.clear();
	}

	public void addStateChangedCallback(BatteryStateChangeCallback cb) {
		mChangeCallbacks.add(cb);
		cb.onBatteryLevelChanged(mOldLevel, mOldPlugged);
	}
	
	public void removeStateChangedCallback(BatteryStateChangeCallback cb){
		mChangeCallbacks.remove(cb);
	}

	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
			final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			mOldLevel = level;
			final boolean plugged = intent.getIntExtra(
					BatteryManager.EXTRA_PLUGGED, 0) != 0;
			mOldPlugged = plugged;
		}
		// stop the animation if the user unplugs the phone or changes a setting
		if (!mOldPlugged || action.equals(Intent.ACTION_BATTERY_ICON_CHANGED)) {
			mAnimating = false;
		}
		for (BatteryStateChangeCallback cb : mChangeCallbacks) {
			cb.onBatteryLevelChanged(mOldLevel, mOldPlugged);
		}
		refreshBattery();
	}

	private void refreshBattery() {
		boolean showText = Settings.System.getInt(
				mContext.getContentResolver(),
				Settings.System.SHOW_BATTERY_TEXT, 0) == 1;
		// for showCharge, 0 = don't show it, 1 = show always, 2 = alternate
		int showCharge = Settings.System.getInt(mContext.getContentResolver(),
				Settings.System.BATTERY_TEXT_SHOW_CHARGE, 0);
		int textColor = Settings.System.getInt(mContext.getContentResolver(),
				Settings.System.BATTERY_TEXT_COLOR, Color.WHITE);

		final Drawable icon = getIconDrawable(mOldPlugged);
		int N = mIconViews.size();
		for (int i = 0; i < N; i++) {
			ImageView v = mIconViews.get(i);
			v.setImageDrawable(icon);
			v.setImageLevel(mOldLevel);
			v.setContentDescription(mContext.getString(
					R.string.accessibility_battery_level, mOldLevel));
		}
		N = mLabelViews.size();
		for (int i = 0; i < N; i++) {
			mBatteryText = mLabelViews.get(i);
			mBatteryText.setTextColor(textColor);
			if (!isTablet()) {
				mBatteryText.setVisibility(View.VISIBLE);
				if (mOldPlugged && (showCharge != CHARGING_TEXT_DISABLE)
						&& showText) {
					if (showCharge == CHARGING_TEXT_SHOW) {
						mBatteryText
								.setText(mContext
										.getString(
												R.string.status_bar_settings_battery_meter_format_phone,
												mOldLevel));
						mBatteryText.setBackground(null);
					} else if (showCharge == CHARGING_TEXT_ALTERNATE) {
						if (!mAnimating) {
							animateCharging();
						}
					}
				} else if (mOldPlugged && (showCharge == CHARGING_TEXT_DISABLE)
						&& showText) {
					mBatteryText.setText("");
					mBatteryText.setBackground(getChargingIndicator());
				} else if (showText) {
					mBatteryText
							.setText(mContext
									.getString(
											R.string.status_bar_settings_battery_meter_format_phone,
											mOldLevel));
					mBatteryText.setBackground(null);
				} else if (mOldPlugged) {
					mBatteryText.setText("");
					mBatteryText.setBackground(getChargingIndicator());
				} else {
					mBatteryText.setText("");
					mBatteryText.setBackground(null);
				}
				setTextPadding();
			} else {
				mBatteryText.setText(mContext.getString(
						R.string.status_bar_settings_battery_meter_format,
						mOldLevel));
			}
		}
	}

	private boolean isTablet() {
		// TODO Auto-generated method stub
		return false;
	}

	private void setTextPadding() {

		int left, right, top, bottom;
		Resources res = null;
		String packageName = Settings.System.getString(
				mContext.getContentResolver(),
				Settings.System.CUSTOM_BATTERY_PACKAGE);

		if (packageName != null && !packageName.isEmpty()) {
			try {
				res = mContext.getPackageManager().getResourcesForApplication(
						packageName);
			} catch (Exception e) {
				Log.w(TAG, "Error Resolving custom battery package: "
						+ packageName);
				Settings.System.putString(mContext.getContentResolver(),
						Settings.System.CUSTOM_BATTERY_PACKAGE, "");
			}
		}
		if (res != null) {
			try {
				left = res.getInteger(res.getIdentifier(
						"config_batteryPaddingLeft", "integer", packageName));
				right = res.getInteger(res.getIdentifier(
						"config_batteryPaddingRight", "integer", packageName));
				top = res.getInteger(res.getIdentifier(
						"config_batteryPaddingTop", "integer", packageName));
				bottom = res.getInteger(res.getIdentifier(
						"config_batteryPaddingBottom", "integer", packageName));
				mBatteryText.setPadding(left, top, right, bottom);
			} catch (Exception e) {
				mBatteryText.setPadding(0, 0, 0, 0);
			}
		} else {
			mBatteryText.setPadding(0, 0, 0, 0);
		}
	}

	private Drawable getIconDrawable(boolean plugged) {

		int resource = plugged ? R.drawable.stat_sys_battery_charge
				: R.drawable.stat_sys_battery;

		return SkinHelper.getIconDrawable(mContext, resource,
				Settings.System.CUSTOM_BATTERY_PACKAGE);
	}

	private Drawable getChargingIndicator() {
		return SkinHelper.getIconDrawable(mContext,
				R.drawable.stat_sys_battery_charge_indicator,
				Settings.System.CUSTOM_BATTERY_PACKAGE);
	}

	private void animateCharging() {
		// added for possible future setting;
		final int TEXT_TIME = 2000;
		final int ICON_TIME = 2000;

		final Handler h = new Handler();
		final Runnable showText = new Runnable() {
			public void run() {
				mBatteryText
						.setText(mContext
								.getString(
										R.string.status_bar_settings_battery_meter_format_phone,
										mOldLevel));
				mBatteryText.setBackground(null);
			}
		};
		final Runnable showCharge = new Runnable() {
			public void run() {
				mBatteryText.setText("");
				mBatteryText.setBackground(getChargingIndicator());
			}
		};
		final Runnable refresh = new Runnable() {
			public void run() {
				refreshBattery();
			}
		};

		// this is not the cleanest way, but we
		// don't want several threads running
		while (!mThreadExited) {
			// wait
		}

		new Thread() {
			@Override
			public void run() {
				mAnimating = true;
				mThreadExited = false;
				while (mAnimating) {
					try {
						h.post(showCharge);
						sleep(ICON_TIME);
						h.post(showText);
						sleep(TEXT_TIME);
					} catch (Exception e) {
						Log.w(TAG,
								"Something happened while animating the charge");
					}
				}
				mThreadExited = true;
				// we need to call this so the battery is proper after the
				// thread finishes
				h.post(refresh);
			}
		}.start();
	}
}
