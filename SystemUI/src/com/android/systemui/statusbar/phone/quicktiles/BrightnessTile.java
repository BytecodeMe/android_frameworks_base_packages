package com.android.systemui.statusbar.phone.quicktiles;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.statusbar.policy.CurrentUserTracker;

public class BrightnessTile extends QuickSettingsTileContent implements
		View.OnClickListener, BrightnessStateChangeCallback {

	private BrightnessObserver mBrightnessObserver;
	private BrightnessState mBrightnessState = new BrightnessState();
	private BrightnessController mBrightnessController;

	private Handler mHandler;
	private ImageView mImageFoo;
	
	private CurrentUserTracker mUserTracker = new CurrentUserTracker(mContext) {
		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			onUserSwitched();
		}
	};

	private static final String TAG = "QuickBrightnessTile";
	private static final int TIMEOUT = 3000;

	public BrightnessTile(Context context, View view) {
		super(context, view);
		
		mHandler = new Handler();
		mImageFoo = new ImageView(mContext);
		init();
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mTag = TAG;
		mBrightnessObserver = new BrightnessObserver(mHandler);
		mBrightnessObserver.startObserving();
		
		final SeekBar slider = (SeekBar)mSlider.findViewById(R.id.slider);
		slider.setOnTouchListener(new View.OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_MOVE:
						mHandler.removeCallbacks(mResetRunnable);
						return false;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						mHandler.postDelayed(mResetRunnable, TIMEOUT);
						return false;
				}
				return false;
			}
			
		});
		
		mBrightnessController = new BrightnessController(mContext,
				mImageFoo, mSlider);
		mBrightnessController.addStateChangedCallback(this);
		
		refreshResources();

	}

	@Override
	public void onClick(View v) {
		mHandler.removeCallbacks(mResetRunnable);
		if(mCallBack!=null){
			mSlider.setVisibility(View.VISIBLE);
			updateGUI();
			mCallBack.changeSize(1,3);
			mHandler.postDelayed(mResetRunnable, TIMEOUT);
		}
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
		boolean adjustForSlider = (mSlider.getVisibility()==View.VISIBLE);
		
		mTextView.setCompoundDrawablesWithIntrinsicBounds(
				(adjustForSlider?mBrightnessState.iconId:0),
				(adjustForSlider?0:mBrightnessState.iconId), 0, 0);
		mTextView.setText(mBrightnessState.label);
		mTextView.setPadding(mTextView.getPaddingLeft(), 
				mTextView.getPaddingTop(), 
				mTextView.getPaddingRight(), 
				(adjustForSlider?(int)dpToPx(70f):0));
	}
	
	private float dpToPx(float dp) {
		Resources r = mContext.getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				r.getDisplayMetrics());
		return px;
	}

	@Override
	public void release() {
		mBrightnessObserver.stop();
		mBrightnessObserver = null;
		mBrightnessController = null;
		mUserTracker = null;		
	}

	@Override
	public void refreshResources() {
		onBrightnessLevelChanged();
	}
	
	Runnable mResetRunnable = new Runnable(){
		@Override
		public void run() {
			mSlider.setVisibility(View.GONE);
			updateGUI();
			mCallBack.changeSize(1,1);
		}
	};

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
