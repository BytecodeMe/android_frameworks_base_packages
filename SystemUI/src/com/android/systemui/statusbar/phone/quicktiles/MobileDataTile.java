package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class MobileDataTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	private final static String TAG = MobileDataTile.class.getSimpleName();
	private ConnectivityManager mConnManager;
	private boolean mDataMode = false;

	public MobileDataTile(Context context, View view) {
		super(context, view);
		mConnManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		mDataMode = mConnManager.getMobileDataEnabled();
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);
		mTag = TAG;

		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mReceiver, filter);

		updateGUI();
	}

	@Override
	public void onClick(View v) {
		mContentView.setEnabled(false);
		mTextView.setText(mDataMode ? "Disabling..." : "Enabling...");
		mConnManager.setMobileDataEnabled(!mDataMode);
	}

	@Override
	public boolean onLongClick(View v) {
		launchActivity(new Intent(Intent.ACTION_MAIN).setClassName(
				"com.android.settings",
				"com.android.settings.Settings$DataUsageSummaryActivity"));
		return true;
	}

	private void updateGUI() {
		mContentView.setEnabled(true);
		mTextView.setText(mDataMode ? R.string.quick_settings_mobile_data_on
				: R.string.quick_settings_mobile_data_off);
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				mDataMode ? R.drawable.ic_qs_mobile_data_on
						: R.drawable.ic_qs_mobile_data_off, 0, 0);
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mDataMode = mConnManager.getMobileDataEnabled();
			updateGUI();

		}

	};

}
