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

import java.io.FileDescriptor;
import java.io.PrintWriter;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.animation.AccelerateInterpolator;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;
import com.android.systemui.recent.RecentsPanelView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DelegateViewHelper;
import com.android.systemui.statusbar.policy.CustomKeyButtonView;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.SkinHelper;

public class NavigationBarView extends LinearLayout {
    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/NavigationBarView";

    final static boolean NAVBAR_ALWAYS_AT_RIGHT = true;

    // slippery nav bar when everything is disabled, e.g. during setup
    final static boolean SLIPPERY_WHEN_DISABLED= true;

    final static boolean ANIMATE_HIDE_TRANSITION = false; // turned off because it introduces unsightly delay when videos goes to full screen

    protected IStatusBarService mBarService;
    final Display mDisplay;
    View mCurrentView = null;
    View[] mRotatedViews = new View[4];

    int mBarSize;
    boolean mVertical;
    boolean mScreenOn;

    boolean mHidden, mLowProfile, mShowMenu;
    int mDisabledFlags = 0;
    int mNavigationIconHints = 0;
    private String mOldBackFunction = null;

    private Drawable mBackIcon, mBackLandIcon, mBackAltIcon, mBackAltLandIcon;
    
    private DelegateViewHelper mDelegateHelper;
    private DeadZone mDeadZone;
	public boolean mHasReflections = false;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = mCurrentView.getWidth();
                    final int vh = mCurrentView.getHeight();

