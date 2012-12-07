package com.android.systemui.statusbar.phone.quicktiles;

import com.android.systemui.statusbar.phone.QuickSettingsTileContent;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView.ScaleType;

public class BatteryTile extends QuickSettingsTileContent implements
		View.OnClickListener, BatteryStateChangeCallback {

	private BatteryState mBatteryState = new BatteryState();
	private LevelListDrawable mBatteryLevels;
	private LevelListDrawable mChargingBatteryLevels;

	public BatteryTile(Context context, View view) {
		super(context, view);
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mBatteryImageView.setVisibility(View.VISIBLE);

		Resources r = mContext.getResources();
		mBatteryLevels = (LevelListDrawable) r
				.getDrawable(R.drawable.qs_sys_battery);
		mChargingBatteryLevels = (LevelListDrawable) r
				.getDrawable(R.drawable.qs_sys_battery_charging);

	}

	private void updateGUI(State state) {
		BatteryState batteryState = (BatteryState) state;

		Drawable d = batteryState.pluggedIn ? mChargingBatteryLevels
				: mBatteryLevels;
		String t;
		if (batteryState.batteryLevel == 100) {
			t = mContext
					.getString(R.string.quick_settings_battery_charged_label);
		} else {
			t = batteryState.pluggedIn ? mContext.getString(
					R.string.quick_settings_battery_charging_label,
					batteryState.batteryLevel) : mContext.getString(
					R.string.status_bar_settings_battery_meter_format,
					batteryState.batteryLevel);
		}
		mBatteryImageView.setImageDrawable(d);
		mBatteryImageView.setImageLevel(batteryState.batteryLevel);
		mTextView.setText(t);
		mContentView.setContentDescription(mContext.getString(
				R.string.accessibility_quick_settings_battery, t));
	}

	// BatteryController callback
	@Override
	public void onBatteryLevelChanged(int level, boolean pluggedIn) {
		mBatteryState.batteryLevel = level;
		mBatteryState.pluggedIn = pluggedIn;
		updateGUI(mBatteryState);
	}

	static class BatteryState extends State {
		int batteryLevel;
		boolean pluggedIn;
	}

	@Override
	public void onClick(View v) {
		launchActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshResources() {
		updateGUI(mBatteryState);
	}

}
