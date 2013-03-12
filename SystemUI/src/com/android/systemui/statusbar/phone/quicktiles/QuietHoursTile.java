package com.android.systemui.statusbar.phone.quicktiles;

import java.util.Calendar;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class QuietHoursTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	private boolean mEnabled;
	private boolean mActive;
	private int mQuietHoursStart = 0;
	private int mQuietHoursEnd = 0;

	public QuietHoursTile(Context context, View view) {
		super(context, view);
		init();
	}

	@Override
	public boolean onLongClick(View v) {
		openSettings();
		return true;
	}

	private void openSettings() {
		mContext.startActivity(new Intent(
				"com.bamf.settings.notificationmanager")
				.addCategory(Intent.CATEGORY_DEFAULT)
				.putExtra("quiet_hours", true)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		getStatusBarManager().collapsePanels();
	}

	@Override
	public void onClick(View v) {
		Settings.System.putIntForUser(mContext.getContentResolver(),
				Settings.System.QUIET_HOURS_ENABLED, mEnabled ? 0 : 1,
				UserHandle.USER_CURRENT);
		updateGUI();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);
		
		mTextView.setEllipsize(TruncateAt.MARQUEE);
		mTextView.setMarqueeRepeatLimit(-1);
		mTextView.setHorizontallyScrolling(true);
		mTextView.setSelected(true);
		
		updateGUI();
	}

	private void updateGUI() {
		final ContentResolver resolver = mContext.getContentResolver();

		mEnabled = Settings.System
				.getIntForUser(resolver, Settings.System.QUIET_HOURS_ENABLED,
						0, UserHandle.USER_CURRENT) == 1;
		mQuietHoursStart = Settings.System.getIntForUser(resolver,
				Settings.System.QUIET_HOURS_START, 0,
				UserHandle.USER_CURRENT_OR_SELF);
		mQuietHoursEnd = Settings.System.getIntForUser(resolver,
				Settings.System.QUIET_HOURS_END, 0,
				UserHandle.USER_CURRENT_OR_SELF);

		mActive = inQuietHours();

		if (mEnabled && mActive) {
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_quiet_hours_on, 0, 0);
			mTextView.setText(getQuietHours());
		} else if (mEnabled && !mActive) {
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_quiet_hours_off, 0, 0);
			mTextView.setText(getQuietHours());
		} else {
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_quiet_hours_off, 0, 0);
			mTextView.setText(R.string.quick_settings_quiethours_off);
		}

	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
	}

	@Override
	public void refreshResources() {
		updateGUI();
	}

	private boolean inQuietHours() {
		if (mEnabled && (mQuietHoursStart != mQuietHoursEnd)) {
			// Get the date in "quiet hours" format.
			Calendar calendar = Calendar.getInstance();
			int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60
					+ calendar.get(Calendar.MINUTE);
			if (mQuietHoursEnd < mQuietHoursStart) {
				// Starts at night, ends in the morning.
				return (minutes > mQuietHoursStart)
						|| (minutes < mQuietHoursEnd);
			} else {
				return (minutes > mQuietHoursStart)
						&& (minutes < mQuietHoursEnd);
			}
		}
		return false;
	}

	private String getQuietHours() {

		final ContentResolver resolver = mContext.getContentResolver();
		String activeHours;
		activeHours = returnTime(String.valueOf(Settings.System.getIntForUser(
				resolver, Settings.System.QUIET_HOURS_START, 0,
				UserHandle.USER_CURRENT)))
				+ " - "
				+ returnTime(String.valueOf(Settings.System.getIntForUser(
						resolver, Settings.System.QUIET_HOURS_END, 0,
						UserHandle.USER_CURRENT)));
		return activeHours;
	}

	private String returnTime(String t) {
		if (t == null || t.equals("")) {
			return "";
		}
		int hr = Integer.parseInt(t.trim());
		int mn = hr;

		hr = hr / 60;
		mn = mn % 60;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hr);
		cal.set(Calendar.MINUTE, mn);
		Date date = cal.getTime();
		return DateFormat.getTimeFormat(mContext.getApplicationContext())
				.format(date);
	}

}
