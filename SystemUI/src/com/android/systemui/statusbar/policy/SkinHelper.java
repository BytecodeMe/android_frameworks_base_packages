package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Log;

public class SkinHelper {

	static final String TAG = "StatusBar.SkinHelper";

	public static Drawable getIconDrawable(Context c, int resId, String type) {

		if(resId == 0) return null;
		// Log.w(TAG, "Looking for Icon");
		Drawable d = null;
		Resources res = null;
		String image = getResourceName(c, resId);
		int resource = resId;

		String packageName = Settings.System.getString(c.getContentResolver(),
				type);

		Log.w(TAG, "looking for drawable: " + image + "From: " + packageName);

		if (packageName != null && !packageName.isEmpty()) {
			try {
				res = c.getPackageManager().getResourcesForApplication(
						packageName);
			} catch (Exception e) {
				Log.w(TAG, "Error Resolving custom signal package: "
						+ packageName);
				Settings.System.putString(c.getContentResolver(), type, "");
			}
		}
		if (res != null) {
			try {
				d = res.getDrawable(res.getIdentifier(image, "drawable",
						packageName));
			} catch (Exception e) {
				Log.w(TAG, "couldn't get drawable from " + packageName);
			}

		}
		if (d != null) {
			return d;
		} else {
			return c.getResources().getDrawable(resource);
		}
	}

	private static String getResourceName(Context c, int resId) {
		if (resId != 0) {
			final Resources res = c.getResources();
			try {
				String name = res.getResourceName(resId);
				return name.substring(name.indexOf("/")+1);
			} catch (Exception ex) {
				return "(unknown)";
			}
		} else {
			return "(null)";
		}
	}
}
