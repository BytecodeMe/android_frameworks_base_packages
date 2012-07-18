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

package com.android.systemui.statusbar.phone.quicksettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

import java.util.List;

public class Wifi extends StatusBarPreference
        implements CompoundButton.OnCheckedChangeListener, 
        View.OnLongClickListener {
	
    private static final String TAG = Wifi.class.getSimpleName();
	private static final boolean DEBUG = false;
    private static final String EMPTY = "";
    
    private boolean mWifi = false;
    
    private WifiManager mWifiManager;
    private ConnectivityManager mConnManager;

    public Wifi(Context context, View view) {
        super(context, view);
        mContentView.setOnLongClickListener(this);
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        mConnManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifi = isWifiEnabled();
        init();
    }
    
    @Override
    public void init() {
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setOnCheckedChangeListener(this);
        mCheckBox.setChecked(mWifi);
        
        mTag = TAG;
        
        mIcon.setImageResource(R.drawable.ic_sysbar_wifi_on);
        mTitle.setText(R.string.status_bar_settings_wifi_button);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiReceiver, filter);
    }
    
    public void updateGUI(){
        updateGUI(EMPTY);
    }
    
    public void updateGUI(String summary){
        mCheckBox.setChecked(mWifi);
        mTitle.setText(R.string.status_bar_settings_wifi_button);
        
        NetworkInfo active = null;
        boolean isWifiConnected = false;
        try{
            active = mConnManager.getActiveNetworkInfo();
            if(active.getType() == ConnectivityManager.TYPE_WIFI 
                    && active.getState().equals(NetworkInfo.State.CONNECTED)){
                isWifiConnected = true;
            }
        }catch(NullPointerException e){}
        
        if(isWifiConnected){
            mTitle.setText(R.string.status_bar_settings_wifi_button);
            mSummary.setVisibility(View.VISIBLE);
            setSummary();
        }else {
            setSummary(summary);
        }
        
        mContentView.invalidate();
    }
    
    public boolean isWifiEnabled(){
        int state = mWifiManager.getWifiState();
        switch(state){
            case WifiManager.WIFI_STATE_ENABLED:
                return true;
            default:
                return false;
        }
    }
    
    private String getSSID(){
        return mWifiManager.getConnectionInfo().getSSID();
    }
    
    private WifiConfiguration getSecurity(){
        final List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        if(wifiConfigs == null)return null;
        
        if(DEBUG)Log.d(TAG, "configs size:"+wifiConfigs.size());
        for (int i = wifiConfigs.size() - 1; i >= 0; i--) {
            if(DEBUG)Log.d(TAG, "config:"+wifiConfigs.get(i).SSID+" current:"+getSSID());
            if(removeDoubleQuotes(wifiConfigs.get(i).SSID).equals(getSSID())){
                return wifiConfigs.get(i);
            }
        }
        //didnt find it?
        return null;
    }
    
    private ScanResult getScanResult(){
        
        final List<ScanResult> results = mWifiManager.getScanResults();
        if(results==null)return null;
        
        if(DEBUG)Log.d(TAG, "results size:"+results.size());
        for (int i = results.size() - 1; i >= 0; i--) {
        	if(DEBUG)Log.d(TAG, "result:"+results.get(i).SSID+" current:"+getSSID());
            if(removeDoubleQuotes(results.get(i).SSID).equals(getSSID())){
                return results.get(i);
            }
        }
        
        return null;
    }
    
    private void setSummary(String text){
        if(!text.equals(EMPTY)){
            mSummary.setVisibility(View.VISIBLE);
            mSummary.setText(text);
        }else{
            mSummary.setVisibility(View.GONE);
        }
    }
    
    private void setSummary(){
        StringBuilder summary = new StringBuilder();
        mConfig = getSecurity();
        
        if (mConfig == null) {
            Log.d(TAG, "mConfig returned null");
            return;
        }
        
        summary.append(String.format(mContext.getString(R.string.wifi_ssid),getSSID()));
        security = getSecurity(mConfig);
        pskType = getPskType(getScanResult());

        if (security != SECURITY_NONE) {
            String securityStrFormat;
            if (summary.length() == 0) {
                securityStrFormat = mContext.getString(R.string.wifi_secured_first_item);
            } else {
                securityStrFormat = mContext.getString(R.string.wifi_secured_second_item);
            }
            summary.append(String.format(securityStrFormat, getSecurityString(true)));
        }

        mSummary.setText(summary.toString());
    }
    
    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String summary = EMPTY;
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,-1);
                boolean enabled = false;
                boolean checkbox = false;
                switch(state){
                    case WifiManager.WIFI_STATE_ENABLED:
                        enabled = true;
                        checkbox = true;
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        checkbox = true;
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        summary = "Disabling ...";
                        checkbox = false;
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        summary = "Enabling ...";
                        checkbox = false;                  
                    default: 
                }
                mWifi = enabled;
                mCheckBox.setEnabled(checkbox);
            }
            updateGUI(summary);
        }
        
    }; 

    @Override
    public void release() {
        mContext.unregisterReceiver(mWifiReceiver);
    }

    @Override
    public boolean onLongClick(View v) {
        mContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getStatusBarManager().collapse();   
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != mWifi) {
            setSummary(isChecked?"Enabling ...":"Disabling ...");
            mCheckBox.setEnabled(false);
            if(mWifiManager.setWifiEnabled(isChecked)){
                mWifi = isChecked;
            }
        }
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }

    /** These values are matched in string arrays -- changes must be kept in sync */
    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    int security;
    PskType pskType = PskType.UNKNOWN;

    private WifiConfiguration mConfig;

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }
    
    private String getSecurityString(boolean concise) {

        switch(security) {
            case SECURITY_EAP:
                return concise ? mContext.getString(R.string.wifi_security_short_eap) :
                    mContext.getString(R.string.wifi_security_eap);
            case SECURITY_PSK:
                switch (pskType) {
                    case WPA:
                        return concise ? mContext.getString(R.string.wifi_security_short_wpa) :
                            mContext.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return concise ? mContext.getString(R.string.wifi_security_short_wpa2) :
                            mContext.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return concise ? mContext.getString(R.string.wifi_security_short_wpa_wpa2) :
                            mContext.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                    default:
                        return concise ? mContext.getString(R.string.wifi_security_short_psk_generic)
                                : mContext.getString(R.string.wifi_security_psk_generic);
                }
            case SECURITY_WEP:
                return concise ? mContext.getString(R.string.wifi_security_short_wep) :
                    mContext.getString(R.string.wifi_security_wep);
            case SECURITY_NONE:
            default:
                return concise ? "" : mContext.getString(R.string.wifi_security_none);
        }
    }

    private static PskType getPskType(ScanResult result) {
        if(result==null)return PskType.UNKNOWN;
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            Log.w(TAG, "Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }
    
    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }
}

