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

import java.util.Random;

import com.android.systemui.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 *
 */
class QuickSettingsTileView extends FrameLayout {

    private int mColSpan;
    private int mRowSpan;
    private int mCellWidth;
    
    private static int[] Colors = new int[]{
    	android.R.color.holo_blue_dark,
    	android.R.color.holo_green_dark,
    	android.R.color.holo_orange_dark,
    	android.R.color.holo_purple,
    	android.R.color.holo_red_dark
    };

    public QuickSettingsTileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColSpan = 1;
        mRowSpan = 1;
        
    }
    
    void setEgg(boolean enabled){
    	if(enabled){
	        Random generator = new Random();
	        int color = getContext().getResources().getColor(Colors[generator.nextInt(Colors.length)]);
	        this.setBackgroundColor(color);
        }else{
        	this.setBackgroundResource(R.drawable.qs_tile_background);
        }
    }

	void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }
    
    void setRowSpan(int span) {
    	mRowSpan = span;
    }
    
    int getRowSpan() {
    	return mRowSpan;
    }

    void setContent(int layoutId, LayoutInflater inflater) {
        inflater.inflate(layoutId, this);
    }
}