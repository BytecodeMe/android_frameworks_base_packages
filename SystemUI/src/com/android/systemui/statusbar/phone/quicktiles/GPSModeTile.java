package com.android.systemui.statusbar.phone.quicktiles;

import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.View;

import com.android.internal.view.RotationPolicy;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

import java.util.Observable;
import java.util.Observer;

public class GPSModeTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	private ContentQueryMap mContentQueryMap;
	private Observer mSettingsObserver;

	public GPSModeTile(Context context, View view) {
		super(context, view);

		Cursor settingsCursor = mContext.getContentResolver().query(
				Settings.Secure.CONTENT_URI, null,
				"(" + Settings.System.NAME + "=?)",
				new String[] { Settings.Secure.LOCATION_PROVIDERS_ALLOWED },
				null);
		mContentQueryMap = new ContentQueryMap(settingsCursor,
				Settings.System.NAME, true, null);
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);

		if (mSettingsObserver == null) {
			mSettingsObserver = new Observer() {
				public void update(Observable o, Object arg) {
					updateGUI();
				}
			};
			mContentQueryMap.addObserver(mSettingsObserver);
		}

		updateGUI();
	}

	@Override
	public void onClick(View v) {
		Settings.Secure.setLocationProviderEnabled(
				mContext.getContentResolver(), LocationManager.GPS_PROVIDER,
				!isGPSEnabled());
	}

	@Override
	public boolean onLongClick(View v) {
		launchActivity(new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
		return true;
	}

	private void updateGUI() {

		if (isGPSEnabled()) {
			mTextView.setText(R.string.quick_settings_gps_on);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_location, 0, 0);
		} else {
			mTextView.setText(R.string.quick_settings_gps_off);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_location_off, 0, 0);
		}
	}

	public boolean isGPSEnabled() {
		return Settings.Secure.isLocationProviderEnabled(
				mContext.getContentResolver(), LocationManager.GPS_PROVIDER);
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		if (mSettingsObserver != null) {
			mContentQueryMap.deleteObserver(mSettingsObserver);
		}
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}
}
