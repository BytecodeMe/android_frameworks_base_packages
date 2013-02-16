package com.android.systemui.statusbar.phone.quicktiles;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class UserTile extends QuickSettingsTileContent implements
		View.OnClickListener {

	private static final String TAG = "QuickSettings";
	private UserState mUserState = new UserState();
	private AsyncTask<Void, Void, Pair<String, Drawable>> mUserInfoTask;
	boolean mTilesSetUp = false;

	public UserTile(Context context, View view) {
		super(context, view);
		init();
	}

	@Override
	public void release() {
		mContext.unregisterReceiver(mProfileReceiver);
		mContext.unregisterReceiver(mReceiver);
	}

	@Override
	public void refreshResources() {
		// TODO Auto-generated method stub

	}

	void setUserTileInfo(String name, Drawable avatar) {
		mUserState.label = name;
		mUserState.avatar = avatar;
		updateGUI();
	}

	@Override
	public void onClick(View v) {
		getStatusBarManager().collapsePanels();

		final UserManager um = (UserManager) mContext
				.getSystemService(Context.USER_SERVICE);
		if (um.getUsers(true).size() > 1) {
			Bundle opts = null;
			opts = new Bundle();
            opts.putInt(LockPatternUtils.KEYGUARD_SHOW_USER_SWITCHER, UserHandle.USER_CURRENT);
			try {
				WindowManagerGlobal.getWindowManagerService().lockNow(opts);
			} catch (RemoteException e) {
				Log.e(TAG, "Couldn't show user switcher", e);
			}
		} else {
			Intent intent = ContactsContract.QuickContact
					.composeQuickContactsIntent(mContext, v,
							ContactsContract.Profile.CONTENT_URI,
							ContactsContract.QuickContact.MODE_LARGE, null);
			mContext.startActivityAsUser(intent, new UserHandle(
					UserHandle.USER_CURRENT));
		}
	}

	@Override
	protected void init() {
		mContentView.setOnClickListener(this);
		mImageView.setVisibility(View.VISIBLE);
		mTextView.setBackgroundColor(mContext.getResources().getColor(R.color.qs_user_banner_background));
		adjustLayouts();
		queryForUserInformation();
		mTilesSetUp = true;

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_USER_SWITCHED);
		mContext.registerReceiver(mReceiver, filter);

		IntentFilter profileFilter = new IntentFilter();
		profileFilter
				.addAction(ContactsContract.Intents.ACTION_PROFILE_CHANGED);
		profileFilter.addAction(Intent.ACTION_USER_INFO_CHANGED);
		mContext.registerReceiverAsUser(mProfileReceiver, UserHandle.ALL,
				profileFilter, null, null);
	}

	private void updateGUI() {
		mTextView.setText(mUserState.label);
		mImageView.setImageDrawable(mUserState.avatar);
		mContentView.setContentDescription(mContext.getString(
				R.string.accessibility_quick_settings_user, mUserState.label));
	}

	void adjustLayouts() {
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) ((LinearLayout) mTextView
				.getParent()).getLayoutParams();
		lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
		lp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
		lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;

		((LinearLayout) mTextView.getParent()).setLayoutParams(lp);

		LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) mTextView
				.getLayoutParams();
		lp2.width = LinearLayout.LayoutParams.MATCH_PARENT;
		lp2.height = LinearLayout.LayoutParams.WRAP_CONTENT;
		lp2.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;

		mTextView.setLayoutParams(lp2);
	}

	private void queryForUserInformation() {
		Context currentUserContext = null;
		UserInfo userInfo = null;
		try {
			userInfo = ActivityManagerNative.getDefault().getCurrentUser();
			currentUserContext = mContext.createPackageContextAsUser("android",
					0, new UserHandle(userInfo.id));
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Couldn't create user context", e);
			throw new RuntimeException(e);
		} catch (RemoteException e) {
			Log.e(TAG, "Couldn't get user info", e);
		}
		final int userId = userInfo.id;
		final String userName = userInfo.name;

		final Context context = currentUserContext;
		mUserInfoTask = new AsyncTask<Void, Void, Pair<String, Drawable>>() {
			@Override
			protected Pair<String, Drawable> doInBackground(Void... params) {
				final UserManager um = (UserManager) mContext
						.getSystemService(Context.USER_SERVICE);

				// Fall back to the UserManager nickname if we can't read the
				// name from the local
				// profile below.
				String name = userName;
				Drawable avatar = null;
				Bitmap rawAvatar = um.getUserIcon(userId);
				if (rawAvatar != null) {
					avatar = new BitmapDrawable(mContext.getResources(),
							rawAvatar);
				} else {
					avatar = mContext.getResources().getDrawable(
							R.drawable.ic_qs_default_user);
				}

				// If it's a single-user device, get the profile name, since the
				// nickname is not
				// usually valid
				if (um.getUsers().size() <= 1) {
					// Try and read the display name from the local profile
					final Cursor cursor = context.getContentResolver().query(
							Profile.CONTENT_URI,
							new String[] { Phone._ID, Phone.DISPLAY_NAME },
							null, null, null);
					if (cursor != null) {
						try {
							if (cursor.moveToFirst()) {
								name = cursor.getString(cursor
										.getColumnIndex(Phone.DISPLAY_NAME));
							}
						} finally {
							cursor.close();
						}
					}
				}
				return new Pair<String, Drawable>(name, avatar);
			}

			@Override
			protected void onPostExecute(Pair<String, Drawable> result) {
				super.onPostExecute(result);
				mUserInfoTask = null;
				setUserTileInfo(result.first, result.second);			}
		};
		mUserInfoTask.execute();
	}

	void reloadUserInfo() {
		if (mUserInfoTask != null) {
			mUserInfoTask.cancel(false);
			mUserInfoTask = null;
		}
		if (mTilesSetUp) {
			queryForUserInformation();
		}
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_USER_SWITCHED.equals(action)) {
				reloadUserInfo();
				if(mCallBack!=null){
					mCallBack.refreshTiles();
				}
			}
		}
	};

	private final BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (ContactsContract.Intents.ACTION_PROFILE_CHANGED.equals(action)
					|| Intent.ACTION_USER_INFO_CHANGED.equals(action)) {
				try {
					final int userId = ActivityManagerNative.getDefault()
							.getCurrentUser().id;
					if (getSendingUserId() == userId) {
						reloadUserInfo();
					}
				} catch (RemoteException e) {
					Log.e(TAG,
							"Couldn't get current user id for profile change",
							e);
				}
			}

		}
	};

	static class UserState extends State {
		Drawable avatar;
	}

}
