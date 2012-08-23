package com.android.systemui;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PowToast extends Activity {

	Toast mToast;
	Handler mH = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		
		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        mToast.setView(makeView());
        makePowSound();
	}
	
	private View makeView() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setLayoutParams(
                new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ));
        final int p = (int)(8 * metrics.density);
        view.setPadding(p, p, p, p);

        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = (int) (-4*metrics.density);
        
        ImageView pow = new ImageView(this);
        pow.setBackgroundResource(R.drawable.big_pow);
        view.addView(pow, lp);
        
        return view;
    }
	
	private void makePowSound(){
		final String POW_URL = "http://soundbible.com/mp3/10%20Guage%20Shotgun-SoundBible.com-74120584.mp3";
		final MediaPlayer mMediaPlayer = new MediaPlayer();
		
		new Thread(new Runnable(){
			 @Override
				public void run() {
					 try{
						 mMediaPlayer.setDataSource(POW_URL);
						 final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
						 if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) {
							 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
							 mMediaPlayer.setLooping(false);
							 mMediaPlayer.prepare();
							 mMediaPlayer.start();
						  }
						 
						 mToast.show();
						 while(mMediaPlayer.isPlaying()){
									// wait for it to finish
						}
						finish();
					 }catch(Exception e){
						finish();
					 }			
			}
		 }).start();
	}
}