                    if (h != vh || w != vw) {
                        Slog.w(TAG, String.format(
                            "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                            how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    public void setDelegateView(View view) {
        mDelegateHelper.setDelegateView(view);
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        mDelegateHelper.setBar(phoneStatusBar);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDeadZone != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mDeadZone.poke(event);
        }
        if (mDelegateHelper != null) {
            boolean ret = mDelegateHelper.onInterceptTouchEvent(event);
            if (ret) return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mDelegateHelper.onInterceptTouchEvent(event);
    }

    private H mHandler = new H();

    public ImageView getRecentsButton() {
        return (ImageView)mCurrentView.findViewById(R.id.recent_apps);
    }

    public ImageView getRightMenuButton() {
        return (ImageView)mCurrentView.findViewById(R.id.menu);
    }

    public ImageView getBackButton() {
        return (ImageView)mCurrentView.findViewById(R.id.back);
    }

    public ImageView getHomeButton() {
        return (ImageView)mCurrentView.findViewById(R.id.home);
    }
	
	public ImageView getSearchButton() {
        return (ImageView)mCurrentView.findViewById(R.id.search);
    }

    // for when home is disabled, but search isn't
    public ImageView getSearchLight() {
        return (ImageView)mCurrentView.findViewById(R.id.search_light);
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHidden = false;

        mDisplay = ((WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));

        final Resources res = mContext.getResources();
        mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        mDelegateHelper = new DelegateViewHelper(this);

        mBackIcon = SkinHelper.getIconDrawable(context, R.drawable.ic_sysbar_back,Settings.System.CUSTOM_NAVBAR_PACKAGE);
        mBackLandIcon = SkinHelper.getIconDrawable(context, R.drawable.ic_sysbar_back_land,Settings.System.CUSTOM_NAVBAR_PACKAGE);
        mBackAltIcon = SkinHelper.getIconDrawable(context, R.drawable.ic_sysbar_back_ime,Settings.System.CUSTOM_NAVBAR_PACKAGE);
        mBackAltLandIcon = SkinHelper.getIconDrawable(context, R.drawable.ic_sysbar_back_ime,Settings.System.CUSTOM_NAVBAR_PACKAGE);
    }

    public void notifyScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
        setDisabledFlags(mDisabledFlags, true);
    }

    View.OnTouchListener mLightsOutListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                // even though setting the systemUI visibility below will turn these views
                // on, we need them to come up faster so that they can catch this motion
                // event
                setLowProfile(false, false, false);

                try {
                    mBarService.setSystemUiVisibility(0, View.SYSTEM_UI_FLAG_LOW_PROFILE);
                } catch (android.os.RemoteException ex) {
                }
            }
            return false;
        }
    };

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;

        if (DEBUG) {
            android.widget.Toast.makeText(mContext,
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        // since the user can remove these, check to see if they are null first
        if(getBackButton() != null){
        	final boolean IME = (0 != (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT));
        	//getBackButton().setAlpha(IME ? 0.5f : 1.0f);
        	getBackButton().setImageDrawable(IME
                        ? (mVertical ? mBackAltLandIcon : mBackAltIcon)
                        : (mVertical ? mBackLandIcon : mBackIcon));
        	if(IME){
        		if(mOldBackFunction ==null)
        			mOldBackFunction = Settings.System.getString(
        				mContext.getContentResolver(), Settings.System.LONG_ACTION_BACK,CustomKeyButtonView.ACTION_DEFAULT_NONE);        		
        		Settings.System.putString(mContext.getContentResolver(), Settings.System.LONG_ACTION_BACK, CustomKeyButtonView.ACTION_PICKER);
        		((CustomKeyButtonView) getBackButton()).setLongPress();
        	}else if(mOldBackFunction != null){        		
        		Settings.System.putString(mContext.getContentResolver(), Settings.System.LONG_ACTION_BACK, mOldBackFunction);
        		((CustomKeyButtonView) getBackButton()).setLongPress();
        		mOldBackFunction = null;
        	}
        }
        if(getHomeButton() != null){
        	getHomeButton().setAlpha(
        			(0 != (hints & StatusBarManager.NAVIGATION_HINT_HOME_NOP)) ? 0.5f : 1.0f);
        }
        if(getRecentsButton() != null){
        	getRecentsButton().setAlpha(
        			(0 != (hints & StatusBarManager.NAVIGATION_HINT_RECENT_NOP)) ? 0.5f : 1.0f);
        }
        if(getSearchButton() != null){
        	getSearchButton().setAlpha(
                    (0 != (hints & StatusBarManager.NAVIGATION_HINT_RECENT_NOP)) ? 0.5f : 1.0f);
        }
    }
    public void setButtonImages (boolean withReflect){
    	
    	mHasReflections = withReflect;    	
    	mBackIcon = SkinHelper.getIconDrawable(mContext,withReflect ? R.drawable.ic_sysbar_back_reflect : R.drawable.ic_sysbar_back,Settings.System.CUSTOM_NAVBAR_PACKAGE);
    	mBackLandIcon = SkinHelper.getIconDrawable(mContext,withReflect ? R.drawable.ic_sysbar_back_land_reflect : R.drawable.ic_sysbar_back_land,Settings.System.CUSTOM_NAVBAR_PACKAGE);
    	
    	if(getRightMenuButton() != null){
	    	getRightMenuButton().setImageDrawable(SkinHelper.getIconDrawable(mContext,(mVertical 
	    			? (withReflect ? R.drawable.ic_sysbar_menu_land_reflect : R.drawable.ic_sysbar_menu_land) 
	    			: (withReflect ? R.drawable.ic_sysbar_menu_reflect : R.drawable.ic_sysbar_menu)),Settings.System.CUSTOM_NAVBAR_PACKAGE));
    	}
    	if(getBackButton() != null){
	    	getBackButton().setImageDrawable(mVertical ? mBackLandIcon : mBackIcon);
    	}
    	if(getHomeButton() != null){
	    	getHomeButton().setImageDrawable(SkinHelper.getIconDrawable(mContext,(mVertical 
	    			? (withReflect ? R.drawable.ic_sysbar_home_land_reflect : R.drawable.ic_sysbar_home_land) 
	    			: (withReflect ? R.drawable.ic_sysbar_home_reflect : R.drawable.ic_sysbar_home)),Settings.System.CUSTOM_NAVBAR_PACKAGE));
    	}
    	if(getRecentsButton() != null){
	    	getRecentsButton().setImageDrawable(SkinHelper.getIconDrawable(mContext,(mVertical 
	    			? (withReflect ? R.drawable.ic_sysbar_recent_land_reflect : R.drawable.ic_sysbar_recent_land) 
	    			: (withReflect ? R.drawable.ic_sysbar_recent_reflect : R.drawable.ic_sysbar_recent)),Settings.System.CUSTOM_NAVBAR_PACKAGE));
    	}
    	if(getSearchButton() != null){
    		getSearchButton().setImageDrawable(SkinHelper.getIconDrawable(mContext,(mVertical 
        			? (withReflect ? R.drawable.ic_sysbar_search_land_reflect : R.drawable.ic_sysbar_search_land) 
        	    	: (withReflect ? R.drawable.ic_sysbar_search_reflect : R.drawable.ic_sysbar_search)),Settings.System.CUSTOM_NAVBAR_PACKAGE));
    	}
    	// this needs to be here to fix the buttons appearing white after the first
    	// orientation change
    	setButtonColor();
    }
	    /**
     * change the color overlay for the buttons
     * @return
     */
    public void setButtonColor(){
    	setButtonColor(getBackButton());
    	setButtonColor(getHomeButton());
    	setButtonColor(getRecentsButton());
    	setButtonColor(getSearchButton());
    	setButtonColor(getRightMenuButton());
    }
    
    protected void setButtonColor(View view){
    	if(view!=null){
    		((KeyButtonView) view).setButtonColor();
    	}
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        final boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0);
                //&& ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);
        final boolean disableSearch = ((disabledFlags & View.STATUS_BAR_DISABLE_SEARCH) != 0);

        if (SLIPPERY_WHEN_DISABLED) {
            setSlippery(disableHome && disableRecent && disableBack && disableSearch);
        }

        if (!mScreenOn && mCurrentView != null) {
            ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
            LayoutTransition lt = navButtons == null ? null : navButtons.getLayoutTransition();
            if (lt != null) {
                lt.disableTransitionType(
                        LayoutTransition.CHANGE_APPEARING | LayoutTransition.CHANGE_DISAPPEARING |
                        LayoutTransition.APPEARING | LayoutTransition.DISAPPEARING);
            }
        }

        setButtonVisibility(getBackButton(), disableBack);
		setButtonVisibility(getHomeButton(), disableHome);
		setButtonVisibility(getRecentsButton(), disableRecent);
		setButtonVisibility(getSearchButton(), disableRecent);
		
		View blackout = mCurrentView.findViewById(R.id.blackout);		
		blackout.setVisibility(disableHome ? View.VISIBLE : View.GONE);


        getSearchLight().setVisibility((disableHome && !disableSearch) ? View.VISIBLE : View.GONE);
    }
	
	protected void setButtonVisibility(View view, boolean disable){
    	if(view != null){
    		view.setVisibility(disable ? View.INVISIBLE:View.VISIBLE);
    	}
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & WindowManager.LayoutParams.FLAG_SLIPPERY) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) return;

        mShowMenu = show;

        if(getRightMenuButton()==null) return;
        getRightMenuButton().setVisibility(mShowMenu ? View.VISIBLE : View.INVISIBLE);
    }

    public void setLowProfile(final boolean lightsOut) {
        setLowProfile(lightsOut, true, false);
    }

    public void setLowProfile(final boolean lightsOut, final boolean animate, final boolean force) {
        if (!force && lightsOut == mLowProfile) return;

        mLowProfile = lightsOut;

        if (DEBUG) Slog.d(TAG, "setting lights " + (lightsOut?"out":"on"));

        final View navButtons = mCurrentView.findViewById(R.id.nav_buttons);
        final View lowLights = mCurrentView.findViewById(R.id.lights_out);

        // ok, everyone, stop it right there
        navButtons.animate().cancel();
        lowLights.animate().cancel();

        if (!animate) {
            navButtons.setAlpha(lightsOut ? 0f : 1f);

            lowLights.setAlpha(lightsOut ? 1f : 0f);
            lowLights.setVisibility(lightsOut ? View.VISIBLE : View.GONE);
        } else {
            navButtons.animate()
                .alpha(lightsOut ? 0f : 1f)
                .setDuration(lightsOut ? 750 : 250)
                .start();

            lowLights.setOnTouchListener(mLightsOutListener);
            if (lowLights.getVisibility() == View.GONE) {
                lowLights.setAlpha(0f);
                lowLights.setVisibility(View.VISIBLE);
            }
            lowLights.animate()
                .alpha(lightsOut ? 1f : 0f)
                .setDuration(lightsOut ? 750 : 250)
                .setInterpolator(new AccelerateInterpolator(2.0f))
                .setListener(lightsOut ? null : new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator _a) {
                        lowLights.setVisibility(View.GONE);
                    }
                })
                .start();
        }
    }

    public void setHidden(final boolean hide) {
        if (hide == mHidden) return;

        mHidden = hide;
        Slog.d(TAG,
            (hide ? "HIDING" : "SHOWING") + " navigation bar");

        // bring up the lights no matter what
        setLowProfile(false);
    }

    @Override
    public void onFinishInflate() {
        mRotatedViews[Surface.ROTATION_0] = 
        mRotatedViews[Surface.ROTATION_180] = findViewById(R.id.rot0);

        mRotatedViews[Surface.ROTATION_90] = findViewById(R.id.rot90);
        
        mRotatedViews[Surface.ROTATION_270] = NAVBAR_ALWAYS_AT_RIGHT
                                                ? findViewById(R.id.rot90)
                                                : findViewById(R.id.rot270);

        mCurrentView = mRotatedViews[Surface.ROTATION_0];
    }

    public void reorient() {
        final int rot = mDisplay.getRotation();
        for (int i=0; i<4; i++) {
            mRotatedViews[i].setVisibility(View.GONE);
        }
        mCurrentView = mRotatedViews[rot];
        mCurrentView.setVisibility(View.VISIBLE);

        mDeadZone = (DeadZone) mCurrentView.findViewById(R.id.deadzone);
		setButtonImages(mHasReflections);
        setNavigationIconHints(mNavigationIconHints, true);

        // force the low profile & disabled states into compliance
        setLowProfile(mLowProfile, false, true /* force */);
        setDisabledFlags(mDisabledFlags, true /* force */);
        setMenuVisibility(mShowMenu, true /* force */);

        if (DEBUG) {
            Slog.d(TAG, "reorient(): rot=" + mDisplay.getRotation());
        }

    }

