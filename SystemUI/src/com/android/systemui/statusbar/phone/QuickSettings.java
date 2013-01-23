/*
 * Copyright (C) 2012 The Android Open Source Project
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.quicktiles.*;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;


/**
 *
 */
class QuickSettings {
	private static final String TAG = "QuickSettings";
	public static final boolean SHOW_IME_TILE = false;
	private static final boolean DEBUG = true;

	private ArrayList<QuickSettingsTileContent> mAllCustomTiles;
	private String mLoadedSettings;
	private boolean mTilesSetUp = false;

	private final HashMap<String, Boolean> mConfigs = new HashMap<String, Boolean>();

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
	private static final String QUICK_SIGNAL = "QuickSignal";
	private static final String QUICK_BATTERY = "QuickBattery";
	private static final String QUICK_ALARM = "QuickAlarm";
	private static final String QUICK_USER = "QuickUser";


	private static final HashMap<String, Class<? extends QuickSettingsTileContent>> SETTINGS = 
			new HashMap<String, Class<? extends QuickSettingsTileContent>>();

	// TODO: ones that are not converted yet are commented out here
	static{
		SETTINGS.put(QUICK_AIRPLANE, AirplaneModeTile.class);
		SETTINGS.put(QUICK_ROTATE, AutoRotateTile.class);
		SETTINGS.put(QUICK_BRIGHTNESS, BrightnessTile.class);
		SETTINGS.put(QUICK_NODISTURB, DoNotDisturbTile.class); 
		SETTINGS.put(QUICK_TORCH, TorchTile.class);
		SETTINGS.put(QUICK_SETTING, SettingsShortcutTile.class);
		SETTINGS.put(QUICK_WIFI, WifiTile.class); 
		//SETTINGS.put(QUICK_VOLUME, Volume.class);
		SETTINGS.put(QUICK_LTE, LTETile.class);
		SETTINGS.put(QUICK_CUSTOM, CustomTile.class);
		SETTINGS.put(QUICK_BLUETOOTH, BluetoothTile.class);
		SETTINGS.put(QUICK_ADB, WirelessADBTile.class);
		SETTINGS.put(QUICK_GPS, GPSModeTile.class);
		SETTINGS.put(QUICK_MOBILE_DATA, MobileDataTile.class);
		SETTINGS.put(QUICK_SYNC, SyncDataTile.class);
		SETTINGS.put(QUICK_MEDIA, AlbumArtTile.class);
		SETTINGS.put(QUICK_HOTSPOT, HotspotTile.class);
		SETTINGS.put(QUICK_TETHER, USBTetherTile.class);
		SETTINGS.put(QUICK_SIGNAL, SignalTile.class);
		SETTINGS.put(QUICK_BATTERY, BatteryTile.class);
		SETTINGS.put(QUICK_ALARM, AlarmTile.class);
		SETTINGS.put(QUICK_USER, UserTile.class);
	}

	private static final String SETTING_DELIMITER = "|";
	private static final String SUB_DELIMITER = ",";

	// do not use anything here that may not work on ALL devices
	private static final String SETTINGS_DEFAULT = QUICK_AIRPLANE
			+ SETTING_DELIMITER + QUICK_MEDIA
			+ SETTING_DELIMITER + QUICK_VOLUME
			+ SETTING_DELIMITER + QUICK_ROTATE
			+ SETTING_DELIMITER + QUICK_BRIGHTNESS
			+ SETTING_DELIMITER + QUICK_SETTING;

	// this is only for testing, do not use
	private static final String SETTINGS_ALL = QUICK_USER + SUB_DELIMITER + "1,3"
			+ SETTING_DELIMITER + QUICK_LTE + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_SIGNAL + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_BATTERY + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_GPS + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_WIFI + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_NODISTURB + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_MEDIA + SUB_DELIMITER + "2,2"
			+ SETTING_DELIMITER + QUICK_TORCH + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_CUSTOM + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_ADB + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_AIRPLANE + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_ROTATE + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_SETTING + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_MOBILE_DATA + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_TETHER + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_BLUETOOTH + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_HOTSPOT + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_BRIGHTNESS + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_SYNC + SUB_DELIMITER + "1,1"
			+ SETTING_DELIMITER + QUICK_ALARM + SUB_DELIMITER + "1,1";

	private static final String EMPTY_STRING = "";

	private Context mContext;
	private PanelBar mBar;
	private ViewGroup mContainerView;
	private PhoneStatusBar mStatusBarService;

