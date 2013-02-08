package com.android.systemui.statusbar.phone.quicktiles;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;
import com.android.systemui.statusbar.policy.SkinHelper;

public class WifiTile extends QuickSettingsTileContent implements
		NetworkSignalChangedCallback, View.OnClickListener,
		View.OnLongClickListener {

	private WifiManager mWifiManager;
	private WifiState mWifiState = new WifiState();

	public WifiTile(Context context, View view) {
		super(context, view);
		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);
	}

	@Override
	public void release() {

	}

	@Override
	public void refreshResources() {
		updateGUI(mWifiState);
	}

	@Override
	public void onClick(View v) {
		if (mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(false);
		} else {
			mWifiManager.setWifiEnabled(true);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		launchActivity(new Intent(
				android.provider.Settings.ACTION_WIFI_SETTINGS));
		return true;
	}

	private void updateGUI(State state) {
		WifiState wifiState = (WifiState) state;
		mTextView.setCompoundDrawablesWithIntrinsicBounds(null, SkinHelper.getIconDrawable
				(mContext, wifiState.iconId,Settings.System.CUSTOM_SIGNAL_PACKAGE),	null, null);
		mTextView.setText(wifiState.label);
		mContentView.setContentDescription(mContext.getString(
				R.string.accessibility_quick_settings_wifi,
				wifiState.signalContentDescription,
				(wifiState.connected) ? wifiState.label : ""));
	}

	// Remove the double quotes that the SSID may contain
	public static String removeDoubleQuotes(String string) {
		if (string == null)
			return null;
		final int length = string.length();
		if ((length > 1) && (string.charAt(0) == '"')
				&& (string.charAt(length - 1) == '"')) {
			return string.substring(1, length - 1);
		}
		return string;
	}

	// Remove the period from the network name
	public static String removeTrailingPeriod(String string) {
		if (string == null)
			return null;
		final int length = string.length();
		if (string.endsWith(".")) {
			string.substring(0, length - 1);
		}
		return string;
	}

	// NetworkSignalChanged callback
	@Override
	public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
			String wifiSignalContentDescription, String enabledDesc) {
		// TODO: If view is in awaiting state, disable
		Resources r = mContext.getResources();

		boolean wifiConnected = enabled && (wifiSignalIconId > 0)
				&& (enabledDesc != null);
		boolean wifiNotConnected = (wifiSignalIconId > 0)
				&& (enabledDesc == null);
		mWifiState.enabled = enabled;
		mWifiState.connected = wifiConnected;
		if (wifiConnected) {
			mWifiState.iconId = wifiSignalIconId;
			mWifiState.label = removeDoubleQuotes(enabledDesc);
			mWifiState.signalContentDescription = wifiSignalContentDescription;
		} else if (wifiNotConnected) {
			mWifiState.iconId = R.drawable.ic_qs_wifi_0;
			mWifiState.label = r.getString(R.string.quick_settings_wifi_label);
			mWifiState.signalContentDescription = r
					.getString(R.string.accessibility_no_wifi);
		} else {
			mWifiState.iconId = R.drawable.ic_qs_wifi_no_network;
			mWifiState.label = r
					.getString(R.string.quick_settings_wifi_off_label);
			mWifiState.signalContentDescription = r
					.getString(R.string.accessibility_wifi_off);
		}
		updateGUI(mWifiState);
	}

	@Override
	public void onMobileDataSignalChanged(boolean enabled,
			int mobileSignalIconId, String mobileSignalContentDescriptionId,
			int dataTypeIconId, String dataTypeContentDescriptionId,
			String description) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAirplaneModeChanged(boolean enabled) {
		// TODO Auto-generated method stub

	}

	static class WifiState extends State {
		String signalContentDescription;
		boolean connected;
	}
}
