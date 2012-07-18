package com.android.systemui.statusbar.phone.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.provider.Settings;
import android.database.ContentObserver;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;
import com.android.systemui.statusbar.policy.ToggleSlider;


public class Volume extends StatusBarPreference
    implements ToggleSlider.Listener, View.OnKeyListener, View.OnLongClickListener {
   
    protected static final String[] VOLUME_PROPER_NAMES = new String[]{
        "VOICE", "SYSTEM", "RINGER", "MEDIA",
        "ALARM", "NOTIFY", "BT"
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
        AudioSystem.STREAM_RING,   // STREAM_NOTIFICATION
        AudioSystem.STREAM_BLUETOOTH_SCO, // STREAM_BLUETOOTH_SCO
        AudioSystem.STREAM_SYSTEM,  // STREAM_SYSTEM_ENFORCED
        AudioSystem.STREAM_VOICE_CALL, // STREAM_DTMF
        AudioSystem.STREAM_MUSIC  // STREAM_TTS
    };
    
    private static int STREAM_TYPE = 3;
    
    private AudioManager mAudioManager;  
    private Handler mHandler = new Handler();
    private VolumeObserver mVolumeObserver;

    public Volume(Context context, View view) {
        super(context, view);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mSlider.setOnChangedListener(this);
        mVolumeObserver = new VolumeObserver(mHandler);
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
        	.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}
    
    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mSlider.setVisibility(View.VISIBLE);
        mSlider.setLabel(VOLUME_PROPER_NAMES[STREAM_TYPE]);
        mSlider.setChecked(true);
        
        mContentView.getChildAt(1).setVisibility(View.GONE);
        
        mIcon.setImageResource(R.drawable.ic_lock_silent_mode_off);
        mContentView.setClickable(false);
        
        mSlider.setMax(mAudioManager.getStreamMaxVolume(STREAM_TYPE));
        mSlider.setValue(mAudioManager.getStreamVolume(STREAM_TYPE));
        
        mVolumeObserver.observe();
    }
    
    private class VolumeObserver extends ContentObserver {
        
        public VolumeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.VOLUME_SETTINGS[STREAM_TYPE]), false, this);
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
            int mValue = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_SETTINGS[STREAM_TYPE], 0);
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
    public void onChanged(ToggleSlider v, boolean tracking, boolean checked, int value) {

        mSlider.setChecked(true);
        if(!checked && !tracking){
            if(STREAM_TYPE < Settings.System.VOLUME_SETTINGS.length - 1){
                STREAM_TYPE++;
            }else{
                STREAM_TYPE = 0;
            }
            mSlider.setLabel(VOLUME_PROPER_NAMES[STREAM_TYPE]);
            mSlider.setMax(mAudioManager.getStreamMaxVolume(STREAM_TYPE));
            mSlider.setValue(mAudioManager.getStreamVolume(STREAM_TYPE));
            mVolumeObserver.reset();
        }else if(tracking){
            mAudioManager.setStreamVolume(STREAM_TYPE, value, AudioManager.FLAG_PLAY_SOUND);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // TODO may not need to use this
        return false;
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }

}
