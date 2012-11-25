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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.widget.TextView;

import com.android.internal.telephony.TelephonyIntents;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.android.internal.R;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
public class CarrierLabel extends TextView {
	private static final String DEFAULT_CUSTOM_LABEL = "BAMF Paradigm";
    private boolean mAttached;
    private boolean mUseCustomString;
    private String mLastUpdatedString = "";
    
    private SettingsObserver mSettingsObserver;

    public CarrierLabel(Context context) {
        this(context, null);
    }

    public CarrierLabel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarrierLabel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateNetworkName(false, null, false, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
            if (mSettingsObserver == null) {
                mSettingsObserver = new SettingsObserver(getHandler());
                mSettingsObserver.observe();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            if (mSettingsObserver != null) {
                mSettingsObserver.stop();
                mSettingsObserver = null;
            }
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                        intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
            }
        }
    };

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (false) {
            Slog.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append('\n');
            }
            str.append(spn);
            something = true;
        }
        if (something) {
        	mLastUpdatedString = str.toString();
        	if(!mUseCustomString)
        		setText(str.toString());
        } else {
        	mLastUpdatedString = getContext().getString(com.android.internal.R.string.lockscreen_carrier_default);
        	if(!mUseCustomString)
        		setText(com.android.internal.R.string.lockscreen_carrier_default);
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
            		Settings.System.CUSTOM_CARRIER_LABEL), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
            		Settings.System.USE_CUSTOM_CARRIER_LABEL), false, this);
            update();
        }

        public void stop() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            mUseCustomString = Settings.System.getInt(mContext.getContentResolver(),
            		Settings.System.USE_CUSTOM_CARRIER_LABEL,0)==1;
            if(mUseCustomString){
            	 String temp = Settings.System.getString(mContext.getContentResolver(), 
            			Settings.System.CUSTOM_CARRIER_LABEL);
            	 if(temp!=null){
            		 setText(temp);
            	 }else{
            		 setText(DEFAULT_CUSTOM_LABEL);
            	 }
            }else{
            	setText(mLastUpdatedString);
            }
            
        }
    }
}


