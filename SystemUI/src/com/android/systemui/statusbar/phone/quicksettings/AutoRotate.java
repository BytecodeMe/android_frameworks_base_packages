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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import android.widget.CompoundButton;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

/**
 * TODO: Listen for changes to the setting.
 */
public class AutoRotate extends StatusBarPreference
    implements CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {
    private static final String TAG = "QuickSettings.AutoRotate";

    private boolean mAutoRotation;

    public AutoRotate(Context context, View view) {
        super(context, view);
        mAutoRotation = getAutoRotation();
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        	.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}

    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setChecked(mAutoRotation);
        mCheckBox.setOnCheckedChangeListener(this);
        
        mIcon.setImageResource(R.drawable.ic_sysbar_rotate_on);
        mTitle.setText(R.string.status_bar_settings_auto_rotation);
    }
    
    @Override
    public void release(){
        
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != mAutoRotation) {
            setAutoRotation(checked);
        }
    }

    private boolean getAutoRotation() {
        ContentResolver cr = mContext.getContentResolver();
        return 0 != Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0);
    }

    private void setAutoRotation(final boolean autorotate) {
        mAutoRotation = autorotate;
        AsyncTask.execute(new Runnable() {
                public void run() {
                    try {
                        IWindowManager wm = IWindowManager.Stub.asInterface(
                                ServiceManager.getService(Context.WINDOW_SERVICE));
                        if (autorotate) {
                            wm.thawRotation();
                        } else {
                            wm.freezeRotation(-1);
                        }
                    } catch (RemoteException exc) {
                        Log.w(TAG, "Unable to save auto-rotate setting");
                    }
                }
            });
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }
}
