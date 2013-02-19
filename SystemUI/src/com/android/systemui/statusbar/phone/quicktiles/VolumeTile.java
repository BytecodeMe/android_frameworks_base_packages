package com.android.systemui.statusbar.phone.quicktiles;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.ToggleSlider;


public class VolumeTile extends QuickSettingsTileContent
	implements View.OnLongClickListener, ToggleSlider.Listener, View.OnClickListener {

	private static final String TAG = VolumeTile.class.getSimpleName();
	private static final int TIMEOUT = 3000;
	
	protected static final String[] VOLUME_PROPER_NAMES = new String[]{
        "VOICE", "SYSTEM", "RINGER", "MEDIA",
        "ALARM", "NOTIFY"
    };
    
    /* STREAM_VOLUME_ALIAS[] indicates for each stream if it uses the volume settings
     * of another stream: This avoids multiplying the volume settings for hidden
     * stream types that follow other stream behavior for volume settings
     * NOTE: do not create loops in aliases! */
    protected static int[] STREAM_VOLUME_ALIAS = new int[] {
        AudioSystem.STREAM_VOICE_CALL,  // STREAM_VOICE_CALL
        AudioSystem.STREAM_SYSTEM,  // STREAM_SYSTEM
        AudioSystem.STREAM_RING,  // STREAM_RING
        AudioSystem.STREAM_MUSIC, // STREAM_MUSIC
        AudioSystem.STREAM_ALARM,  // STREAM_ALARM
        AudioSystem.STREAM_NOTIFICATION,   // STREAM_NOTIFICATION
        AudioSystem.STREAM_BLUETOOTH_SCO, // STREAM_BLUETOOTH_SCO
        AudioSystem.STREAM_SYSTEM,  // STREAM_SYSTEM_ENFORCED
        AudioSystem.STREAM_VOICE_CALL, // STREAM_DTMF
        AudioSystem.STREAM_MUSIC  // STREAM_TTS
    };
    
    private CurrentUserTracker mUserTracker = new CurrentUserTracker(mContext) {
		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			onUserSwitched();
		}
	};
    
    private static int STREAM_TYPE = 3;
    
    private AudioManager mAudioManager;  
    private Handler mHandler = new Handler();
    private VolumeObserver mVolumeObserver;
    private State mVolumeState;

    public VolumeTile(Context context, View view) {
        super(context, view);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mSlider.setOnChangedListener(this);
        mVolumeObserver = new VolumeObserver(mHandler);
        init();
    }
    
    @Override
    public void init() {
    	mContentView.setOnClickListener(this);
    	mContentView.setOnLongClickListener(this);
    	
        mSlider.setChecked(true);
        
        final SeekBar slider = (SeekBar)mSlider.findViewById(R.id.slider);
		slider.setOnTouchListener(mTouchListener);
		final CompoundButton button = (CompoundButton)mSlider.findViewById(R.id.toggle);
		button.setOnTouchListener(mTouchListener);
        
        mVolumeState = new State();
        mVolumeState.iconId = R.drawable.ic_lock_silent_mode_off;
        mVolumeState.label = mContext.getResources().getString(R.string.quick_settings_volume_label);
        
        mVolumeObserver.observe();
        
        updateGUI();
    }

	@Override
	public void onInit(ToggleSlider v) {
		// set the icon and label
    	if (STREAM_TYPE==AudioManager.STREAM_MUSIC &&
    			(mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC) &
                (AudioManager.DEVICE_OUT_BLUETOOTH_A2DP |
                AudioManager.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES |
                AudioManager.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER)) != 0) {
    		mSlider.setLabel("BT");
            //setMusicIcon(com.android.internal.R.drawable.ic_audio_bt, 
            		//com.android.internal.R.drawable.ic_audio_bt_mute);
        } else {
        	mSlider.setLabel(VOLUME_PROPER_NAMES[STREAM_TYPE]);
            //setMusicIcon(com.android.internal.R.drawable.ic_audio_vol, 
            		//com.android.internal.R.drawable.ic_audio_vol_mute);
        }
    	
        mSlider.setMax(mAudioManager.getStreamMaxVolume(STREAM_TYPE));
        mSlider.setValue(mAudioManager.getStreamVolume(STREAM_TYPE));
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
	public boolean onLongClick(View v) {
    	launchActivity(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS));        
		return true;
	}
	
	private void updateGUI() {
		boolean adjustForSlider = (mSlider.getVisibility()==View.VISIBLE);
		
		mTextView.setCompoundDrawablesWithIntrinsicBounds(
				(adjustForSlider?mVolumeState.iconId:0),
				(adjustForSlider?0:mVolumeState.iconId), 0, 0);
		mTextView.setText(mVolumeState.label);
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
	
	Runnable mResetRunnable = new Runnable(){
		@Override
		public void run() {
			mSlider.setVisibility(View.GONE);
			updateGUI();
			mCallBack.changeSize(1,1);
		}
	};
	
	View.OnTouchListener mTouchListener = new View.OnTouchListener(){

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
		
	};

	void onUserSwitched() {
		mVolumeObserver.reset();
		refreshResources();
	}
    
    private class VolumeObserver extends ContentObserver {
        
        public VolumeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.VOLUME_SETTINGS[STREAM_TYPE] + "_" + getDeviceNameForStream(STREAM_TYPE)), false, this);
            Log.d(TAG, "registered uri "+Settings.System.getUriFor(
                    Settings.System.VOLUME_SETTINGS[STREAM_TYPE] + "_" + getDeviceNameForStream(STREAM_TYPE)));
            update();
        }
        
        public void stop() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }
        
        public void reset(){
            stop();
            observe();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            ContentResolver resolver = mContext.getContentResolver();
            int mValue = Settings.System.getIntForUser(resolver,
                    Settings.System.VOLUME_SETTINGS[STREAM_TYPE] + "_" + getDeviceNameForStream(STREAM_TYPE), 
                    Settings.System.getInt(resolver, Settings.System.VOLUME_SETTINGS[STREAM_TYPE], 0),
                    mUserTracker.getCurrentUserId());
            mSlider.setValue(mValue);
        }
    };
    
    @Override
    public void release(){
        mSlider.setOnChangedListener(null);
        mVolumeObserver.stop();
        mVolumeObserver = null;
    }

    @Override
    public void onChanged(ToggleSlider v, boolean tracking, boolean checked, final int value) {

        mSlider.setChecked(true);
        if(!checked && !tracking){
            if(STREAM_TYPE < Settings.System.VOLUME_SETTINGS.length - 2){
                STREAM_TYPE++;
            }else{
                STREAM_TYPE = 0;
            }
            
            // skip system now since it cannot be unlinked
            if(STREAM_TYPE==AudioManager.STREAM_SYSTEM){
            	STREAM_TYPE++;
            }
            
            // set the icon and label
        	if (STREAM_TYPE==AudioManager.STREAM_MUSIC &&
        			(mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC) &
                    (AudioManager.DEVICE_OUT_BLUETOOTH_A2DP |
                    AudioManager.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES |
                    AudioManager.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER)) != 0) {
        		mSlider.setLabel("BT");
//                setMusicIcon(com.android.internal.R.drawable.ic_audio_bt, 
//                		com.android.internal.R.drawable.ic_audio_bt_mute);
            } else {
            	mSlider.setLabel(VOLUME_PROPER_NAMES[STREAM_TYPE]);
//                setMusicIcon(com.android.internal.R.drawable.ic_audio_vol, 
//                		com.android.internal.R.drawable.ic_audio_vol_mute);
            }
            
            mSlider.setMax(mAudioManager.getStreamMaxVolume(STREAM_TYPE));
            mSlider.setValue(mAudioManager.getStreamVolume(STREAM_TYPE));
            mVolumeObserver.reset();
        }else if(tracking){
        	AsyncTask.execute(new Runnable() {
                public void run() {
                	mAudioManager.setStreamVolume(STREAM_TYPE, value, AudioManager.FLAG_PLAY_SOUND);
                }
        	});
        }
    }

    @Override
    public void refreshResources() {
        // hack so I do not have to catch every time the device changes
    	STREAM_TYPE--;
    	onChanged(null, false, false, 0);
    }
    
    /**
     * Switch between icons because Bluetooth music is same as music volume, but with
     * different icons.
     */
    private void setMusicIcon(int resId, int resMuteId) {
    	mImageView.setImageResource(isMuted(STREAM_TYPE) ? resMuteId : resId);
    }
    
    private boolean isMuted(int streamType) {
        return mAudioManager.isStreamMute(streamType);
    }
    
    /**
     * this will return the name of the current device so we can determine the correct
     * value to monitor in the database.
     * @param stream
     * @return the current device name
     */
    private String getDeviceNameForStream(int stream) {
        int device = AudioSystem.getDevicesForStream(stream);
        if ((device & (device - 1)) != 0) {
            // Multiple device selection is either:
            //  - speaker + one other device: give priority to speaker in this case.
            //  - one A2DP device + another device: happens with duplicated output. In this case
            // retain the device on the A2DP output as the other must not correspond to an active
            // selection if not the speaker.
            if ((device & AudioSystem.DEVICE_OUT_SPEAKER) != 0) {
                device = AudioSystem.DEVICE_OUT_SPEAKER;
            } else {
                device &= AudioSystem.DEVICE_OUT_ALL_A2DP;
            }
        }
        return AudioSystem.getDeviceName(device);
    }

}