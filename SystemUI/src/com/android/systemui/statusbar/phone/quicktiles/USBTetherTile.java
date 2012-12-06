package com.android.systemui.statusbar.phone.quicktiles;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class USBTetherTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {
	private static final String TAG = "QuickSettings.Tether";

	private boolean mStartingTether = false;
	private Context mContext;
	private ConnectivityManager mCm;
	private String[] mUsbRegexs;

	private boolean mUsbConnected;
	private boolean mMassStorageActive;
	private boolean mEnabled = false;

	private BroadcastReceiver mTetherChangeReceiver;

	public USBTetherTile(Context context, View view) {
		super(context, view);
		mContext = context;
		init();
	}

	@Override
	public void init() {
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);
		
		mTag = TAG;

		mCm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		mUsbRegexs = mCm.getTetherableUsbRegexs();
		final boolean usbAvailable = mUsbRegexs.length != 0;
		if (!usbAvailable || ActivityManager.isUserAMonkey()) {
			mContentView.setEnabled(false);
		}

		mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment
				.getExternalStorageState());
		mTetherChangeReceiver = new TetherChangeReceiver();
		IntentFilter filter = new IntentFilter(
				ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
		Intent intent = mContext
				.registerReceiver(mTetherChangeReceiver, filter);

		filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_STATE);
		mContext.registerReceiver(mTetherChangeReceiver, filter);

		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_SHARED);
		filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
		filter.addDataScheme("file");
		mContext.registerReceiver(mTetherChangeReceiver, filter);

		if (intent != null)
			mTetherChangeReceiver.onReceive(mContext, intent);


		updateState();

	}

	@Override
	public boolean onLongClick(View v) {
		launchActivity(new Intent(Intent.ACTION_MAIN).setClassName(
				"com.android.settings",
				"com.android.settings.Settings$TetherSettingsActivity"));
		return true;
	}

	public void onClick(View v) {
		setUsbTethering(!mEnabled);
	}

	private void setUsbTethering(boolean enabled) {
		mEnabled = !enabled;
		mStartingTether = enabled;
		
		if (mCm.setUsbTethering(enabled) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
			mEnabled = false;
			mStartingTether = false;
			return;
		}
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

	private class TetherChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context content, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
				// TODO - this should understand the interface types
				ArrayList<String> available = intent
						.getStringArrayListExtra(ConnectivityManager.EXTRA_AVAILABLE_TETHER);
				ArrayList<String> active = intent
						.getStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER);
				ArrayList<String> errored = intent
						.getStringArrayListExtra(ConnectivityManager.EXTRA_ERRORED_TETHER);
				updateState(available.toArray(new String[available.size()]),
						active.toArray(new String[active.size()]),
						errored.toArray(new String[errored.size()]));
			} else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
				mMassStorageActive = true;
				updateState();
			} else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
				mMassStorageActive = false;
				updateState();
			} else if (action.equals(UsbManager.ACTION_USB_STATE)) {
				mUsbConnected = intent.getBooleanExtra(
						UsbManager.USB_CONNECTED, false);
				updateState();
			}
		}
	}

	private void updateState() {

		String[] available = mCm.getTetherableIfaces();
		String[] tethered = mCm.getTetheredIfaces();
		String[] errored = mCm.getTetheringErroredIfaces();
		updateState(available, tethered, errored);
	}

	private void updateState(String[] available, String[] tethered,
			String[] errored) {
		updateUsbState(available, tethered, errored);
	}

	private void updateUsbState(String[] available, String[] tethered,
			String[] errored) {

		boolean usbAvailable = mUsbConnected && !mMassStorageActive;
		int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
		for (String s : available) {
			for (String regex : mUsbRegexs) {
				if (s.matches(regex)) {
					if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
						usbError = mCm.getLastTetherError(s);
					}
				}
			}
		}
		boolean usbTethered = false;
		for (String s : tethered) {
			for (String regex : mUsbRegexs) {
				if (s.matches(regex))
					usbTethered = true;
			}
		}
		boolean usbErrored = false;
		for (String s : errored) {
			for (String regex : mUsbRegexs) {
				if (s.matches(regex))
					usbErrored = true;
			}
		}

		if (usbTethered) {
			mStartingTether = false;
			mEnabled = true;
			mTextView.setText(R.string.usb_tethering_active_subtext);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_usb_device, 0, 0);
		} else if (usbAvailable) {
			if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
				mTextView.setText(R.string.usb_tethering_available_subtext);
				mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.ic_qs_usb_device_off, 0, 0);
			} else {
				mTextView.setText(R.string.usb_tethering_errored_subtext);
				mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.ic_qs_usb_device_off, 0, 0);
			}
			if (!mStartingTether) {
				mEnabled = false;
			}
		} else if (usbErrored) {
			mEnabled = false;
			mTextView.setText(R.string.usb_tethering_errored_subtext);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_usb_device_off, 0, 0);
		} else if (mMassStorageActive) {
			mEnabled = false;
			mTextView.setText(R.string.usb_tethering_storage_active_subtext);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_usb_device_off, 0, 0);
		} else {
			mEnabled = false;
			mTextView.setText(R.string.usb_tethering_unavailable_subtext);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
					R.drawable.ic_qs_usb_device_off, 0, 0);
		}
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mTetherChangeReceiver);
		mTetherChangeReceiver = null;

	}

}
