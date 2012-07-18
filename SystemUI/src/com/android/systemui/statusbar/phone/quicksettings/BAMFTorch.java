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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.CompoundButton;
import android.view.View;

import com.android.internal.widget.Flashlight;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class BAMFTorch extends StatusBarPreference
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "QuickSettings.BAMFTorch";
    private static final boolean DEBUG = true;

    private boolean mTorchMode;

    public BAMFTorch(Context context, View view) {
        super(context, view);
        
        mTorchMode = false;        
        init();
    }
    
    @Override
    public void init() {
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setChecked(mTorchMode);
        mCheckBox.setOnCheckedChangeListener(this);
        
        mTitle.setText(R.string.status_bar_settings_torch);
        mIcon.setImageResource(R.drawable.ic_sysbar_torch_on);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Flashlight.FLASHLIGHT_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mTorchReceiver, filter);
    }

    @Override
    public void release() {
        mContext.unregisterReceiver(mTorchReceiver);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != mTorchMode) {
            mTorchMode = checked;
            
            try{
                Intent intent = new Intent();
                intent.setClassName("com.bamf.ics.torch", "com.bamf.ics.torch.utils.TorchToggleService");
                intent.putExtra("mode", Flashlight.STATE_TOGGLE);
                mContext.startService(intent);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    private BroadcastReceiver mTorchReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Flashlight.FLASHLIGHT_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                final int state = intent.getIntExtra(Flashlight.EXTRA_FLASH_STATE, 0);
                boolean enabled = false;
                switch(state){
                    case Flashlight.STATE_OFF:
                        if(DEBUG){Log.d(TAG,"torch turned off");}
                        enabled = false;
                        break;
                    case Flashlight.STATE_DEATH_RAY:
                    case Flashlight.STATE_HIGH:
                    case Flashlight.STATE_STROBE:
                    case Flashlight.STATE_ON:
                        if(DEBUG){Log.d(TAG,"torch turned on");}
                        enabled = true;
                        break;
                    default:
                        break; 
                }
                mTorchMode = enabled;
                mCheckBox.setChecked(enabled);
            }
        }
        
    };

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }   

}

