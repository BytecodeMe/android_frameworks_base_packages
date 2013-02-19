package com.android.systemui.statusbar.phone.quicktiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;

import com.android.systemui.statusbar.phone.QuickSettingsTileContent;

public class CustomTile extends QuickSettingsTileContent implements
		View.OnClickListener, View.OnLongClickListener {

	private static final String TAG = CustomTile.class.getSimpleName();

	private String mCustomURI = null;
	private PackageManager pm;
	private Handler mHandler;
	private SettingsObserver mSettingsObserver;
	Drawable newIcon;
	int iconSize;

	public CustomTile(Context context, View view) {
		super(context, view);
		pm = context.getPackageManager();
		mHandler = new Handler();
		init();
	}
	
	@Override
	public void init() {
		this.mTag = TAG;
		mContentView.setOnClickListener(this);
		mContentView.setOnLongClickListener(this);
		refreshResources();
		onStart();
	}

	@Override
	public void onClick(View v) {
		try {
			launchActivity(Intent.parseUri(mCustomURI, 0));
		} catch (Throwable t) {
			t.printStackTrace();
			openSettings();
		}
	}

	@Override
	public boolean onLongClick(View v) {
		openSettings();
		return true;
	}

	private void openSettings() {
		mContext.startActivity(new Intent("com.bamf.settings.quicktiles")
				.addCategory(Intent.CATEGORY_DEFAULT)
				.putExtra("settings", true)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		getStatusBarManager().collapsePanels();
	}
	
	public void onStart() {
		if (mSettingsObserver == null) {
			mSettingsObserver = new SettingsObserver(mHandler);
		}
		mSettingsObserver.observe();
	}

	@Override
	public void release() {
		if (mSettingsObserver != null) {
			mSettingsObserver.stop();
			mSettingsObserver = null;
		}
	}

	@Override
	public void refreshResources() {
		Drawable customIcon = CustomIconHelper.loadFromFile(mContext, true);

		mCustomURI = Settings.System.getString(mContext.getContentResolver(),
				Settings.System.QUICK_SETTINGS_CUSTOM);
		CharSequence appname = "None selected";
		Drawable icon = mContext.getResources().getDrawable(
				android.R.drawable.sym_def_app_icon);

		if (mCustomURI != null) {
			try {
				appname = pm.resolveActivity(Intent.parseUri(mCustomURI, 0), 0).activityInfo
						.loadLabel(pm);
				icon = pm.getActivityIcon(Intent.parseUri(mCustomURI, 0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (customIcon != null) {
				icon = customIcon;
			}
		}
		
		// more accurate for other screen sizes
		iconSize = mContext.getResources().getDisplayMetrics().densityDpi / 5;

		newIcon = CustomIconHelper.resizeIcon(mContext,
				((BitmapDrawable) icon).getBitmap(), iconSize, iconSize);

		mTextView.setCompoundDrawablesWithIntrinsicBounds(null, newIcon, null,
				null);
		mTextView.setText(appname);

	}

	class SettingsObserver extends ContentObserver {
		SettingsObserver(Handler handler) {
			super(handler);
		}

		void observe() {
			ContentResolver resolver = mContext.getContentResolver();
			resolver.registerContentObserver(Settings.System
					.getUriFor(Settings.System.QUICK_SETTINGS_CUSTOM), false,
					this);
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
			refreshResources();
		}
	}
	
	public static class CustomIconHelper {
		public static final String SETTING_ICON = "custom_setting.png";

		private static synchronized File getTempFile(Context context) {
			File f = null;
			try {
				f = new File(context.getDir("bamf",
						Context.MODE_WORLD_WRITEABLE), SETTING_ICON);
				f.setReadable(true, false);

			} catch (Exception e) {
				e.printStackTrace();
			}
			return f;
		}

		public static Drawable loadFromFile(Context context, boolean resize) {
			File f = getTempFile(context);
			Drawable temp = null;
			if (f.exists()) {
				Bitmap b = readBitmap(context, Uri.fromFile(f));
				if (b != null) {
					try {
						if (resize) {
							temp = resizeIcon(context, b, 72.f, 72.f);
						} else {
							temp = new BitmapDrawable(context.getResources(), b);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			return temp;
		}

		private static Bitmap readBitmap(Context context, Uri selectedImage) {
			Bitmap bm = null;
			BitmapFactory.Options options = new BitmapFactory.Options();
			// options.inSampleSize = 2; //reduce quality
			AssetFileDescriptor fileDescriptor = null;
			try {
				fileDescriptor = context.getContentResolver()
						.openAssetFileDescriptor(selectedImage, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					bm = BitmapFactory.decodeFileDescriptor(
							fileDescriptor.getFileDescriptor(), null, options);
					fileDescriptor.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return bm;
		}

		private static Drawable resizeIcon(Context context, Bitmap bmp,
				float newWidth, float newHeight) {

			float scaleWidth = newWidth / ((float) bmp.getWidth());
			float scaleHeight = newHeight / ((float) bmp.getHeight());

			Matrix matrix = new Matrix();
			matrix.postScale(scaleWidth, scaleHeight);
			Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0,
					bmp.getWidth(), bmp.getHeight(), matrix, true);
			return new BitmapDrawable(context.getResources(), resizedBitmap);
		}
	}

}
