package com.android.systemui.statusbar.phone.quicktiles;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.ToggleSlider;

public class BrightnessTile extends QuickSettingsTileContent implements
		View.OnClickListener, BrightnessStateChangeCallback {

	private BrightnessObserver mBrightnessObserver;
	private BrightnessState mBrightnessState = new BrightnessState();
	private BrightnessController mBrightnessController;

	private Handler mHandler;
	private Dialog mBrightnessDialog;
	private int mBrightnessDialogShortTimeout;
	private int mBrightnessDialogLongTimeout;
	
	private CurrentUserTracker mUserTracker = new CurrentUserTracker(mContext) {
		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			onUserSwitched();
		}
	};

	private static final String TAG = "QuickBrightnessTile";

	public BrightnessTile(Context context, View view) {
		super(context, view);
		
		Resources r = mContext.getResources();
		mBrightnessDialogLongTimeout = r
				.getInteger(R.integer.quick_settings_brightness_dialog_long_timeout);
		mBrightnessDialogShortTimeout = r
				.getInteger(R.integer.quick_settings_brightness_dialog_short_timeout);

		mHandler = new Handler();
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mTag = TAG;
		mBrightnessObserver = new BrightnessObserver(mHandler);
		mBrightnessObserver.startObserving();
		refreshResources();

	}

	@Override
	public void onClick(View v) {
		getStatusBarManager().collapsePanels();
		showBrightnessDialog();
	}

	@Override
	public void onBrightnessLevelChanged() {
		Resources r = mContext.getResources();
		int mode = Settings.System.getIntForUser(mContext.getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
				mUserTracker.getCurrentUserId());
		mBrightnessState.autoBrightness = (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
		mBrightnessState.iconId = mBrightnessState.autoBrightness ? R.drawable.ic_qs_brightness_auto_on
				: R.drawable.ic_qs_brightness_auto_off;
		mBrightnessState.label = r
				.getString(R.string.quick_settings_brightness_label);
		updateGUI();
	}

	private void updateGUI() {
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0,
				mBrightnessState.iconId, 0, 0);
		mTextView.setText(mBrightnessState.label);
		dismissBrightnessDialog(mBrightnessDialogShortTimeout);
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		mBrightnessObserver.stop();
		mBrightnessObserver = null;
		mUserTracker = null;		
	}

	@Override
	public void refreshResources() {
		// Reset the dialog
		boolean isBrightnessDialogVisible = false;
		if (mBrightnessDialog != null) {
			removeAllBrightnessDialogCallbacks();

			isBrightnessDialogVisible = mBrightnessDialog.isShowing();
			mBrightnessDialog.dismiss();
		}
		mBrightnessDialog = null;
		if (isBrightnessDialogVisible) {
			showBrightnessDialog();
		}
		onBrightnessLevelChanged();
	}

	private void removeAllBrightnessDialogCallbacks() {
		mHandler.removeCallbacks(mDismissBrightnessDialogRunnable);
	}

	private Runnable mDismissBrightnessDialogRunnable = new Runnable() {
		public void run() {
			if (mBrightnessDialog != null && mBrightnessDialog.isShowing()) {
				mBrightnessDialog.dismiss();
			}
			removeAllBrightnessDialogCallbacks();
		};
	};

	void showBrightnessDialog() {

		if (mBrightnessDialog == null) {
			mBrightnessDialog = new Dialog(mContext);
			mBrightnessDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mBrightnessDialog
					.setContentView(R.layout.quick_settings_brightness_dialog);
			mBrightnessDialog.setCanceledOnTouchOutside(true);

			mBrightnessController = new BrightnessController(mContext,
					(ImageView) mBrightnessDialog
							.findViewById(R.id.brightness_icon),
					(ToggleSlider) mBrightnessDialog
							.findViewById(R.id.brightness_slider));
			mBrightnessController.addStateChangedCallback(this);
			mBrightnessDialog
					.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							mBrightnessController = null;
						}
					});

			mBrightnessDialog.getWindow().setType(
					WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			mBrightnessDialog.getWindow().getAttributes().privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
			mBrightnessDialog.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}
		if (!mBrightnessDialog.isShowing()) {
			try {
				WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
			} catch (RemoteException e) {
			}
			mBrightnessDialog.show();
			dismissBrightnessDialog(mBrightnessDialogLongTimeout);
		}
	}

	void dismissBrightnessDialog(int timeout) {
		removeAllBrightnessDialogCallbacks();
		if (mBrightnessDialog != null) {
			mHandler.postDelayed(mDismissBrightnessDialogRunnable, timeout);
		}
	}

	void onUserSwitched() {
		mBrightnessObserver.startObserving();
		onBrightnessLevelChanged();
	}

	/** ContentObserver to watch brightness **/
	private class BrightnessObserver extends ContentObserver {
		public BrightnessObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			onBrightnessLevelChanged();
		}

		public void startObserving() {
			final ContentResolver cr = mContext.getContentResolver();
			cr.unregisterContentObserver(this);
			cr.registerContentObserver(Settings.System
					.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false,
					this, mUserTracker.getCurrentUserId());
			cr.registerContentObserver(Settings.System
					.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, this,
					mUserTracker.getCurrentUserId());
		}
		
		public void stop(){
			final ContentResolver cr = mContext.getContentResolver();
			cr.unregisterContentObserver(this);
		}
	}

	static class BrightnessState extends State {
		boolean autoBrightness;
	}

}
