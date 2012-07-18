package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class BatteryText extends TextView {

	public BatteryText(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public BatteryText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}

	public BatteryText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupText();
	}

	private void setupText() {
		// TODO Auto-generated method stub
		float textSize = Settings.System.getFloat(
				mContext.getContentResolver(), Settings.System.BATTERY_TEXT_SIZE, 7f);
		int textColor = Settings.System.getInt(
				mContext.getContentResolver(),Settings.System.BATTERY_TEXT_COLOR,Color.WHITE);
        
		setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
		setTextColor(textColor);
	}

}
