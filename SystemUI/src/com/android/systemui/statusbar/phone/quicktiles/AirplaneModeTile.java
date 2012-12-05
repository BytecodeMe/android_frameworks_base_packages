package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class AirplaneModeTile extends QuickSettingsTileContent
		implements View.OnClickListener, View.OnLongClickListener {

	private static final String TAG = "QuickSettings.AirplaneMode";

    private boolean mAirplaneMode;

	public AirplaneModeTile(Context context, View view) {
		super(context, view);
		mAirplaneMode = getAirplaneModeState();
	    init();
	}
	
	 @Override
	    public void init() {
	        mContentView.setOnClickListener(this);
	        mContentView.setOnLongClickListener(this);
	       
	        IntentFilter filter = new IntentFilter();
	        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	        mContext.registerReceiver(mAirplaneModeReceiver, filter);
	        
	        mTextView.setText(R.string.quick_settings_airplane_mode_label);
        	mTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_qs_airplane_off, 0, 0);
	    }


	@Override
	public void onClick(View v) {
        setAirplaneModeState(!mAirplaneMode);		
	}
	
	 @Override
     public boolean onLongClick(View v) {
		 launchActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));              
		 return true;
	 }
	 
	 private boolean getAirplaneModeState() {	        
	      return Settings.Global.getInt(mContext.getContentResolver(),
	    		  Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
	    }
	    
	    private void setAirplaneModeState(boolean enabled) {
	        // Change the system setting
	        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
	                                enabled ? 1 : 0);
	        // Post the intent
	        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	        intent.putExtra("state", enabled);
	        mContext.sendBroadcast(intent);
	    }
	    
	    public void onAirplaneModeChanged(boolean enabled) {
	        // TODO: If view is in awaiting state, disable	
	    	mAirplaneMode = enabled;
	        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, 
	        		mAirplaneMode ? R.drawable.ic_qs_airplane_on:R.drawable.ic_qs_airplane_off, 0, 0);
	    }
	    
	 
	 private BroadcastReceiver mAirplaneModeReceiver = new BroadcastReceiver(){
	        public void onReceive(Context context, Intent intent) {
	            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
	                final boolean enabled = intent.getBooleanExtra("state", false);
	                onAirplaneModeChanged(enabled);
	            }
	            
	        }
	    };
	
	  @Override
	    public void release() {
	        mContext.unregisterReceiver(mAirplaneModeReceiver);
	    }
	  
	   @Override
	    public void refreshResources() {
	        // TODO Auto-generated method stub  
	    }
}
