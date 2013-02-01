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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.provider.Settings;
import android.text.util.QuickTileToken;
import android.text.util.QuickTileTokenizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.quicktiles.*;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;


/**
 *
 */
class QuickSettings {
	private static final String TAG = "QuickSettings";
	public static final boolean SHOW_IME_TILE = false;
	private static final boolean DEBUG = true;

	private ArrayList<QuickSettingsTileContent> mAllCustomTiles = new ArrayList<QuickSettingsTileContent>();
	private String mLoadedSettings;
	private boolean mTilesSetUp = false;

	private final HashMap<String, Boolean> mConfigs = new HashMap<String, Boolean>();
	private static final HashMap<String, Class<? extends QuickSettingsTileContent>> SETTINGS = 
			new HashMap<String, Class<? extends QuickSettingsTileContent>>();

	// TODO: ones that are not converted yet are commented out here
	static{
		SETTINGS.put(Settings.System.QUICK_AIRPLANE, AirplaneModeTile.class);
		SETTINGS.put(Settings.System.QUICK_ROTATE, AutoRotateTile.class);
		SETTINGS.put(Settings.System.QUICK_BRIGHTNESS, BrightnessTile.class);
		SETTINGS.put(Settings.System.QUICK_NODISTURB, DoNotDisturbTile.class); 
		SETTINGS.put(Settings.System.QUICK_TORCH, TorchTile.class);
		SETTINGS.put(Settings.System.QUICK_SETTING, SettingsShortcutTile.class);
		SETTINGS.put(Settings.System.QUICK_WIFI, WifiTile.class); 
		//SETTINGS.put(Settings.System.QUICK_VOLUME, Volume.class);
		SETTINGS.put(Settings.System.QUICK_LTE, LTETile.class);
		SETTINGS.put(Settings.System.QUICK_CUSTOM, CustomTile.class);
		SETTINGS.put(Settings.System.QUICK_BLUETOOTH, BluetoothTile.class);
		SETTINGS.put(Settings.System.QUICK_ADB, WirelessADBTile.class);
		SETTINGS.put(Settings.System.QUICK_GPS, GPSModeTile.class);
		SETTINGS.put(Settings.System.QUICK_MOBILE_DATA, MobileDataTile.class);
		SETTINGS.put(Settings.System.QUICK_SYNC, SyncDataTile.class);
		SETTINGS.put(Settings.System.QUICK_MEDIA, AlbumArtTile.class);
		SETTINGS.put(Settings.System.QUICK_HOTSPOT, HotspotTile.class);
		SETTINGS.put(Settings.System.QUICK_TETHER, USBTetherTile.class);
		SETTINGS.put(Settings.System.QUICK_SIGNAL, SignalTile.class);
		SETTINGS.put(Settings.System.QUICK_BATTERY, BatteryTile.class);
		SETTINGS.put(Settings.System.QUICK_ALARM, AlarmTile.class);
		SETTINGS.put(Settings.System.QUICK_USER, UserTile.class);
	}

	private static final String SETTING_DELIMITER = "|";

	// do not use anything here that may not work on ALL devices
	private static final String SETTINGS_DEFAULT = new QuickTileToken(Settings.System.QUICK_USER,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_AIRPLANE,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_MEDIA,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_VOLUME,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_ROTATE,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_BRIGHTNESS,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_SETTING,1,1).toString();

