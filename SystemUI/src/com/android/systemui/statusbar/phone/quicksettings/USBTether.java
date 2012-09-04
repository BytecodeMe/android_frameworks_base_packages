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

import java.util.ArrayList;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

/**
 * TODO: Listen for changes to the setting.
 */
public class USBTether extends StatusBarPreference
    implements CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {
    private static final String TAG = "QuickSettings.Tether";

    private boolean mStartingTether = false;
    private Context mContext;
    private ConnectivityManager mCm;
    private String[] mUsbRegexs;

    private boolean mUsbConnected;
    private boolean mMassStorageActive;
    private BroadcastReceiver mTetherChangeReceiver;

    public USBTether(Context context, View view) {
        super(context, view);
        mContext = context;
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
    	launchActivity(new Intent(Intent.ACTION_MAIN)
        	.setClassName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity"));
		return true;
	}

    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	
    	mCm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mUsbRegexs = mCm.getTetherableUsbRegexs();
        final boolean usbAvailable = mUsbRegexs.length != 0;
        if (!usbAvailable || ActivityManager.isUserAMonkey()) {
            mContentView.setEnabled(false);
        }
        
        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        Intent intent = mContext.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        mContext.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        mContext.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(mContext, intent);        

        mCheckBox.setVisibility(View.VISIBLE);
        mTitle.setText(R.string.usb_tethering_button_text);
        mCheckBox.setOnCheckedChangeListener(this);
        mIcon.setImageResource(R.drawable.ic_sysbar_tether);
        updateState();
        
    }
    
    @Override
    public void release(){
    	mContext.unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        Log.w(TAG, "check changed");
    	setUsbTethering(checked);
        
    }  
    
    private void setUsbTethering(boolean enabled) {       
    	
    	if(enabled){
    		mCheckBox.setEnabled(false);
    		mStartingTether = true;
    	}else{ 
    		mCheckBox.setEnabled(true);
    		mStartingTether = false;
    	}
        if (mCm.setUsbTethering(enabled) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
        	mCheckBox.setChecked(false);
            mSummary.setVisibility(View.VISIBLE);
            mSummary.setText(R.string.usb_tethering_errored_subtext);
            mStartingTether = false;
            return;
        }
        mSummary.setVisibility(View.GONE);
        mSummary.setText("");
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }
    
    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();            
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]),
                        errored.toArray(new String[errored.size()]));
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
                updateState();
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
                updateState();
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                updateState();
            } 
        }
    }
    
    private void updateState() {        

        String[] available = mCm.getTetherableIfaces();
        String[] tethered = mCm.getTetheredIfaces();
        String[] errored = mCm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    private void updateState(String[] available, String[] tethered,
            String[] errored) {
        updateUsbState(available, tethered, errored);        
    }


    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {
        
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = mCm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }
        
        if (usbTethered) {
        	mSummary.setVisibility(View.VISIBLE);
        	mStartingTether = false;
            mSummary.setText(R.string.usb_tethering_active_subtext);
            mCheckBox.setEnabled(true);
            mCheckBox.setChecked(true);
        } else if (usbAvailable) {
        	if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
            	mSummary.setText(R.string.usb_tethering_available_subtext);
            } else {
            	mSummary.setVisibility(View.VISIBLE);
            	mSummary.setText(R.string.usb_tethering_errored_subtext);
            }
            if(!mStartingTether){
            	mCheckBox.setEnabled(true);            
            	mCheckBox.setChecked(false);
            }
        } else if (usbErrored) {
        	mSummary.setVisibility(View.VISIBLE);
        	mSummary.setText(R.string.usb_tethering_errored_subtext);
        	mCheckBox.setEnabled(false);
        	mCheckBox.setChecked(false);
        } else if (mMassStorageActive) {
        	mSummary.setVisibility(View.VISIBLE);
        	mSummary.setText(R.string.usb_tethering_storage_active_subtext);
        	mCheckBox.setEnabled(false);
        	mCheckBox.setChecked(false);
        } else {
        	mSummary.setVisibility(View.GONE);
        	mSummary.setText(R.string.usb_tethering_unavailable_subtext);
        	mCheckBox.setEnabled(false);
        	mCheckBox.setChecked(false);
        }
    }
}
