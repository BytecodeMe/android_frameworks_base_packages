package com.android.systemui.statusbar.phone.quicktiles;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;
import com.android.systemui.statusbar.policy.SkinHelper;

public class SignalTile extends QuickSettingsTileContent implements
		View.OnClickListener, NetworkSignalChangedCallback {

	private RSSIState mRSSIState = new RSSIState();
	private State mWifiState = new State();

	public SignalTile(Context context, View view) {
		super(context, view);
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		
		mTextView.setVisibility(View.GONE);
		mRSSILayout.setVisibility(View.VISIBLE);
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshResources() {
		updateGUI(mRSSIState);
	}

	private void updateGUI(State state) {
        RSSIState rssiState = (RSSIState) state;
        mRSSIImage.setImageDrawable(SkinHelper.getIconDrawable(mContext, 
        		rssiState.signalIconId,Settings.System.CUSTOM_SIGNAL_PACKAGE));

        if (rssiState.dataTypeIconId > 0) {
        	mRSSIImageOverlay.setImageDrawable(SkinHelper.getIconDrawable(mContext, 
        			rssiState.dataTypeIconId,Settings.System.CUSTOM_SIGNAL_PACKAGE));
        } else {
        	mRSSIImageOverlay.setImageDrawable(null);
        }
        
        mRSSITextView.setText(state.label);
        mContentView.setContentDescription(mContext.getResources().getString(
                R.string.accessibility_quick_settings_mobile,
                rssiState.signalContentDescription, rssiState.dataContentDescription,
                state.label));
    }

	@Override
	public void onClick(View v) {
		launchActivity(new Intent(Intent.ACTION_MAIN).setClassName(
				"com.android.settings",
				"com.android.settings.deviceinfo.Status"));
	}

	// NetworkSignalChanged callback
	@Override
	public void onMobileDataSignalChanged(boolean enabled,
			int mobileSignalIconId, String signalContentDescription,
			int dataTypeIconId, String dataContentDescription,
			String enabledDesc) {
		if (deviceSupportsTelephony()) {
			// TODO: If view is in awaiting state, disable
			Resources r = mContext.getResources();
			mRSSIState.signalIconId = enabled && (mobileSignalIconId > 0) ? mobileSignalIconId
					: R.drawable.ic_qs_signal_no_signal;
			mRSSIState.signalContentDescription = enabled
					&& (mobileSignalIconId > 0) ? signalContentDescription : r
					.getString(R.string.accessibility_no_signal);
			mRSSIState.dataTypeIconId = enabled && (dataTypeIconId > 0)
					&& !mWifiState.enabled ? dataTypeIconId : 0;
			mRSSIState.dataContentDescription = enabled && (dataTypeIconId > 0)
					&& !mWifiState.enabled ? dataContentDescription : r
					.getString(R.string.accessibility_no_data);
			mRSSIState.label = enabled ? removeTrailingPeriod(enabledDesc) : r
					.getString(R.string.quick_settings_rssi_emergency_only);
		}
		updateGUI(mRSSIState);
	}

	@Override
	public void onAirplaneModeChanged(boolean enabled) {
		// TODO Auto-generated method stub

	}

	@Override
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
            String wifiSignalContentDescription, String enabledDesc) {
        mWifiState.enabled = enabled;
	}

	boolean deviceSupportsTelephony() {
		PackageManager pm = mContext.getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
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

	static class RSSIState extends State {
		int signalIconId;
		String signalContentDescription;
		int dataTypeIconId;
		String dataContentDescription;
	}

}
