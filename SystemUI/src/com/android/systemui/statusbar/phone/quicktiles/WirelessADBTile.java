package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils.TruncateAt;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class WirelessADBTile extends QuickSettingsTileContent implements
		View.OnClickListener {

	private final static String TAG = WirelessADBTile.class.getSimpleName();
	private final static String PORT = "3700";

	private IntentFilter mIntentFilter = new IntentFilter();
	public boolean mToggleState;
	private ConnectivityManager mConnManager;
	private WifiManager mWifiManager;

	public WirelessADBTile(Context context, View view) {
		super(context, view);
		mConnManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		init();
	}
	
	@Override
	protected void init() {
		mTag = TAG;
		mContentView.setOnClickListener(this);
		
		mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mReceiver, mIntentFilter);
		
		mTextView.setEllipsize(TruncateAt.MARQUEE);
		mTextView.setMarqueeRepeatLimit(-1);
		mTextView.setHorizontallyScrolling(true);
		mTextView.setSelected(true);

		mToggleState = false;
		setText("OFF");
		
		updateGUI();
	}

	@Override
	public void onClick(View v) {
		if (mToggleState) {
			stopAdb();
		} else {
			startAdb();
		}
	}

	private void setText(String text) {
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				mToggleState ? R.drawable.ic_sysbar_adb_on
						: R.drawable.ic_sysbar_adb_off, 0, 0);
		mTextView.setText(text);
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateGUI();
		}

	};

	protected void updateGUI() {

		NetworkInfo active = null;
		boolean isWifiConnected = false;

		try {
			active = mConnManager.getActiveNetworkInfo();
			if (active.getType() == ConnectivityManager.TYPE_WIFI
					&& active.getState().equals(NetworkInfo.State.CONNECTED)) {
				isWifiConnected = true;
			}
		} catch (NullPointerException e) {
		}

		if (isWifiConnected && !mContentView.isEnabled()) {
			enableAdb();
		} else if (!isWifiConnected) {
			disableAdb();
		}

	}

	protected void enableAdb() {
		mContentView.setEnabled(true);
		if (!mToggleState) {
			stopAdb();
		}
		mToggleState = false;
		setText("OFF");
	}

	protected void disableAdb() {
		if (!mToggleState) {
			stopAdb();
		}
		mContentView.setEnabled(false);
		mToggleState = false;
		setText("DISABLED");
	}

	protected void startAdb() {
		// set the port to connect to
		SystemProperties.set("service.adb.tcp.port", PORT);
		// cycle adb off and on
		Settings.Global.putInt(mContext.getContentResolver(),
				Settings.Global.ADB_ENABLED, 0);
		Settings.Global.putInt(mContext.getContentResolver(),
				Settings.Global.ADB_ENABLED, 1);

		mToggleState = true;
		setText(getWifiIp() + ":" + PORT);
	}

	protected void stopAdb() {
		// set the port to -1 (off)
		SystemProperties.set("service.adb.tcp.port", "-1");
		// cycle adb off and on
		Settings.Global.putInt(mContext.getContentResolver(),
				Settings.Global.ADB_ENABLED, 0);
		Settings.Global.putInt(mContext.getContentResolver(),
				Settings.Global.ADB_ENABLED, 1);

		mToggleState = false;
		setText("OFF");
	}

	protected String getWifiIp() {
		return mWifiManager.getConnectionInfo().getStringIpAddress();
	}
}
