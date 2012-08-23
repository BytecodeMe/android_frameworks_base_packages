/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Process;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.View.OnLongClickListener;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.PowToast;
import com.android.systemui.R;

public class CustomKeyButtonView extends KeyButtonView implements OnLongClickListener {
    private static final String TAG = "StatusBar.CustomKeyButtonView";
    
    final static String ACTION_NONE = "None";
    public final static String ACTION_DEFAULT = "Default"; 
    public final static String ACTION_DEFAULT_NONE = "Default(none)";
    final static String ACTION_MENU = "Menu";
    final static String ACTION_RECENT = "Recent Apps";
    final static String ACTION_KILL = "Kill Current App";
    public final static String ACTION_PICKER = "action_picker";
    
    final static int ID_MENU = R.id.menu_large;
    final static int ID_BACK = R.id.back;
    final static int ID_HOME = R.id.home;
    final static int ID_RECENT = R.id.recent_apps;
    final static int ID_SEARCH = R.id.search;
    
    final Object mServiceAquireLock = new Object();
    IStatusBarService mStatusBarService;
    
	private boolean mDisabled = false;	
	private String mLongPressFunction = ACTION_DEFAULT;
	private boolean mLongPressed = false;
	private ContentResolver mResolver;
	//for changes to the layout
    SettingsObserver mSettingsObserver = new SettingsObserver(new Handler());
    
    public CustomKeyButtonView(Context context) {
        this(context, null);
    }

