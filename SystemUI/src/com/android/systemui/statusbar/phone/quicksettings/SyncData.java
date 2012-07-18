package com.android.systemui.statusbar.phone.quicksettings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.content.SyncStorageEngine;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class SyncData extends StatusBarPreference 
	implements OnCheckedChangeListener, View.OnLongClickListener {

    private final static String TAG = SyncData.class.getSimpleName();
    
    private boolean mSyncState = false;

    public SyncData(Context context, View view) {
        super(context, view);

        mSyncState = getSyncState();
        init();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "onCheckChanged: isChecked="+isChecked+", mSyncState="+mSyncState);
        if(mSyncState != isChecked){
            mSyncState = isChecked;
            
            ContentResolver.setMasterSyncAutomatically(isChecked);
        }
        
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_SYNC_SETTINGS)
        	.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}

    @Override
    protected void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setChecked(mSyncState);
        mCheckBox.setOnCheckedChangeListener(this);
        
        mTitle.setText(R.string.status_bar_settings_sync);
        mIcon.setImageResource(R.drawable.ic_sysbar_sync);
        
        mTag = TAG;
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(SyncStorageEngine.SYNC_CONNECTION_SETTING_CHANGED_INTENT.getAction());
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void release() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub

    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            mSyncState = getSyncState();
            mCheckBox.setChecked(mSyncState);
        }
        
    };

    private boolean getSyncState() {
        return ContentResolver.getMasterSyncAutomatically();
    }

}
