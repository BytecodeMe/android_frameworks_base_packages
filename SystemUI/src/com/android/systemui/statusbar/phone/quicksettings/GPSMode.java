package com.android.systemui.statusbar.phone.quicksettings;

import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

import java.util.Observable;
import java.util.Observer;

public class GPSMode extends StatusBarPreference 
	implements CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {

    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;
    private boolean mGPSMode = false;
    
    public GPSMode(Context context, View view) {
        super(context, view);
        Cursor settingsCursor = mContext.getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mGPSMode = getGPSSetting();
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
    	launchActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));        
		return true;
	}

    @Override
    protected void init() {
    	mContentView.setOnLongClickListener(this);
    	
        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    mGPSMode = getGPSSetting();
                    mCheckBox.setChecked(mGPSMode);
                }
            };
            mContentQueryMap.addObserver(mSettingsObserver);
        }
        
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setChecked(mGPSMode);
        mCheckBox.setOnCheckedChangeListener(this);
        
        mTitle.setText("GPS");
        mIcon.setImageResource(R.drawable.ic_sysbar_gps);
    }

    @Override
    public void release() {
        // TODO Auto-generated method stub
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
                    LocationManager.GPS_PROVIDER, isChecked);
    }
    
    public boolean getGPSSetting(){
        return Settings.Secure.isLocationProviderEnabled(
                mContext.getContentResolver(), LocationManager.GPS_PROVIDER);
    }

}
