package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class AlarmTile extends QuickSettingsTileContent implements
		View.OnClickListener {

	private static final boolean DEBUG = true;
	private static final String TAG = AlarmTile.class.getSimpleName();
	private NextAlarmObserver mNextAlarmObserver;
	private Handler mHandler;
	private State mAlarmState = new State();

	public AlarmTile(Context context, View view) {
		super(context, view);
		init();
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.deskclock",
                "com.android.deskclock.AlarmClock"));
		launchActivity(intent);
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mNextAlarmObserver = new NextAlarmObserver(mHandler);
		mNextAlarmObserver.startObserving();
		
		IntentFilter alarmIntentFilter = new IntentFilter();
		alarmIntentFilter.addAction(Intent.ACTION_ALARM_CHANGED);
		mContext.registerReceiver(mAlarmIntentReceiver, alarmIntentFilter);
		onNextAlarmChanged();
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mAlarmIntentReceiver);
	}

	@Override
	public void refreshResources() {
		onNextAlarmChanged();
	}

	/** Broadcast receive to determine if there is an alarm set. */
	private BroadcastReceiver mAlarmIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_ALARM_CHANGED)) {
				onAlarmChanged(intent);
				onNextAlarmChanged();
			}
		}
	};

	/** ContentObserver to determine the next alarm */
	private class NextAlarmObserver extends ContentObserver {
		public NextAlarmObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			onNextAlarmChanged();
		}

		public void startObserving() {
			final ContentResolver cr = mContext.getContentResolver();
			cr.registerContentObserver(Settings.System
					.getUriFor(Settings.System.NEXT_ALARM_FORMATTED), false,
					this);
		}
	}

	void onAlarmChanged(Intent intent) {
		mAlarmState.enabled = intent.getBooleanExtra("alarmSet", false);
		updateGUI();
	}

	void onNextAlarmChanged() {
		mAlarmState.label = Settings.System.getString(
				mContext.getContentResolver(),
				Settings.System.NEXT_ALARM_FORMATTED);
		mAlarmState.enabled = !mAlarmState.label.equals("");
		if(DEBUG)Log.d(TAG, String.format("label: %s, enabled: %s", mAlarmState.label, mAlarmState.enabled));
		updateGUI();
	}

	private void updateGUI() {

		if (mAlarmState.enabled) {
			mTextView.setText(mAlarmState.label);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_alarm_on, 0, 0);
		} else {
			// this is not really needed but here for completeness
			mTextView.setText(R.string.quick_settings_alarm_off);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_alarm_off, 0, 0);
		}
		mContentView
				.setContentDescription(mContext.getString(
						R.string.accessibility_quick_settings_alarm,
						mAlarmState.label));
		
		// this will hide the tile if it is not enabled
//		if(mCallBack!=null){
//			mCallBack.show(mAlarmState.enabled);
//		}
	}

}
