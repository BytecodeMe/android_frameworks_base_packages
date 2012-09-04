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
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class SettingsShortcut extends StatusBarPreference
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "QuickSettings.Settings";

    public SettingsShortcut(Context context, View view) {
        super(context, view);
        
        init();
    }
    
    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	mContentView.setOnClickListener(this);
    	
        mIcon.setImageResource(R.drawable.ic_sysbar_quicksettings);
        mTitle.setText(R.string.status_bar_settings_settings_button);    
    }

    @Override
    public void release() {
    }

    @Override
    public void onClick(View v) {
    	launchActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));  
    }
    
    @Override
	public boolean onLongClick(View v) {
        launchActivity(new Intent(Intent.ACTION_MAIN)
        	.setClassName("com.bamf.settings", "com.bamf.settings.activities.SettingsActivity"));
		return true;
	}

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }


}

