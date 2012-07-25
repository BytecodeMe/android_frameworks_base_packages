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
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PreferenceView;
import com.android.systemui.statusbar.phone.StatusBarPreference;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class QuickSettings extends LinearLayout {
    static final String TAG = "SettingsView";
    
    private StatusBarPreference[] mSettingItems;
    private String mLoadedSettings;
    private LayoutInflater mInflater;
    private Context mContext;
    
    /**
     *  These must be in sync with QuickSettingsUtil in BAMF settings and vice versa
     */
    //TODO: move these to Settings.System in an array instead of maintaining two copies
    private static final String QUICK_AIRPLANE = "QuickAirplane";
    private static final String QUICK_ROTATE = "QuickRotate";
    private static final String QUICK_BRIGHTNESS = "QuickBrightness";
    private static final String QUICK_BLUETOOTH = "QuickBluetooth";
    private static final String QUICK_NODISTURB = "QuickNoDisturb";
    private static final String QUICK_TORCH = "QuickTorch";
    private static final String QUICK_SETTING = "QuickSetting";
    private static final String QUICK_WIFI = "QuickWifi";
    private static final String QUICK_VOLUME = "QuickVolume";
    private static final String QUICK_LTE = "QuickLTE";
    private static final String QUICK_CUSTOM = "QuickCustom";
    private static final String QUICK_ADB = "QuickAdb";
    private static final String QUICK_GPS = "QuickGPS";
    private static final String QUICK_MOBILE_DATA = "QuickMobileData";
    private static final String QUICK_SYNC = "QuickSync";
    private static final String QUICK_MEDIA = "QuickMedia";
    private static final String QUICK_HOTSPOT = "QuickHotspot";
    private static final String QUICK_TETHER = "QuickTether";
    
    
    private static final HashMap<String, Class<? extends StatusBarPreference>> SETTINGS = 
            new HashMap<String, Class<? extends StatusBarPreference>>();
    
    static{
        SETTINGS.put(QUICK_AIRPLANE, AirplaneMode.class);
        SETTINGS.put(QUICK_ROTATE, AutoRotate.class);
        SETTINGS.put(QUICK_BRIGHTNESS, Brightness.class);
        SETTINGS.put(QUICK_NODISTURB, DoNotDisturb.class); 
        SETTINGS.put(QUICK_TORCH, BAMFTorch.class);
        SETTINGS.put(QUICK_SETTING, SettingsShortcut.class);
        SETTINGS.put(QUICK_WIFI, Wifi.class); 
        SETTINGS.put(QUICK_VOLUME, Volume.class);
        SETTINGS.put(QUICK_LTE, LTE.class);
        SETTINGS.put(QUICK_CUSTOM, Custom.class);
        SETTINGS.put(QUICK_BLUETOOTH, Bluetooth.class);
        SETTINGS.put(QUICK_ADB, WirelessADB.class);
        SETTINGS.put(QUICK_GPS, GPSMode.class);
        SETTINGS.put(QUICK_MOBILE_DATA, MobileData.class);
        SETTINGS.put(QUICK_SYNC, SyncData.class);
        SETTINGS.put(QUICK_MEDIA, MediaPlayer.class);
        SETTINGS.put(QUICK_HOTSPOT, Hotspot.class);
        SETTINGS.put(QUICK_TETHER, USBTether.class);
    }
    
    private static final String SETTING_DELIMITER = "|";
    private static final String SETTINGS_DEFAULT = QUICK_AIRPLANE
                             + SETTING_DELIMITER + QUICK_TORCH
                             + SETTING_DELIMITER + QUICK_VOLUME
                             + SETTING_DELIMITER + QUICK_ROTATE
                             + SETTING_DELIMITER + QUICK_BRIGHTNESS
                             + SETTING_DELIMITER + QUICK_SETTING;
    
    private static final String EMPTY_STRING = "";
    

    public QuickSettings(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickSettings(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mLoadedSettings = EMPTY_STRING;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    public boolean isDirty(){
        String settings = Settings.System.getString(mContext.getContentResolver(), 
                Settings.System.QUICK_SETTINGS);
        
        if(mLoadedSettings.equals(settings)){
            return false;
        }
        
        return true;
    }
    
    public void loadSettings(){
        
        String settings = Settings.System.getString(mContext.getContentResolver(), 
                Settings.System.QUICK_SETTINGS);
        if(settings == null) {
            Log.i(TAG, "Default settings being loaded");
            settings = SETTINGS_DEFAULT;
        }
        
        if(mLoadedSettings.equals(settings)){
            Log.i(TAG, "no changes; not reloading");
            return;
        }
        
        mLoadedSettings = settings;
        
        removeAllViews();
        
        mSettingItems = new StatusBarPreference[settings.split("\\|").length];
        int count = 0;
        for(String setting : settings.split("\\|")) {
            Log.i(TAG, "Inflating setting: " + setting);

            if(SETTINGS.containsKey(setting)){
                try {
                    // inflate the setting
                    LinearLayout settingView = (LinearLayout)mInflater.inflate(R.layout.status_bar_preference, null, false);                                      
                    Class<?> cls = SETTINGS.get(setting);
                    Constructor<?> con = cls.getConstructor(new Class[]{Context.class, View.class});
                    StatusBarPreference pref = 
                            (StatusBarPreference)con.newInstance(new Object[]{mContext, settingView.getChildAt(0)});
                    mSettingItems[count] = pref;
                    
                    // add it to the view here
                    addView(settingView);
                    
                    count++;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }         
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        loadSettings();
    }

    public void release(){
        synchronized (mSettingItems) {
            // cycle through our settings and release them
            for(StatusBarPreference qs : mSettingItems) {
                try{
                    qs.release();
                }catch(Exception e){
                    Log.e(TAG, "Error on release ("+((qs==null)?"None selected":qs.getTag())+")");
                }
            }
            
        }
    }
    
    public void refreshResources(){
        synchronized (mSettingItems) {
            // cycle through our settings and refresh anything necessary
            for(StatusBarPreference qs : mSettingItems) {
                try{
                    qs.refreshResources();
                }catch(Exception e){
                    Log.e(TAG, "Error on refresh ("+((qs==null)?"None selected":qs.getTag())+")");
                }
            }
            
        }
    }

}

