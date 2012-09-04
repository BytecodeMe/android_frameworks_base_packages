/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.MediaPlayerWidget;
import com.android.systemui.statusbar.policy.ToggleSlider;

/**
 * This class holds the preference views
 */
public abstract class StatusBarPreference {
    protected CompoundButton mCheckBox;
    protected LinearLayout mContentView;
    protected Context mContext;
    protected View mDivider;
    protected ImageView mIcon;
    //protected LinearLayout mItemView;
    protected MediaPlayerWidget mPlayer;
    protected ToggleSlider mSlider;
    PhoneStatusBar mService;
    protected TextView mSummary;
    protected TextView mTitle;
    protected String mTag;

    public StatusBarPreference(Context context, View view) {
        mContext = context;
        mTag = "Preference";
        mContentView = (LinearLayout)view;
        //mItemView = (LinearLayout)mContentView.getChildAt(0);
        mDivider = mContentView.getChildAt(1);
        setupViews();
    }

    private void setupViews(){
        mIcon = (ImageView)mContentView.findViewById(R.id.pref_icon);
        mTitle = (TextView)mContentView.findViewById(R.id.pref_label);
        mSummary = (TextView)mContentView.findViewById(R.id.pref_summary);
        mCheckBox = (CompoundButton)mContentView.findViewById(R.id.pref_checkbox);
        mSlider = (ToggleSlider)mContentView.findViewById(R.id.pref_slider);
        mPlayer = (MediaPlayerWidget)mContentView.findViewById(R.id.pref_player);
    }
    
    protected abstract void init();
    public abstract void release();
    public abstract void refreshResources();
    
    public StatusBarManager getStatusBarManager() {
        return (StatusBarManager)mContext.getSystemService(Context.STATUS_BAR_SERVICE);
    }
    
    public String getTag(){
        return mTag;
    }
    
    protected void launchActivity(Intent intent) throws ActivityNotFoundException {
    	// We take this as a good indicator that Setup is running and we shouldn't
        // allow you to go somewhere else
        if (!isDeviceProvisioned()) return;
    	try {
			// The intent we are sending is for the application, which
			// won't have permission to immediately start an activity after
			// the user switches to home. We know it is safe to do at this
			// point, so make sure new activity switches are now allowed.
			ActivityManagerNative.getDefault().resumeAppSwitches();
			// Also, notifications can be launched from the lock screen,
			// so dismiss the lock screen when the activity starts.
			ActivityManagerNative.getDefault()
					.dismissKeyguardOnNextActivity();
		} catch (RemoteException e) {
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
		
		getStatusBarManager().collapse();
    }
    
    private boolean isDeviceProvisioned(){
    	final boolean provisioned = 0 != Settings.Secure.getInt(
                mContext.getContentResolver(), Settings.Secure.DEVICE_PROVISIONED, 0);
    	return provisioned;
    }
    
}


