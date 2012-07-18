package com.android.systemui.statusbar.phone.quicksettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class MobileData extends StatusBarPreference 
	implements OnCheckedChangeListener, View.OnLongClickListener {

    private final static String TAG = MobileData.class.getSimpleName();
    
    private ConnectivityManager mConnManager;
    private boolean mDataMode = false;
    
    public MobileData(Context context, View view) {
        super(context, view);
        // TODO Auto-generated constructor stub
        mConnManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mDataMode = mConnManager.getMobileDataEnabled();
        init();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(mDataMode != isChecked){
            mDataMode = isChecked;
            mConnManager.setMobileDataEnabled(isChecked);
        }
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(Intent.ACTION_MAIN)
        	.setClassName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity")
        	.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}

    @Override
    protected void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setOnCheckedChangeListener(this);
        mCheckBox.setChecked(mDataMode);
        
        mTag = TAG;
        
        mIcon.setImageResource(R.drawable.ic_sysbar_data);
        mTitle.setText(R.string.status_bar_settings_mobiledata);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void release() {
        // TODO Auto-generated method stub
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub

    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            mDataMode = mConnManager.getMobileDataEnabled();
            mCheckBox.setChecked(mDataMode);
        }
        
    };

}
