package com.android.systemui.statusbar.phone.quicktiles;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;


public class BluetoothTile extends QuickSettingsTileContent 
		implements View.OnClickListener, View.OnLongClickListener {
	
	private static final String TAG = BluetoothTile.class.getSimpleName();
    private static final boolean DEBUG = false;

    private int mIconId = R.drawable.ic_qs_bluetooth_off;
    private int mContentDescriptionId = R.string.quick_settings_bluetooth_off_label;
    private int mContentSummaryId = 0;
    private boolean mEnabled = false;

	
	public BluetoothTile(Context context, View view) {
		super(context, view);
		IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        init();
    }
	
	@Override
    public void init() {
        mContentView.setOnClickListener(this);
        mContentView.setOnLongClickListener(this);
        
        mTag = TAG;
        updateGUI();
    }
	
	@Override
    public boolean onLongClick(View v) {
    launchActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            return true;
    }
	
	private void updateGUI() {
        
        mTextView.setText(mContentDescriptionId);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mIconId, 0, 0);

    }
	
	@Override 
    public void release(){
        mContext.unregisterReceiver(mReceiver);
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
    	if (adapterState == BluetoothAdapter.STATE_ON) {
		        mEnabled = (adapterState == BluetoothAdapter.STATE_ON);
		        mIconId = R.drawable.ic_qs_bluetooth_not_connected;
		        mContentDescriptionId = R.string.bluetooth;
		        mContentSummaryId = 0;
    	} else if (adapterState == BluetoothAdapter.STATE_OFF) {
    	        mEnabled = (adapterState == BluetoothAdapter.STATE_OFF);
    	        mIconId = R.drawable.ic_qs_bluetooth_off;
    	        mContentDescriptionId = R.string.quick_settings_bluetooth_off_label;;
    	        mContentSummaryId = 0;
    	}
    }

    public void handleConnectionStateChange(int connectionState) {
        final boolean connected = (connectionState == BluetoothAdapter.STATE_CONNECTED);
        if (connected) {
            mIconId = R.drawable.ic_qs_bluetooth_on;
            mContentSummaryId = R.string.quick_settings_bluetooth_label;
        } else {
            mIconId = R.drawable.ic_qs_bluetooth_not_connected;
            mContentSummaryId = R.string.quick_settings_bluetooth_label;
        }
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }

	@Override
	public void onClick(View v) {
	       final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
           if (adapter != null) {
               if(adapter.isEnabled()){
                   if(DEBUG)Log.d(TAG, "enabling bluetooth");
                   adapter.disable();
               }else{
                   if(DEBUG)Log.d(TAG, "disabling bluetooth");
                   adapter.enable();
               }
           }
	}
	
	

}
