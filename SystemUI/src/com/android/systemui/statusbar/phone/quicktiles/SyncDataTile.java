package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.content.SyncStorageEngine;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class SyncDataTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	private final static String TAG = SyncDataTile.class.getSimpleName();
	private boolean mSyncState = false;

	public SyncDataTile(Context context, View view) {
		super(context, view);
		mSyncState = getSyncState();
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);

		mTag = TAG;
		
		updateGUI();

		IntentFilter filter = new IntentFilter();
		filter.addAction(SyncStorageEngine.SYNC_CONNECTION_SETTING_CHANGED_INTENT
				.getAction());
		mContext.registerReceiver(mSyncStateReceiver, filter);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onCheckChanged: isChecked=" + mSyncState + ", mSyncState="
				+ mSyncState);
		ContentResolver.setMasterSyncAutomatically(!mSyncState);
	}

	@Override
	public boolean onLongClick(View v) {
		launchActivity(new Intent(
				android.provider.Settings.ACTION_SYNC_SETTINGS));
		return true;
	}

	private void updateGUI() {
		if (mSyncState) {
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_sync_on, 0, 0);
			mTextView.setText(R.string.quick_settings_sync_on);
		} else {
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_sync_off, 0, 0);
			mTextView.setText(R.string.quick_settings_sync_off);

		}
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mSyncStateReceiver);
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub
	}

	private BroadcastReceiver mSyncStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mSyncState = getSyncState();
			updateGUI();
		}

	};

	private boolean getSyncState() {
		return ContentResolver.getMasterSyncAutomatically();
	}

}