	public QuickSettings(Context context, QuickSettingsContainerView container) {

		mContext = context;
		mContainerView = container;

		mLoadedSettings = EMPTY_STRING;

		Resources r = mContext.getResources();

		// setup config values - only need to load these once
		// TODO: these configs need to use updated APIs now available

		mConfigs.put(QUICK_TORCH, mContext.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingTorch));
		mConfigs.put(QUICK_LTE, mContext.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingLTE));
		mConfigs.put(QUICK_MOBILE_DATA, mContext.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingMobileData));
		mConfigs.put(QUICK_HOTSPOT, context.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingMobileData));
		mConfigs.put(QUICK_TETHER, context.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingMobileData));

	}
	
	void setBar(PanelBar bar) {
        mBar = bar;
    }

	public void setService(PhoneStatusBar phoneStatusBar) {
		mStatusBarService = phoneStatusBar;
	}

	public PhoneStatusBar getService() {
		return mStatusBarService;
	}

	public void setImeWindowStatus(boolean visible) {
		//mModel.onImeWindowStatusChanged(visible);
	}

	void setup(NetworkController networkController, BluetoothController bluetoothController,
			BatteryController batteryController, LocationController locationController) {

		setupQuickSettings();
		updateResources();

		for(QuickSettingsTileContent qs: mAllCustomTiles){
			if(qs instanceof WifiTile){
				networkController.addNetworkSignalChangedCallback((WifiTile)qs);
			}
			if(qs instanceof SignalTile){
				networkController.addNetworkSignalChangedCallback((SignalTile)qs);
			}
			if(qs instanceof BatteryTile){
				batteryController.addStateChangedCallback((BatteryTile)qs);
			}
		}
	}

	private void setupQuickSettings() {
		// Setup the tiles that we are going to be showing (including the temporary ones)
		LayoutInflater inflater = LayoutInflater.from(mContext);

		// add custom tiles here for now
		addCustomTiles(mContainerView, inflater);

		mTilesSetUp = true;
	}

	public void addCustomTiles(final ViewGroup parent, LayoutInflater inflater){

		String settings = Settings.System.getString(mContext.getContentResolver(), 
				Settings.System.QUICK_SETTINGS_TILES);
		if(settings == null) {
			if(DEBUG)Log.i(TAG, "Default settings being loaded");
			settings = SETTINGS_DEFAULT;
		}

		// TODO: remove this after testing
		settings = SETTINGS_ALL;

		if(mLoadedSettings.equals(settings)){
			if(DEBUG)Log.i(TAG, "no changes; not reloading");
			return;
		}

		// just in case one sneaks in, get rid of it
		for(String config: mConfigs.keySet()){
			if(settings.contains(config) && !mConfigs.get(config)){
				String removeMe = settings.substring(
						settings.indexOf(config),
						settings.indexOf("|", settings.indexOf(config))+1);
				if(DEBUG)Log.d(TAG, "removeMe: "+removeMe);
				settings = settings.replace(removeMe, EMPTY_STRING).replace("||", "|");
			}
		}

		mLoadedSettings = settings;

		//removeAllViews();

		mAllCustomTiles = new ArrayList<QuickSettingsTileContent>();
		int count = 0;
		for(String setting : settings.split("\\|")) {
			if(DEBUG)Log.i(TAG, "Inflating setting: " + setting);
			String[] settingParts = setting.split(",");
			if(SETTINGS.containsKey(settingParts[0])){
				try {
					// inflate the setting
					final QuickSettingsTileView tileView = (QuickSettingsTileView)inflater.inflate(R.layout.quick_settings_tile, mContainerView, false);
					tileView.setContent(R.layout.quick_settings_tile_general, inflater);
					// get the tile size from the setting
					tileView.setRowSpan(Integer.parseInt(settingParts[1]));
					tileView.setColumnSpan(Integer.parseInt(settingParts[2]));

					Class<?> cls = SETTINGS.get(settingParts[0]);

					Constructor<?> con = cls.getConstructor(new Class[]{Context.class, View.class});
					QuickSettingsTileContent pref = 
							(QuickSettingsTileContent)con.newInstance(new Object[]{mContext, tileView.getChildAt(0)});
					final int pos = count;
					pref.mCallBack = new QuickSettingsTileContent.TileCallback(){
						int position = pos;
						@Override
						public void changeSize(int height, int width) {
							// removing and adding the view to take advantage
							// of the layout transitions
							position = parent.indexOfChild(tileView);
							parent.removeView(tileView);
							tileView.setRowSpan(height);
							tileView.setColumnSpan(width);
							parent.addView(tileView, position);
						}

						@Override
						public void show(boolean visible) {
							if(visible && parent.indexOfChild(tileView)==-1){
								parent.addView(tileView, position);
							}else if (!visible && parent.indexOfChild(tileView)!=-1){
								position = parent.indexOfChild(tileView);
								parent.removeView(tileView);
							}
						}

						@Override
						public void refreshTiles() {
							updateResources();
						}

					};
					// add it to the view here
					parent.addView(tileView);
					mAllCustomTiles.add(pref);

					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}         
			}
		}
	}

	void updateResources() {

		// update custom tiles
		for(QuickSettingsTileContent qs : mAllCustomTiles){
			try{
				qs.refreshResources();
			}catch(Exception e){
				Log.e(TAG, "Error on refresh ("+((qs==null)?"None selected":qs.getTag())+")");
			}
		}

		((QuickSettingsContainerView)mContainerView).updateResources();
		mContainerView.requestLayout();
	}

}
