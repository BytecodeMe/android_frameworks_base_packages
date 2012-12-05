package com.android.systemui.statusbar.phone.quicktiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Prefs;

public class DoNotDisturbTile extends QuickSettingsTileContent implements
		View.OnClickListener,
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "QuickSettings.DoNotDisturb";

	SharedPreferences mPrefs;

	private boolean mDoNotDisturb;

	public DoNotDisturbTile(Context context, View view) {
		super(context, view);

		mPrefs = Prefs.read(context);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		mDoNotDisturb = mPrefs.getBoolean(Prefs.DO_NOT_DISTURB_PREF,
				Prefs.DO_NOT_DISTURB_DEFAULT);
		init();
	}
	
	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mTextView.setText(R.string.quick_settings_dnd);

		updateGUI();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		mDoNotDisturb = sharedPreferences.getBoolean(
				Prefs.DO_NOT_DISTURB_PREF, Prefs.DO_NOT_DISTURB_DEFAULT);
		updateGUI();
	}

	public void onClick(View v) {
		mDoNotDisturb = !mDoNotDisturb;
		SharedPreferences.Editor editor = Prefs.edit(mContext);
		editor.putBoolean(Prefs.DO_NOT_DISTURB_PREF, mDoNotDisturb);
		editor.apply();
	}
	
	public void updateGUI(){
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				mDoNotDisturb?R.drawable.ic_notification_open:R.drawable.ic_notification_open_on, 0, 0);
	}

	@Override
	public void release() {
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}
}
