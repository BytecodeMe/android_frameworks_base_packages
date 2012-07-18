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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;

import com.android.systemui.R;

import com.android.systemui.statusbar.policy.ToggleSlider;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class Brightness extends StatusBarPreference
    implements ToggleSlider.Listener, View.OnLongClickListener {
    private static final String TAG = "QuickSettings.Brightness";

    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    private static final int MINIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_DIM;
    private static final int MAXIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_ON;

    private IPowerManager mPower;
    private boolean mAutomaticAvailable = false;

    public Brightness(Context context, View view) {
        super(context, view);

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        mSlider.setOnChangedListener(this);
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
        	.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}
    
    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mSlider.setVisibility(View.VISIBLE);
        mSlider.setLabel(R.string.status_bar_settings_auto_brightness_label);
        mIcon.setImageResource(R.drawable.ic_sysbar_brightness);
        
        mTag = TAG;
        
        mContentView.getChildAt(1).setVisibility(View.GONE);        
        updateGUI();
    }
    
    @Override
    public void release(){
        // nothing to do
    }

    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value) {
        setMode(automatic ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (!automatic) {
            final int val = value + MINIMUM_BACKLIGHT;
            setBrightness(val);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putInt(mContext.getContentResolver(), 
                                    Settings.System.SCREEN_BRIGHTNESS, val);
                        }
                    });
            }
        }
    }
    
    private void updateGUI(){
        if (mAutomaticAvailable) {
            int automatic;
            try {
                automatic = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
            } catch (SettingNotFoundException snfe) {
                automatic = 0;
            }
            mSlider.setChecked(automatic != 0);
        } else {
            mSlider.setChecked(false);
        }
        
        int value;
        try {
            value = Settings.System.getInt(mContext.getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException ex) {
            value = MAXIMUM_BACKLIGHT;
        }

        mSlider.setMax(MAXIMUM_BACKLIGHT - MINIMUM_BACKLIGHT);
        mSlider.setValue(value - MINIMUM_BACKLIGHT);
    }

    private void setMode(int mode) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }
    
    private void setBrightness(int brightness) {
        try {
            mPower.setBacklightBrightness(brightness);
        } catch (RemoteException ex) {
        }        
    }

    @Override
    public void refreshResources() {
        // in place of content observer
        updateGUI();
    }
}
