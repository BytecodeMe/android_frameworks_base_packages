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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.view.View;

import com.android.systemui.R;

import com.android.systemui.statusbar.phone.StatusBarPreference;

public class AirplaneMode extends StatusBarPreference
        implements CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {
    private static final String TAG = "QuickSettings.AirplaneMode";

    private boolean mAirplaneMode;

    public AirplaneMode(Context context, View view) {
        super(context, view);
        mAirplaneMode = getAirplaneMode();
        init();
    }
    
    @Override
	public boolean onLongClick(View v) {
		this.getStatusBarManager().collapse();
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
        	.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        
		return true;
	}
    
    @Override
    public void init() {
    	mContentView.setOnLongClickListener(this);
    	
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setChecked(mAirplaneMode);
        mCheckBox.setOnCheckedChangeListener(this);
        
        mIcon.setImageResource(R.drawable.ic_sysbar_airplane_on);
        mTitle.setText(R.string.status_bar_settings_airplane);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mContext.registerReceiver(mAirplaneModeReceiver, filter);
    }

    @Override
    public void release() {
        mContext.unregisterReceiver(mAirplaneModeReceiver);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != mAirplaneMode) {
            mAirplaneMode = checked;
            unsafe(checked);
        }
    }

    private BroadcastReceiver mAirplaneModeReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                final boolean enabled = intent.getBooleanExtra("state", false);
                if (enabled != mAirplaneMode) {
                    mAirplaneMode = enabled;
                    mCheckBox.setChecked(enabled);
                }
            }
        }
    };

    private boolean getAirplaneMode() {
        ContentResolver cr = mContext.getContentResolver();
        return 0 != Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0);
    }

    // TODO: Fix this racy API by adding something better to TelephonyManager or
    // ConnectivityService.
    private void unsafe(final boolean enabled) {
        AsyncTask.execute(new Runnable() {
                public void run() {
                    Settings.System.putInt(
                            mContext.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON,
                            enabled ? 1 : 0);
                    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    intent.putExtra("state", enabled);
                    mContext.sendBroadcast(intent);
                }
            });
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }
}