	// this is only for testing, do not use
	private static final String SETTINGS_ALL = new QuickTileToken(Settings.System.QUICK_USER,1,3).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_LTE,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_SIGNAL,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_BATTERY,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_GPS,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_WIFI,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_NODISTURB,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_MEDIA,2,2).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_TORCH,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_CUSTOM,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_ADB,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_AIRPLANE,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_ROTATE,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_SETTING,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_MOBILE_DATA,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_TETHER,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_BLUETOOTH,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_HOTSPOT,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_BRIGHTNESS,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_SYNC,1,1).toString()
			+ SETTING_DELIMITER + new QuickTileToken(Settings.System.QUICK_ALARM,1,1).toString();

	private static final String EMPTY_STRING = "";

	private Context mContext;
	private PanelBar mBar;
	private ViewGroup mContainerView;
	private PhoneStatusBar mStatusBarService;

	private boolean mEggEnabled;
	private Handler mHandler = new Handler();

	public QuickSettings(Context context, QuickSettingsContainerView container) {

		mContext = context;
		mContainerView = container;

		mLoadedSettings = EMPTY_STRING;

		Resources r = mContext.getResources();

		// setup config values - only need to load these once
		// TODO: these configs need to use updated APIs now available

		mConfigs.put(Settings.System.QUICK_TORCH, mContext.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingTorch));
		mConfigs.put(Settings.System.QUICK_LTE, mContext.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingLTE));
		mConfigs.put(Settings.System.QUICK_MOBILE_DATA, mContext.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingMobileData));
		mConfigs.put(Settings.System.QUICK_HOTSPOT, context.getResources()
				.getBoolean(com.android.internal.R.bool.config_allowQuickSettingMobileData));
		mConfigs.put(Settings.System.QUICK_TETHER, context.getResources()
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

	void toggleEgg() {
		// used strictly for the easter egg
		mEggEnabled = !mEggEnabled;
		int delay = 0;
		for(int x = 0; x < mContainerView.getChildCount(); x++){
			QuickSettingsTileView tileView = (QuickSettingsTileView)mContainerView.getChildAt(x);
			delay += 100;
			flipTile(tileView, delay);
		}
	}
	
	private void flipTile(final QuickSettingsTileView view, int delay){
    	final AnimatorSet anim = (AnimatorSet) AnimatorInflater.loadAnimator(mContext, R.anim.flip_right);
		anim.setTarget(view);
		anim.setDuration(200);		
		anim.addListener(new AnimatorListener(){

			@Override
			public void onAnimationEnd(Animator animation) {
				if(view!=null){
					view.setEgg(mEggEnabled);
				}
			}
			@Override
			public void onAnimationStart(Animator animation) {}
			@Override
			public void onAnimationCancel(Animator animation) {}
			@Override
			public void onAnimationRepeat(Animator animation) {}
			
		});
		
		Runnable doAnimation = new Runnable(){
			@Override
			public void run() {
				anim.start();
			}
		};
		
		mHandler.postDelayed(doAnimation, delay);
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

	private void addCustomTiles(final ViewGroup parent, final LayoutInflater inflater){

		String savedSettings = Settings.System.getString(mContext.getContentResolver(), 
				Settings.System.QUICK_SETTINGS_TILES);
		if(savedSettings == null) {
			if(DEBUG)Log.i(TAG, "Default settings being loaded");
			savedSettings = SETTINGS_DEFAULT;
		}

		// TODO: remove this after testing
		savedSettings = SETTINGS_ALL;

		if(DEBUG)Log.i(TAG, "quick tiles: "+savedSettings);

		if(mLoadedSettings.equals(savedSettings)){
			if(DEBUG)Log.i(TAG, "no changes; not reloading");
			return;
		}

		// just in case one sneaks in, get rid of it
		for(String config: mConfigs.keySet()){
			if(savedSettings.contains(config) && !mConfigs.get(config)){
				String removeMe = savedSettings.substring(
						savedSettings.indexOf(config),
						savedSettings.indexOf("|", savedSettings.indexOf(config))+1);
				if(DEBUG)Log.d(TAG, "removeMe: "+removeMe);
				savedSettings = savedSettings.replace(removeMe, EMPTY_STRING).replace("||", "|");
			}
		}


		final String settings = savedSettings;
		mLoadedSettings = settings;
		
		int count = 0;
		for(QuickTileToken token : QuickTileTokenizer.tokenize(settings)) {
			if(DEBUG)Log.i(TAG, "Inflating setting: " + token.getName());

			if(SETTINGS.containsKey(token.getName())){
				try {
					// inflate the setting
					final QuickSettingsTileView tileView = (QuickSettingsTileView)inflater.inflate(R.layout.quick_settings_tile, mContainerView, false);
					tileView.setContent(R.layout.quick_settings_tile_general, inflater);
					// get the tile size from the setting
					tileView.setRowSpan(token.getRows());
					tileView.setColumnSpan(token.getColumns());
					tileView.setEgg(mEggEnabled);

					Class<?> cls = SETTINGS.get(token.getName());

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

	void removeAll(){
		if(mAllCustomTiles.size()==0) return;
		
		for(QuickSettingsTileContent qs : mAllCustomTiles){
			try{
				qs.release();
				qs = null;
			}catch(Exception e){
				Log.e(TAG, "Error on remove ("+((qs==null)?"None selected":qs.getTag())+")");
			}
		}
		mContainerView.removeAllViews();
		mAllCustomTiles.clear();
		
	}

}
