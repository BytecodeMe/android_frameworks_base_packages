package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;

import com.android.internal.widget.Flashlight;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class TorchTile extends QuickSettingsTileContent implements
		View.OnClickListener {

	private static final String TAG = "QuickSettings.Torch";
	private static final boolean DEBUG = true;

	private boolean mTorchMode;

	public TorchTile(Context context, View view) {
		super(context, view);

		mTorchMode = false;
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);

		IntentFilter filter = new IntentFilter();
		filter.addAction(Flashlight.FLASHLIGHT_STATE_CHANGED_ACTION);
		mContext.registerReceiver(mTorchReceiver, filter);

		updateGUI();
	}

	@Override
	public void onClick(View v) {
		try {
			Intent intent = new Intent();
			intent.setClassName("com.bamf.ics.torch",
					"com.bamf.ics.torch.utils.TorchToggleService");
			intent.putExtra("mode", Flashlight.STATE_TOGGLE);
			mContext.startService(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateGUI() {
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				mTorchMode ? R.drawable.ic_sysbar_torch_on
						: R.drawable.ic_sysbar_torch_off, 0, 0);
		mTextView.setText(mTorchMode ? R.string.quick_settings_torch_on
				: R.string.quick_settings_torch_off);
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mTorchReceiver);
	}

	private BroadcastReceiver mTorchReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Flashlight.FLASHLIGHT_STATE_CHANGED_ACTION.equals(intent
					.getAction())) {
				final int state = intent.getIntExtra(
						Flashlight.EXTRA_FLASH_STATE, 0);
				boolean enabled = false;
				switch (state) {
				case Flashlight.STATE_OFF:
					if (DEBUG) {
						Log.d(TAG, "torch turned off");
					}
					enabled = false;
					break;
				case Flashlight.STATE_DEATH_RAY:
				case Flashlight.STATE_HIGH:
				case Flashlight.STATE_STROBE:
				case Flashlight.STATE_ON:
					if (DEBUG) {
						Log.d(TAG, "torch turned on");
					}
					enabled = true;
					break;
				default:
					break;
				}
				mTorchMode = enabled;
			}
			updateGUI();
		}
	};

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

}
