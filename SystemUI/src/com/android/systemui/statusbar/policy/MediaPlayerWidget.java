/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.systemui.R;

public class MediaPlayerWidget extends LinearLayout 
        implements View.OnClickListener {
    private static final String TAG = "StatusBar.MediaPlayerWidget";
    
    private TextView mTrackTitle;
    private TextView mArtistAlbum;
    private ImageButton mPrev;
    private ImageButton mPlayPause;
    private ImageButton mNext;

    public interface Listener {
        public void onClick(View v);
    }

    private Listener mListener;

    public MediaPlayerWidget(Context context) {
        this(context, null);
    }

    public MediaPlayerWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaPlayerWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View.inflate(context, R.layout.status_bar_media_player, this);

        mTrackTitle = (TextView)this.findViewById(R.id.trackname);
        mArtistAlbum = (TextView)this.findViewById(R.id.artistalbum);
        mPrev = (ImageButton)this.findViewById(R.id.prev);
        mPrev.setImageResource(R.drawable.btn_playback_rew_holo);
        mPlayPause = (ImageButton)this.findViewById(R.id.playpause);
        mPlayPause.setImageResource(R.drawable.btn_playback_pause_holo);
        mNext = (ImageButton)this.findViewById(R.id.next);
        mNext.setImageResource(R.drawable.btn_playback_ff_holo);
        
        mPrev.setOnClickListener(this);
        mPlayPause.setOnClickListener(this);
        mNext.setOnClickListener(this);
        
        mTrackTitle.setSelected(true);
        mArtistAlbum.setSelected(true);
    }

    public void setOnClickListener(Listener l) {
        mListener = l;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onClick(v);
        }        
    }
    
    public void setTrackTitle(int text){
        mTrackTitle.setText(text);
    }
    
    public void setTrackTitle(CharSequence text){
        mTrackTitle.setText(text);
    }
    
    public void setArtistAlbumText(CharSequence text){
        // unhide this as soon as it has text
        mArtistAlbum.setVisibility(View.VISIBLE);
        mArtistAlbum.setText(text);
    }
    
    public void setPlayImage(){
        mPlayPause.setImageResource(android.R.drawable.ic_media_play);
    }
    
    public void setPauseImage(){
        mPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }
}

