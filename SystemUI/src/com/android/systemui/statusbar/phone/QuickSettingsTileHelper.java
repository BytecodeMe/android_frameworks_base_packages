package com.android.systemui.statusbar.phone;

import java.util.ArrayList;
import java.util.List;

import com.android.systemui.R;

import android.content.Context;
import android.content.res.Configuration;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.util.QuickTileToken;
import android.text.util.QuickTileTokenizer;

public class QuickSettingsTileHelper {

	public static int getPosition(final Context context, Object token){
		List<QuickTileToken> settings = new ArrayList<QuickTileToken>();
		QuickTileTokenizer.tokenize(Settings.System.getString(context.getContentResolver(), 
				Settings.System.QUICK_SETTINGS_TILES,Settings.System.QUICK_TILES_DEFAULT), settings);
		for(QuickTileToken setting: settings){
			if(setting.getName().equals(((QuickTileToken)token).getName())){
				return settings.indexOf(setting);
			}
		}
		return -1;
	}
	
	public static int getMaxColumns(Context context) {
		int maxColumns = context.getResources().getInteger(R.integer.quick_settings_num_columns);
    	try{
    		if(context.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT){
        		maxColumns = Settings.System.getIntForUser(
        				context.getContentResolver(), 
        				Settings.System.QUICK_SETTINGS_NUM_COLUMNS_PORT, maxColumns, UserHandle.USER_CURRENT);
            }else{
        		maxColumns = Settings.System.getIntForUser(
        				context.getContentResolver(), 
        				Settings.System.QUICK_SETTINGS_NUM_COLUMNS_LAND, maxColumns, UserHandle.USER_CURRENT);
            }
    	}catch(Exception e){}
    	
    	return maxColumns;
		
	}

}
