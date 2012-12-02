/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone.quicktiles;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import com.android.internal.telephony.Phone;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class LTETile extends QuickSettingsTileContent
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "QuickSettings.LTE";
    private static final boolean DEBUG = true;
    
    private static final String EMPTY = "";

    private boolean mLTEmode;
    private SettingsObserver mSettingsObserver;
    private Handler mHandler;
    private int mNetworkState;
    private TelephonyManager mTelephonyManager;
    private ConnectivityManager mConnManager;
    private int mPhoneState;

    public LTETile(Context context, View view) {
        super(context, view);
        
        mLTEmode = false; 
        mTelephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneState = mTelephonyManager.getCallState();
        mConnManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
    	launchActivity(new Intent(Intent.ACTION_MAIN)
			.setClassName("com.android.phone", "com.android.phone.Settings"));        
		return true;
	}
    
    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	mContentView.setOnClickListener(this);
    	//mTextView.setVisibility(View.GONE);
    	//mRSSILayout.setVisibility(View.VISIBLE);
    	setText(R.string.status_bar_settings_lte);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mTelephoneReceiver, filter);
        
        startListening();        
        createObserver();
        updateGUI();
    }
    
    private void updateGUI(){
    //updates the preference based on network and phone info
        String summary = EMPTY;
        
        switch(mNetworkState){
            case Phone.NT_MODE_CDMA:
                mLTEmode = false;
                break;
            case Phone.NT_MODE_GLOBAL:
                mLTEmode = true; 
                break;
            default:
                mLTEmode = false;
        }
        
        NetworkInfo active = null;
        NetworkInfo mobile = null;
        boolean isWifiConnected = false;
        try{
            active = mConnManager.getActiveNetworkInfo();
            if(active.getType() == ConnectivityManager.TYPE_WIFI 
                    && active.getState().equals(NetworkInfo.State.CONNECTED)){
                isWifiConnected = true;
            }
            // update the summary with the state of the mobile connection
            mobile = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if(mobile.getType()==ConnectivityManager.TYPE_MOBILE){
                switch(mobile.getState()){
                    case CONNECTING:
                        summary = "Connecting ...";
                        break;
                    case CONNECTED:
                        summary = "Connected";
                        break;
                    case SUSPENDED:
                        summary = "Suspended";
                        break;
                    case DISCONNECTING:
                        summary = "Disconnecting ...";
                        break;
                    case DISCONNECTED:
                        summary = "Disconnected";
                        break;
                    case UNKNOWN:
                    default:
                        summary = "Unknown ...";
                }
            }
        }catch(NullPointerException e){}
        
        mContentView.setClickable(!isWifiConnected && mPhoneState == TelephonyManager.CALL_STATE_IDLE);
        String text = "";
        if(mTelephonyManager.getNetworkType()==TelephonyManager.NETWORK_TYPE_UNKNOWN){
        	text = mContext.getString(R.string.status_bar_settings_disabled);
            mContentView.setEnabled(false);
        }else{
        	text = mTelephonyManager.getNetworkTypeName();
            mContentView.setEnabled(true);
        }
        
        // dont update the summary in case there was an issue getting the state
        if(!summary.equals(EMPTY)){
        	text += "\n"+summary;
        }
        
        setText(text);
        	
        if(DEBUG)dump();
    }
    
    private void setText(int id){
    	String text = mContext.getResources().getString(id);
    	setText(text);
    }
    
    private void setText(String text){
    	mTextView.setCompoundDrawablesWithIntrinsicBounds(0, 
    			mLTEmode?R.drawable.ic_sysbar_lte_on:R.drawable.ic_sysbar_lte_off, 0, 0);
    	mTextView.setText(text);
    	/*if(mLTEmode){
    		mRSSIImage.setImageResource(R.drawable.ic_qs_signal_full_4);
    		mRSSIImageOverlay.setImageResource(R.drawable.ic_qs_signal_full_4g);
    	}else{
    		mRSSIImage.setImageResource(R.drawable.ic_qs_signal_4);
    		mRSSIImageOverlay.setImageResource(R.drawable.ic_qs_signal_3g);
    	}
    	mRSSITextView.setText(text);
    	*/
    }
    
    private void dump(){
        String message = "Connection Info:";
        try{
            message+=" Active Conn Name="+mConnManager.getActiveNetworkInfo().getTypeName();
            message+=" Active Conn State="+mConnManager.getActiveNetworkInfo().getState().name();
        }catch(Exception e){
            message+=" Null";
        }
                
        Log.d(TAG,message);
    }
    
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.i(TAG, "[onDataConnect] state:" + state + " networkType:" + networkType);
            updateGUI();
        }
    };
    
    private void startListening() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
              | PhoneStateListener.LISTEN_DATA_ACTIVITY
              | PhoneStateListener.LISTEN_SERVICE_STATE);            
    }
    
    private void stopListening(){
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }
   
    @Override
    public void release() {
        mContext.unregisterReceiver(mTelephoneReceiver);
        stopListening();
        if(mSettingsObserver != null){
            mSettingsObserver.stop();
            mSettingsObserver = null;
        }
    }

    @Override
	public void onClick(View v) {
        Intent intent = new Intent("com.bamf.settings.request.LTE_NETWORK_CHANGE");
            intent.putExtra("networkType", (mNetworkState==Phone.NT_MODE_CDMA)?Phone.NT_MODE_GLOBAL:Phone.NT_MODE_CDMA);
            mContext.sendBroadcast(intent);
    }
    
    private BroadcastReceiver mTelephoneReceiver = new BroadcastReceiver(){
        //this monitors the connections and phone state to so we know when to 
        //update the preference
        @Override
        public void onReceive(Context context, Intent intent) {
            
            if(ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())){
                String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            }else if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())){
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                if(state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                    if(DEBUG){Log.d(TAG,"phone state idle");}

                }else if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) ||
                        state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                    if(DEBUG){Log.d(TAG,"phone is ringing or off the hook");}
                }
            }
            
            updateGUI();            
        }
    };  
    
    private void createObserver(){
        if(mHandler == null){
            mHandler = new Handler();
        }
        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver(mHandler);
            mSettingsObserver.observe();
        }else{
            mSettingsObserver.update();
        }
    }
    
    private class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }
        
        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor(
                    "preferred_network_mode"), false, this);
            update();
        }
        
        public void stop() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            ContentResolver resolver = mContext.getContentResolver();
            mNetworkState = Settings.Global.getInt(resolver,
                    "preferred_network_mode", Phone.NT_MODE_CDMA);
            updateGUI();
        }
        
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }

}