    public CustomKeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomKeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);        
        mResolver = context.getContentResolver();
        mSettingsObserver.observe();
    }
    
    @Override
    public void setId(int id){
    	super.setId(id);    	
    }
    
    public void setKeyCode(int code){
    	mCode = code;
    }
    
    class SettingsObserver extends ContentObserver {
    	
    	ContentResolver resolver;
    	
        SettingsObserver(Handler handler) {
            super(handler);
        }
        
        void observe() {
        	resolver = mContext.getContentResolver();
		    resolver.registerContentObserver(
		    		Settings.System.getUriFor(Settings.System.LONG_ACTION_BACK), false, this);
		    resolver.registerContentObserver(
	            	Settings.System.getUriFor(Settings.System.LONG_ACTION_HOME), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.LONG_ACTION_MENU), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.LONG_ACTION_RECENT), false, this);            
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.LONG_ACTION_SEARCH), false, this);            
        }
        
        @Override
        public void onChange(boolean selfChange) {        	
            setLongPress();
        }
        
       
    }
    
    public void setLongPress(){
    	
    	boolean support = false;    	
    	String action = ACTION_DEFAULT;    	
    	
    	switch(getId()){
			case ID_RECENT:
				action = Settings.System.getString(mResolver,
						Settings.System.LONG_ACTION_RECENT, ACTION_DEFAULT_NONE);				
				break;
			case ID_SEARCH:
				action = Settings.System.getString(mResolver,
						Settings.System.LONG_ACTION_SEARCH, ACTION_DEFAULT);
				support = true;
				break;	
			case ID_HOME:
				action = Settings.System.getString(mResolver,
						Settings.System.LONG_ACTION_HOME, ACTION_DEFAULT);						
				break;
			case ID_BACK:
				action = Settings.System.getString(mResolver,
						Settings.System.LONG_ACTION_BACK, ACTION_DEFAULT_NONE);				
				break;
			case ID_MENU:
				action = Settings.System.getString(mResolver,
						Settings.System.LONG_ACTION_MENU, ACTION_DEFAULT_NONE);					
				break;
    	}
    	if(!action.equals(ACTION_DEFAULT) && !action.equals(ACTION_DEFAULT_NONE) && !action.equals(ACTION_NONE)){ 		
		    support = true;			
			setOnLongClickListener(this);			
		}	
    	mLongPressFunction = action;
    	mSupportsLongpress = support;
	
	}    
    
    public void setGlowBG(boolean land){
    	mGlowBG = getResources().getDrawable(land ? R.drawable.ic_sysbar_highlight_land : R.drawable.ic_sysbar_highlight);
        if (mGlowBG != null) {
            setDrawingAlpha(BUTTON_QUIESCENT_ALPHA);
            mGlowWidth = mGlowBG.getIntrinsicWidth();
            mGlowHeight = mGlowBG.getIntrinsicHeight();
        }
    }

	public void disable() {
		setKeyCode(0);
		setVisibility(View.INVISIBLE);
		setClickable(false);
		mDisabled = true;
	}
	
	public void enable() {
		setClickable(true);
		setKeyCode(KeyEvent.KEYCODE_MENU);
		mDisabled = false;
	}
    
	public boolean isDisabled(){
		return mDisabled;
	}	
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        int x, y;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //Slog.d("KeyButtonView", "press");
                mDownTime = SystemClock.uptimeMillis();
                setPressed(true);                
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
                } else {
                    // Provide the same haptic feedback that the system offers for virtual keys.
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                if (mSupportsLongpress) {                	
                    removeCallbacks(mLongPressCheck);
                    postDelayed(mLongPressCheck, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                x = (int)ev.getX();
                y = (int)ev.getY();
                setPressed(x >= -mTouchSlop
                        && x < getWidth() + mTouchSlop
                        && y >= -mTouchSlop
                        && y < getHeight() + mTouchSlop);
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);                
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                }
                if (mSupportsLongpress) {
                    removeCallbacks(mLongPressCheck);
                }
                break;
            case MotionEvent.ACTION_UP:
                final boolean doIt = isPressed();
                setPressed(false);                
                if ((mCode != 0 && !mLongPressed)) {
                    if (doIt) {                    	
                        sendEvent(KeyEvent.ACTION_UP, 0);
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                        playSoundEffect(SoundEffectConstants.CLICK);
                    } else {                    	
                        sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                    }
                } else {
                    // no key code, just a regular ImageView
                    if (doIt && !mLongPressed) {                    	
                        performClick();
                    }else if(mLongPressed){
                    	mLongPressed = false;
                    }
                }
                if (mSupportsLongpress) {
                    removeCallbacks(mLongPressCheck);
                }
                break;
        }

        return true;
    }
    
	Runnable mLongPressCheck = new Runnable() {
        public void run() {
            if (isPressed()) {                
                if (mCode != 0 && (mLongPressFunction == ACTION_NONE ||
                		mLongPressFunction == ACTION_DEFAULT || mLongPressFunction == ACTION_DEFAULT_NONE)) {
                	sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                } else {
                	// Just an old-fashioned ImageView                	
                	mLongPressed = true;                	
                    performLongClick();
                }
            }
        }
    };    
    
	@Override
	public boolean onLongClick(View v) {
		
		if(mLongPressFunction.equals(ACTION_MENU)){
			int oldCode = mCode;
			mCode = 82;
			sendEvent(KeyEvent.ACTION_DOWN, 0);
			sendEvent(KeyEvent.ACTION_UP, 0);
			mCode = oldCode;	
			return true;
		}else if(mLongPressFunction.equals(ACTION_RECENT)){		   
			try {
                IStatusBarService statusbar = getStatusBarService();
                if (statusbar != null) {
                    statusbar.toggleRecentApps();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException when showing recent apps", e);
                // re-acquire status bar service next time it is needed.
                mStatusBarService = null;
            }
		    return true;
		}else if(mLongPressFunction.equals(ACTION_KILL)){			
			return performKill();
		}else if(mLongPressFunction.equals(ACTION_PICKER)){
			Intent intent = new Intent(Settings.ACTION_SHOW_INPUT_METHOD_PICKER);
			mContext.sendBroadcast(intent);
		}else{
			performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			launchUserApp(mLongPressFunction);			
		}
		return true;
	}
	
	private void launchUserApp(String action){
    	
    	try{
    		Intent intent = Intent.parseUri(action, 0);
    		if(intent!=null){
    			launchActivity(intent);
    		}
    	}catch(Exception e){}
    }

    private void launchActivity(Intent intent) {    	
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	try {
    		mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Activity not found for intent + " + intent.getAction());
        }        
    }

	/**
	 * Courtesy of CyanogenMod
	 */
	private boolean performKill() {
		boolean targetKilled = false;
		try {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            String defaultHomePackage = "com.android.launcher";
            intent.addCategory(Intent.CATEGORY_HOME);
            final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
            if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                defaultHomePackage = res.activityInfo.packageName;
            }            
            IActivityManager am = ActivityManagerNative.getDefault();
            List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
            for (RunningAppProcessInfo appInfo : apps) {
                int uid = appInfo.uid;
                // Make sure it's a foreground user application (not system,
                // root, phone, etc.)
                if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID
                        && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    if (appInfo.pkgList != null && (appInfo.pkgList.length > 0)) {
                        for (String pkg : appInfo.pkgList) {
                            if (!pkg.equals("com.android.systemui") && !pkg.equals(defaultHomePackage)) {
                                am.forceStopPackage(pkg);
                                targetKilled = true;
                                break;
                            }
                        }
                    } else {
                        Process.killProcess(appInfo.pid);
                        targetKilled = true;
                    }
                }
                if (targetKilled) {                    
                    //Toast.makeText(mContext, "Unable to kill Application", Toast.LENGTH_SHORT).show();
                	Intent pow = new Intent(mContext, PowToast.class);
                	pow.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                	mContext.startActivity(pow);
                    break;
                }
            }
        } catch (RemoteException remoteException) {
            // Do nothing; just let it go.
        }
		return targetKilled;
		
	}

	IStatusBarService getStatusBarService() {
        synchronized (mServiceAquireLock) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }
}


