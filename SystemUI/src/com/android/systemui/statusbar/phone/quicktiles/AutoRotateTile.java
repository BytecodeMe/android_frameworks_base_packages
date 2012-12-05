package com.android.systemui.statusbar.phone.quicktiles;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.view.View;
import com.android.internal.view.RotationPolicy;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;


public class AutoRotateTile extends QuickSettingsTileContent
		implements View.OnClickListener, View.OnLongClickListener{

	public AutoRotateTile(Context context, View view) {
		super(context, view);
		init();
	}
	
	@Override
	public boolean onLongClick(View v) {
        launchActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
		return true;
	}


	@Override
	public void onClick(View v) {
		boolean locked = RotationPolicy.isRotationLocked(mContext);
		RotationPolicy.setRotationLock(mContext, !locked);
	    onRotationChanged();
	}
	
	private void onRotationChanged() {
		boolean locked = RotationPolicy.isRotationLocked(mContext);
		
		if(locked) {
			mTextView.setText(R.string.quick_settings_rotation_locked_label);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_notify_rotation_on_pressed, 0, 0);
		} else {
			mTextView.setText(R.string.quick_settings_rotation_unlocked_label);
			mTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_notify_rotation_on_normal, 0, 0);
		}
	}


	@Override
	protected void init() {
	    mContentView.setOnClickListener(this);
	    mContentView.setOnLongClickListener(this);
	    onRotationChanged();
	    
        RotationPolicy.registerRotationPolicyListener(mContext, mRotationPolicyListener,
                UserHandle.USER_ALL);
	}


	@Override
	public void release() {
		// TODO Auto-generated method stub
		RotationPolicy.unregisterRotationPolicyListener(mContext, mRotationPolicyListener);
	}


	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub
		
	}
	
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
        	onRotationChanged();
        }
    };

}
