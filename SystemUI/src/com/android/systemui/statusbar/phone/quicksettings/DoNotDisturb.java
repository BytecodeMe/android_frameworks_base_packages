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
import android.content.SharedPreferences;
import android.widget.CompoundButton;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Prefs;
import com.android.systemui.statusbar.phone.StatusBarPreference;

public class DoNotDisturb extends StatusBarPreference
    implements CompoundButton.OnCheckedChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "QuickSettings.DoNotDisturb";

    SharedPreferences mPrefs;

    private boolean mDoNotDisturb;

    public DoNotDisturb(Context context, View view) {
        super(context, view);

        mPrefs = Prefs.read(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mDoNotDisturb = mPrefs.getBoolean(Prefs.DO_NOT_DISTURB_PREF, Prefs.DO_NOT_DISTURB_DEFAULT);
        init();
    }
    
    @Override
    public void init() {
        mCheckBox.setVisibility(View.VISIBLE);
        mCheckBox.setOnCheckedChangeListener(this);
        mCheckBox.setChecked(!mDoNotDisturb);
        
        mIcon.setImageResource(R.drawable.ic_notification_open);
        mTitle.setText(R.string.status_bar_settings_notifications);
    }

    // The checkbox is ON for notifications coming in and OFF for Do not disturb, so we
    // don't have a double negative.
    public void onCheckedChanged(CompoundButton view, boolean checked) {
        //Slog.d(TAG, "onCheckedChanged checked=" + checked + " mDoNotDisturb=" + mDoNotDisturb);
        final boolean value = !checked;
        if (value != mDoNotDisturb) {
            SharedPreferences.Editor editor = Prefs.edit(mContext);
            editor.putBoolean(Prefs.DO_NOT_DISTURB_PREF, value);
            editor.apply();
        }
    }
    
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        final boolean val = prefs.getBoolean(Prefs.DO_NOT_DISTURB_PREF,
                Prefs.DO_NOT_DISTURB_DEFAULT);
        if (val != mDoNotDisturb) {
            mDoNotDisturb = val;
            mCheckBox.setChecked(!val);
        }
    }

    @Override
    public void release() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void refreshResources() {
        // TODO Auto-generated method stub
        
    }
}