//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        super.onLayout(changed, l, t, r, b);
//        mDelegateHelper.setInitialTouchRegion(getHomeButton(), getBackButton(), getRecentsButton());
//    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG) Slog.d(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));

        final boolean newVertical = w > 0 && h > w;
        if (newVertical != mVertical) {
            mVertical = newVertical;
            //Slog.v(TAG, String.format("onSizeChanged: h=%d, w=%d, vert=%s", h, w, mVertical?"y":"n"));
            reorient();
        }

        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /*
    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) Slog.d(TAG, String.format(
                    "onLayout: %s (%d,%d,%d,%d)", 
                    changed?"changed":"notchanged", left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);
    }

    // uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT situations: if all else
    // fails, any touch on the display will fix the layout.
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Slog.d(TAG, "onInterceptTouchEvent: " + ev.toString());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            postCheckForInvalidLayout("touch");
        }
        return super.onInterceptTouchEvent(ev);
    }
    */
        

    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        final Rect r = new Rect();
        final Point size = new Point();
        mDisplay.getRealSize(size);

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                        + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: " 
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                        getResourceName(mCurrentView.getId()),
                        mCurrentView.getWidth(), mCurrentView.getHeight(),
                        visibilityToString(mCurrentView.getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s hidden=%s low=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mHidden ? "true" : "false",
                        mLowProfile ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        final View back = getBackButton();
        final View home = getHomeButton();
        final View recent = getRecentsButton();
        final View menu = getRightMenuButton();

        try{
        pw.println("      back: "
                + PhoneStatusBar.viewInfo(back)
                + " " + visibilityToString(back.getVisibility())
                );
        pw.println("      home: "
                + PhoneStatusBar.viewInfo(home)
                + " " + visibilityToString(home.getVisibility())
                );
        pw.println("      rcnt: "
                + PhoneStatusBar.viewInfo(recent)
                + " " + visibilityToString(recent.getVisibility())
                );
        pw.println("      menu: "
                + PhoneStatusBar.viewInfo(menu)
                + " " + visibilityToString(menu.getVisibility())
                );
        pw.println("    }");
        }catch(NullPointerException e){}
    }

	public DelegateViewHelper getDelegateHelper() {
		return mDelegateHelper;
	}

//	public void setRecentButtonOnTouchListener(RecentsPanelView mRecentsPanel) {
//		if(getRecentsButton()!=null){
//			getRecentsButton().setOnTouchListener(mRecentsPreloadOnTouchListener);
//		}
//	}

	public void setRecentButtonOnClickListener(OnClickListener mRecentsClickListener) {
		if(getRecentsButton()!=null){
			getRecentsButton().setOnClickListener(mRecentsClickListener);
		}
	}
}
