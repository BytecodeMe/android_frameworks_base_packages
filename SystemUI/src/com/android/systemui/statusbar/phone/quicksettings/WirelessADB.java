package com.android.systemui.statusbar.phone.quicksettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class WirelessADB extends StatusBarPreference
    implements CompoundButton.OnCheckedChangeListener {
    private final static String TAG = WirelessADB.class.getSimpleName();
    private final static String PORT = "3700";
    
    private IntentFilter mIntentFilter = new IntentFilter();
    public boolean mState = false;
    public boolean mToggleState;
    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;

    public WirelessADB(Context context, View view) {
        super(context, view);
        mConnManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        
        mToggleState = false;
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mReceiver, mIntentFilter);
        init();
    }

    @Override
    protected void init() {
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setOnCheckedChangeListener(this);
        
        mTag = TAG;
        
        setLabel("OFF");
        mIcon.setImageResource(R.drawable.ic_sysbar_adb_on);

        updateGUI();
    }

    @Override
    public void release() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(mToggleState!=isChecked){
            mToggleState = isChecked;
            if(isChecked){
                startAdb();
            }else{
                stopAdb();
            }
        }
        
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI();            
        }
        
    };
    
    private void setLabel(String text){
        mTitle.setText(text);
        mSummary.setVisibility(View.GONE);
    }
    
    private void setLabelSummary(String text){
        mSummary.setVisibility(View.VISIBLE);
        mSummary.setText(text);
    }
    
    private void setLabelDisabled(){
        mTitle.setText("Disabled");
        mSummary.setVisibility(View.VISIBLE);
        mSummary.setText("Please enable Wi-Fi");
    }
    
    protected void updateGUI(){
        mCheckBox.setChecked(mToggleState);
        
        NetworkInfo active = null;
        boolean isWifiConnected = false;
        try{
            active = mConnManager.getActiveNetworkInfo();
            if(active.getType() == ConnectivityManager.TYPE_WIFI 
                    && active.getState().equals(NetworkInfo.State.CONNECTED)){
                isWifiConnected = true;
            }
        }catch(NullPointerException e){}
        
        mCheckBox.setEnabled(isWifiConnected);
        
        if(isWifiConnected && !mContentView.isEnabled()){
            enableAdb();
        }else if(!isWifiConnected){
            disableAdb();
        }
    }
    
    protected void enableAdb(){
       mContentView.setEnabled(true);
       if(mToggleState){
           stopAdb();
       }
       setLabel("OFF");
    }
    
    protected void disableAdb(){
        if(mToggleState){
            stopAdb();
        }
        mContentView.setEnabled(false);
        if(mCheckBox.isChecked()){
            mCheckBox.setChecked(false);
        }
        mToggleState = false;
        setLabelDisabled();
    }
    
    protected void startAdb(){
        //set the port to connect to
        SystemProperties.set("service.adb.tcp.port",PORT);
        //cycle adb off and on
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADB_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADB_ENABLED, 1);
        
        mState = true;
        setLabel("ENABLED");
        setLabelSummary(getWifiIp()+":"+PORT);
    }
    
    protected void stopAdb(){
        //set the port to -1 (off)
        SystemProperties.set("service.adb.tcp.port","-1");
        //cycle adb off and on
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADB_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADB_ENABLED, 1);
        
        mState = false;
        setLabel("OFF");
    }
    
    protected String getWifiIp(){
        return mWifiManager.getConnectionInfo().getStringIpAddress();
    }
}
