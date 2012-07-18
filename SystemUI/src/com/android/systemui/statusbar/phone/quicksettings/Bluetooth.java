/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class Bluetooth extends StatusBarPreference 
	implements CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {
	
    private static final String TAG = Bluetooth.class.getSimpleName();
    private static final boolean DEBUG = false;

    private int mIconId = R.drawable.ic_sysbar_bluetooth;
    private int mContentDescriptionId = R.string.bluetooth;
    private int mContentSummaryId = 0;
    private boolean mEnabled = false;

    public Bluetooth(Context context, View view) {
        super(context, view);        
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            handleAdapterStateChange(adapter.getState());
            handleConnectionStateChange(adapter.getConnectionState());
        }
        init();
    }
    
    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setOnCheckedChangeListener(this);
        mTag = TAG;
        updateGUI();
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
        	.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}
    
    private void updateGUI() {
        mCheckBox.setChecked(mEnabled);
        mTitle.setText(mContentDescriptionId);
        mIcon.setImageResource(mIconId);
        
        if(mContentSummaryId == 0){
            mSummary.setVisibility(View.GONE);
        }else{
            mSummary.setVisibility(View.VISIBLE);
            mSummary.setText(mContentSummaryId);
        }
    }
    
    @Override 
    public void release(){
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked != mEnabled){
            mEnabled = isChecked;
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                if(isChecked){
                    if(DEBUG)Log.d(TAG, "enabling bluetooth");
                    adapter.enable();
                }else{
                    if(DEBUG)Log.d(TAG, "disabling bluetooth");
                    adapter.disable();
                }
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
    
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                handleAdapterStateChange(
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
            } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                handleConnectionStateChange(
                        intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                            BluetoothAdapter.STATE_DISCONNECTED));
            }
            updateGUI();
        }     
    };
    
    public void handleAdapterStateChange(int adapterState) {
        mEnabled = (adapterState == BluetoothAdapter.STATE_ON);
        mIconId = R.drawable.ic_sysbar_bluetooth;
        mContentDescriptionId = R.string.bluetooth;
        mContentSummaryId = 0;
    }

    public void handleConnectionStateChange(int connectionState) {
        final boolean connected = (connectionState == BluetoothAdapter.STATE_CONNECTED);
        if (connected) {
            mIconId = R.drawable.ic_sysbar_bluetooth_connected;
            mContentSummaryId = R.string.bluetooth_connected;
        } else if(mEnabled) {
            mIconId = R.drawable.ic_sysbar_bluetooth;
            mContentSummaryId = R.string.bluetooth_disconnected;
        }
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }
}
