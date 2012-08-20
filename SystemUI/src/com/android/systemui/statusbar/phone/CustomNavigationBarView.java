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

package com.android.systemui.statusbar.phone;

import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar.SettingsObserver;
import com.android.systemui.statusbar.policy.CustomKeyButtonView;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class CustomNavigationBarView extends NavigationBarView {
    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/CustomNavigationBarView"; 
    
    final static int MENU_DEFAULT = 0;
    final static int MENU_DEFAULT_HIDDEN = 1;
    final static int MENU_LEFT = 2;
    final static int MENU_LEFT_HIDDEN = 3;
    final static int MENU_BOTH = 4;
    final static int MENU_BOTH_HIDDEN = 5;
    final static int MENU_DISABLED = 6;
    
    final static int ID_MENU = R.id.menu_large;
    
    private int mMenuWidth;
    private int mKeyWidth;
    private Context mContext;
    private boolean mHasMenuKey = false;
    private boolean mHideLeft = false;
    private boolean mHideRight = false;
    
    public int mMenuMode = 0;
	private Drawable mMenuLeftIcon, mMenuLeftLandIcon, mMenuLeftAltIcon, mMenuLeftAltLandIcon;
	private Drawable mMenuRightIcon, mMenuRightLandIcon, mMenuRightAltIcon, mMenuRightAltLandIcon;
	
	//for changes to the layout
    SettingsObserver mSettingsObserver = new SettingsObserver(new Handler());

    public View getLeftMenuButton() {
        return mCurrentView.findViewById(R.id.menu_left);
    }
    
    public View getLargeMenuButton() {
        return mCurrentView.findViewById(ID_MENU);
    }    

    public CustomNavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        final Resources res = getResources();
        mMenuWidth = res.getDimensionPixelSize(R.dimen.navigation_menu_key_width);        
        mContext = context;
        mMenuMode = Settings.System.getInt(context.getContentResolver(), Settings.System.NAVBAR_MENU_MODE,MENU_DEFAULT);        
        mMenuLeftIcon = res.getDrawable(R.drawable.ic_sysbar_menu);
        mMenuLeftLandIcon = res.getDrawable(R.drawable.ic_sysbar_menu_land);
        mMenuLeftAltIcon = res.getDrawable(R.drawable.ic_sysbar_left_arrow);
        mMenuLeftAltLandIcon = res.getDrawable(R.drawable.ic_sysbar_left_arrow_land);
        
        mMenuRightIcon = res.getDrawable(R.drawable.ic_sysbar_menu);
        mMenuRightLandIcon = res.getDrawable(R.drawable.ic_sysbar_menu_land);
        mMenuRightAltIcon = res.getDrawable(R.drawable.ic_sysbar_right_arrow);
        mMenuRightAltLandIcon = res.getDrawable(R.drawable.ic_sysbar_right_arrow_land);
        
        mSettingsObserver.observe();
    }
    
    class SettingsObserver extends ContentObserver {
    	
    	ContentResolver resolver;
    	
        SettingsObserver(Handler handler) {
            super(handler);
        }
        
        void observe() {
        	resolver = mContext.getContentResolver();
		    
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVBAR_MENU_MODE), false, this); 
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVBAR_KEY_ORDER), false, this); 
        }
        
        @Override
        public void onChange(boolean selfChange) {        	
            update();
        }
        
        public void update(){  
        	mMenuMode = Settings.System.getInt(mContext.getContentResolver(), Settings.System.NAVBAR_MENU_MODE,MENU_DEFAULT);        	
        	for(int i = 0;i<mRotatedViews.length;i++){
				configureMenuKeys(mRotatedViews[i].findViewById(R.id.nav_buttons));        		
			}  
        	updateMenuImages();
			setMenuVisibility(mShowMenu, true /* force */);
        }
    }

    @Override
    public void onFinishInflate() {
    	
    	mRotatedViews[Surface.ROTATION_0] = 
    	mRotatedViews[Surface.ROTATION_180] = buildCustomKeys((FrameLayout) findViewById(R.id.rot0),Surface.ROTATION_0);

    	mRotatedViews[Surface.ROTATION_90] = buildCustomKeys((FrameLayout) findViewById(R.id.rot90),Surface.ROTATION_90);
    	        
    	mRotatedViews[Surface.ROTATION_270] = NAVBAR_ALWAYS_AT_RIGHT
    	                                                ? mRotatedViews[Surface.ROTATION_90]
    	                                                : buildCustomKeys((FrameLayout) findViewById(R.id.rot270),Surface.ROTATION_270);

    	mCurrentView = mRotatedViews[Surface.ROTATION_0];    	       
            	
    }

    private View buildCustomKeys(FrameLayout container,int rot) {
    	
    	final boolean landscape = (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270);
    	final boolean phablet = getResources().getBoolean(R.bool.config_isPhablet);
    	
    	final int totalWidth = landscape ? 
    			getResources().getDimensionPixelSize(R.dimen.navigation_key_width_land)*3 : 
    			getResources().getDimensionPixelSize(R.dimen.navigation_key_width)*3 ;
    			
    	final int padding = landscape ? getResources().getDimensionPixelSize(R.dimen.navigation_key_padding_land) 
    			: getResources().getDimensionPixelSize(R.dimen.navigation_key_padding);
    	
    	String[] temp = Settings.System.getString(mContext.getContentResolver(),
    			Settings.System.NAVBAR_KEY_ORDER,"back home recent_apps").split(" ");
    	String[] keys = new String[temp.length];
    	if(landscape && !phablet){
    		int j = 0;
    		for(int i = (temp.length-1);i>-1;i--){
    			keys[j] = temp[i];
    			j++;
    		}
    	}else
    		keys = temp;    		
    	
    	final int[] keyIds = getIds(keys);
		
    	LinearLayout oldNav = (LinearLayout) container.findViewById(R.id.nav_buttons);  
    	LinearLayout oldLights = (LinearLayout) container.findViewById(R.id.lights_out);  
    	
    	final LinearLayout navButtons = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.navigation_key_layout, null);   
    	final LinearLayout lightsOut = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.navigation_lights_out_layout, null);  
    	navButtons.setOrientation(landscape && !phablet ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);    	
    	lightsOut.setOrientation(landscape && !phablet ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);    
    	
    	CustomKeyButtonView menu = new CustomKeyButtonView(mContext);
    	menu.setId(landscape && !phablet ? R.id.menu : R.id.menu_left);    	
    	menu.setKeyCode(getKeyCode(menu.getId()));
    	menu.setLongPress();
    	menu.setGlowBG(landscape && !phablet);
    	LayoutParams params = new LinearLayout.LayoutParams(
    			landscape && !phablet ? LayoutParams.MATCH_PARENT : mMenuWidth, 
    			landscape && !phablet ? mMenuWidth : LayoutParams.MATCH_PARENT, 0f);
    	menu.setLayoutParams(params);
    	menu.setVisibility(View.INVISIBLE);    	
    	navButtons.addView(menu);
    	
    	int count = keyIds.length;
    	mKeyWidth = totalWidth/count;
    	for(int i = 0;i<count;i++){
    		CustomKeyButtonView v = new CustomKeyButtonView(mContext);
        	v.setId(keyIds[i]);        	
        	v.setKeyCode(getKeyCode(v.getId()));
        	v.setLongPress();
        	v.setGlowBG(landscape && !phablet);
        	params = new LinearLayout.LayoutParams(
        			landscape && !phablet ? LayoutParams.MATCH_PARENT : mKeyWidth, 
        			landscape && !phablet ? mKeyWidth : LayoutParams.MATCH_PARENT, 0f);
        	v.setLayoutParams(params);
        	v.setPadding(padding, 0, padding, 0);
        	((ImageView) v).setScaleType(ScaleType.CENTER);
        	navButtons.addView(v);
        	lightsOut.addView(createLightsOut(
        			(landscape && !phablet),(i == 0),(i == count-1)));
        	if(i < (count-1)){
        		navButtons.addView(createSpacer());
        		lightsOut.addView(createSpacer());
        	}
        	if(v.getId() == ID_MENU){
        		mHasMenuKey = true;
        		mMenuMode = MENU_DISABLED;
        	}        	
    	}    	
    	
    	menu = new CustomKeyButtonView(mContext);
    	menu.setId(landscape && !phablet ? R.id.menu_left : R.id.menu);    	
    	menu.setKeyCode(getKeyCode(menu.getId()));
    	menu.setLongPress();
    	menu.setGlowBG(landscape && !phablet);
    	params = new LinearLayout.LayoutParams(
    			landscape && !phablet ? LayoutParams.MATCH_PARENT : mMenuWidth, 
    			landscape && !phablet ? mMenuWidth : LayoutParams.MATCH_PARENT, 0f);
    	menu.setLayoutParams(params);
    	menu.setVisibility(View.INVISIBLE);    	
    	navButtons.addView(menu);
    	
    	configureMenuKeys(navButtons);    	
    	
    	container.addView(navButtons, container.indexOfChild(oldNav));
    	container.addView(lightsOut, container.indexOfChild(oldLights));
    	container.removeView(oldNav);  
    	container.removeView(oldLights); 
    	
    	return container;
	}    

	private View createLightsOut(boolean rotated, boolean first,boolean last) {
		
		ImageView iv = new ImageView(mContext);
		LayoutParams params = new LinearLayout.LayoutParams(
    			rotated ? LayoutParams.MATCH_PARENT : mKeyWidth, 
    			rotated ? mKeyWidth : LayoutParams.MATCH_PARENT, 0f);
		if(first && !rotated){
			params.leftMargin = mMenuWidth;
		}else if(last && !rotated){
			params.rightMargin = mMenuWidth;
		}else if(first && rotated){
			params.topMargin = mMenuWidth;			
		}else if(last && rotated){
			params.bottomMargin = mMenuWidth;
		}
		iv.setLayoutParams(params);
		iv.setScaleType(ScaleType.CENTER);
		iv.setImageResource(R.drawable.ic_sysbar_lights_out_dot_small);
		
		return iv;
	}

	private View createSpacer() {
    	
		View v = new View(mContext);
		v.setLayoutParams(new LinearLayout.LayoutParams(
    			LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f));
		v.setVisibility(View.INVISIBLE);
		return v;
	}	

	private int getKeyCode(int key) {
		
		switch(key){
			case R.id.menu:
			case R.id.menu_left:
			case R.id.menu_large:
				return KeyEvent.KEYCODE_MENU;
			case R.id.home:
				return KeyEvent.KEYCODE_HOME;
			case R.id.back:
				return KeyEvent.KEYCODE_BACK;
			case R.id.search:
				return KeyEvent.KEYCODE_SEARCH;
		}
		return 0;
	}

	private int[] getIds(String[] input) {
		
    	int[] ids = new int[input.length];
    	for(int i = 0;i<input.length;i++){    		
    		if(input[i].equals("back"))
    			ids[i] = R.id.back;
    		else if(input[i].equals("menu_large"))
    			ids[i] = R.id.menu_large;
    		else if(input[i].equals("home"))
    			ids[i] = R.id.home;
    		else if(input[i].equals("recent_apps"))
    			ids[i] = R.id.recent_apps;
    		else if(input[i].equals("search"))
    			ids[i] = R.id.search;   		
    	}
		return ids;
	}

	private void configureMenuKeys(View navButtons) {		
		CustomKeyButtonView left = ((CustomKeyButtonView) navButtons.findViewById(R.id.menu_left));
		CustomKeyButtonView right = ((CustomKeyButtonView) navButtons.findViewById(R.id.menu));
		left.enable();
		right.enable();
		mHideRight = mHideLeft = false;		
				
		if(mHasMenuKey){
			right.disable();			
			left.disable();
			mHideRight = mHideLeft = true;
			return;
		}		
		
		switch(mMenuMode){			
			case MENU_DEFAULT:
				left.disable();
				mHideLeft = true;				
				break;
			case MENU_DEFAULT_HIDDEN:
				left.disable();
				mHideLeft = mHideRight = true;
				break;
			case MENU_LEFT:
				right.disable();
				mHideRight = true;
				break;
			case MENU_LEFT_HIDDEN:
				right.disable();
				mHideRight = mHideLeft = true;
				break;
			case MENU_BOTH_HIDDEN:
				mHideRight = mHideLeft = true;
				break;			
		}	
		
	}	
	
	@Override 
	public void setMenuVisibility(final boolean show, final boolean force){
		setMenuVisibility(show,force,false);
	}			
	
	public void setMenuVisibility(final boolean show, final boolean force, final boolean arrows) {
        if (!force && mShowMenu == show) return;

        if(!arrows)
        	mShowMenu = show;

        CustomKeyButtonView right = (CustomKeyButtonView) getMenuButton();
        CustomKeyButtonView left = (CustomKeyButtonView) getLeftMenuButton();
        
        right.setVisibility((arrows || mShowMenu) && !mHasMenuKey && !right.isDisabled() ? View.VISIBLE : View.INVISIBLE);
        left.setVisibility((arrows || mShowMenu) && !mHasMenuKey && !left.isDisabled()? View.VISIBLE : View.INVISIBLE);
    }
	
	@Override
	public void setButtonImages (boolean withReflect){
		
		super.setButtonImages(withReflect);    	
    	final Resources res = mContext.getResources();    	
    	mMenuLeftIcon = mMenuRightIcon = res.getDrawable(withReflect ? R.drawable.ic_sysbar_menu_reflect : R.drawable.ic_sysbar_menu);
    	mMenuLeftLandIcon = mMenuRightIcon = res.getDrawable(withReflect ? R.drawable.ic_sysbar_menu_land_reflect : R.drawable.ic_sysbar_menu_land);    	
    	
    	if(((ImageView) getLargeMenuButton()) != null){
    		((ImageView) getLargeMenuButton()).setImageDrawable(res.getDrawable(mVertical 
        			? (withReflect ? R.drawable.ic_sysbar_menu_large_land_reflect : R.drawable.ic_sysbar_menu_large_land) 
        			: (withReflect ? R.drawable.ic_sysbar_menu_large_reflect : R.drawable.ic_sysbar_menu_large)));
    	}
    	((ImageView) getMenuButton()).setImageDrawable(mHideRight ? null :
    				mVertical ? mMenuRightLandIcon : mMenuRightIcon);
    	((ImageView) getLeftMenuButton()).setImageDrawable(mHideLeft ? null :
			mVertical ? mMenuLeftLandIcon : mMenuLeftIcon);
    	   	
    	
    }
	
	public void updateMenuImages(){
		((ImageView) getMenuButton()).setImageDrawable(mHideRight ? null :
			mVertical ? mMenuRightLandIcon : mMenuRightIcon);
		((ImageView) getLeftMenuButton()).setImageDrawable(mHideLeft ? null :
			mVertical ? mMenuLeftLandIcon : mMenuLeftIcon);
	}
	
	public void setNavigationIconHints(int hints, boolean force) {
		super.setNavigationIconHints(hints, force);        
		if(getLargeMenuButton() != null)
        	getLargeMenuButton().setAlpha(
                    (0 != (hints & StatusBarManager.NAVIGATION_HINT_RECENT_NOP)) ? 0.5f : 1.0f);
		
		if(Settings.System.getInt(mContext.getContentResolver(), Settings.System.SHOW_KEYBOARD_CURSOR, 1) ==1){
			final boolean IMEShowing = (0 != (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT));
			CustomKeyButtonView left = (CustomKeyButtonView) getLeftMenuButton();
			CustomKeyButtonView right = (CustomKeyButtonView) getMenuButton();
			left.setVisibility(View.INVISIBLE);
			right.setVisibility(View.INVISIBLE);
		
			left.setImageDrawable(IMEShowing
                ? (mVertical ? mMenuLeftAltLandIcon : mMenuLeftAltIcon)
                : (mVertical ? mMenuLeftLandIcon : mMenuLeftIcon));
			right.setImageDrawable(IMEShowing
                ? (mVertical ? mMenuRightAltLandIcon : mMenuRightAltIcon)
                : (mVertical ? mMenuRightLandIcon : mMenuRightIcon));   
        
			if(IMEShowing){        	
				left.setVisibility(View.VISIBLE);
				left.enable();        	
				left.setKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);        	
				right.setVisibility(View.VISIBLE);
				right.enable();
				right.setKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
				setMenuVisibility(mShowMenu, true /* force */,true);
			}else{        	
				left.setKeyCode(KeyEvent.KEYCODE_MENU);        	
				right.setKeyCode(KeyEvent.KEYCODE_MENU); 				
				for(int i = 0;i<mRotatedViews.length;i++){
					configureMenuKeys(mRotatedViews[i].findViewById(R.id.nav_buttons));        		
				}	
				updateMenuImages();
				setMenuVisibility(mShowMenu, true /* force */);
			}
		}
        
    }
	
	@Override    
    public void setButtonColor(){
    	super.setButtonColor();        
        if(getLargeMenuButton() != null)
            ((KeyButtonView) getLargeMenuButton()).setButtonColor();
        ((KeyButtonView) getLeftMenuButton()).setButtonColor();
    }
	
	@Override
	public void setDisabledFlags(int disabledFlags, boolean force) {
		super.setDisabledFlags(disabledFlags, force);       

        final boolean disableMenu = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        
		if(getLargeMenuButton() != null)
            getLargeMenuButton().setVisibility(disableMenu ? View.INVISIBLE : View.VISIBLE);		
		
    }
	
	@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(getSearchButton() != null && getLargeMenuButton() != null){
        	getDelegateHelper().setInitialTouchRegion(getHomeButton(), 
        			getBackButton(), getRecentsButton(), getSearchButton(),getLargeMenuButton());
        }else if(getSearchButton() != null && getLargeMenuButton() == null){
        	getDelegateHelper().setInitialTouchRegion(getHomeButton(), 
        			getBackButton(), getRecentsButton(), getSearchButton());
        }else if(getSearchButton() == null && getLargeMenuButton() != null){
        	getDelegateHelper().setInitialTouchRegion(getHomeButton(), 
        			getBackButton(), getRecentsButton(), getLargeMenuButton());
        }else{       	
            getDelegateHelper().setInitialTouchRegion(getHomeButton(), 
            		getBackButton(), getRecentsButton());            
        }
        
    }

}