package com.android.systemui.statusbar.phone.quicktiles;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class SettingsShortcutTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	public SettingsShortcutTile(Context context, View view) {
		super(context, view);
		init();
	}

	public void init() {
		mContentView.setOnLongClickListener(this);
		mContentView.setOnClickListener(this);

		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				R.drawable.ic_notify_settings_normal, 0, 0);
		mTextView.setText(R.string.quick_settings_settings_label);
	}

	@Override
	public void release() {
	}

	@Override
	public void onClick(View v) {
		launchActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
	}

	@Override
	public boolean onLongClick(View v) {
		launchActivity(new Intent(Intent.ACTION_MAIN).setClassName(
				"com.bamf.settings",
				"com.bamf.settings.activities.SettingsActivity"));
		return true;
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

}
