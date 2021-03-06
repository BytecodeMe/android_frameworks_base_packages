package com.android.systemui.statusbar.phone.quicksettings;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarPreference;
import com.android.systemui.statusbar.policy.MediaPlayerWidget;

public class MediaPlayer extends StatusBarPreference 
	implements View.OnClickListener {

    private static final String TAG = MediaPlayer.class.getSimpleName();
    private static final boolean DEBUG = false;
    
    private static final int REFRESH = 1;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;
    
    private static final Uri MUSIC_CONTENT_URI = Uri.parse("content://com.google.android.music.MusicContent");
    
    private static final String EMPTY = "";

    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
    
    private CursorLoader mLoader;
    private CursorLoader mMusicLoader;
    private Cursor cAlbums;
    private Cursor cMusicAlbums;
    
    private String mArtist = EMPTY;
    private String mTrack = EMPTY;
    private String mAlbum = EMPTY;
    private boolean mPlaying = false;
    private long mSongId = -1;
    private long mAlbumId = -1;
    
    private PackageManager pm;
    String mMusicPackage = null;
    String mMusicService = null;
    
    public MediaPlayer(Context context, View view) {
        super(context, view);
        
        pm = mContext.getPackageManager();
        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
        
        init();
    }
    
    private MediaPlayerWidget.Listener mMediaButtonListener = new MediaPlayerWidget.Listener(){

		@Override
		public void onClick(View v) {
			try{
	            switch(v.getId()){
	                case R.id.prev:
	                    sendMediaButtonEvent("com.android.music.musicservicecommand.previous");
	                    if(DEBUG)Log.d(TAG, "prev");
	                    break;
	                case R.id.playpause:
	                    sendMediaButtonEvent("com.android.music.musicservicecommand.togglepause");
	                    setPauseButtonImage();
	                    break;
	                case R.id.next:
	                    sendMediaButtonEvent("com.android.music.musicservicecommand.next");
	                    if(DEBUG)Log.d(TAG, "next");
	                    break;
	                default:
	            }
	        }catch(NullPointerException e){
	            e.printStackTrace();
	        }
		}
    	
    };

    @Override
    public void onClick(View v) {
    	
    	if(findMusicService()){
    		launchActivity(new Intent("com.google.android.music.PLAYBACK_VIEWER")
	    		.setClassName(mMusicPackage, "com.android.music.activitymanagement.TopLevelActivity"));
	    	return;
    	}
    }
    
    private boolean findMusicService(){
        
        List<PackageInfo> list = pm.getInstalledPackages(PackageManager.GET_SERVICES);
        if(DEBUG)Log.d(TAG, "Found "+list.size()+" packages with services");

        for(PackageInfo pi: list){
            if(pi.packageName.equals("com.android.music") || pi.packageName.equals("com.google.android.music")){
                if(DEBUG)Log.d(TAG, "Found music under this package name: "+pi.packageName);
                if(pi.services.length > 0){
                    if(DEBUG)Log.d(TAG, "Music has services totaling: "+pi.services.length);
                    for(ServiceInfo service: pi.services){
                        if(DEBUG)Log.d(TAG, "Music service name: "+service.name);
                        if(service.name.contains("MusicPlaybackService")){
                            mMusicPackage = pi.packageName;
                            mMusicService = service.name;
                            if(DEBUG)Log.d(TAG, "Found the music service");
                            return true;
                        }
                    }
                }
            }
            
        }
        
        return false;
    }
    
    private void loadAlbumsData(){
        // load all of the albums into a cursor
        // we use this to compare to the meta data in order to
        // find the proper ablum id that we can use to 
        // grab the correct album art
        String[] projection = { MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ALBUM };
        String selection = "";
        String [] selectionArgs = null;
        mLoader = new CursorLoader(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null );
        mLoader.registerListener(0, mLoaderListener);
        mLoader.startLoading();
    }
    
    private void loadOnlineAlbumsData(){
        String[] projection = { MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ALBUM };
        String selection = "";
        String [] selectionArgs = null;
        
        mMusicLoader = new CursorLoader(mContext, Uri.withAppendedPath(MUSIC_CONTENT_URI, "audio"),
                projection, selection, selectionArgs, null );
        mMusicLoader.registerListener(1, mLoaderListener);
        mMusicLoader.startLoading();
    }
    
    private void sendMediaButtonEvent(String what){
        //TODO: allow the user to unlock this link to Google Music
        // so that this may work as a general media button event
        
        if(mMusicPackage == null || mMusicPackage == null){
            Log.w(TAG, "The Google music service was not found");
            Toast.makeText(mContext, "Google Music was not found", Toast.LENGTH_SHORT).show();
        }
        
        try{
            Intent intent = new Intent(what);
            intent.setClassName(mContext.createPackageContext(mMusicPackage, 0)
                    , mMusicService);
            mContext.startService(intent);
        }catch(NameNotFoundException e){
            e.printStackTrace();
        }catch(SecurityException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void init() {
    	mContentView.setOnClickListener(this);
        mContentView.getChildAt(1).setVisibility(View.GONE);
        mPlayer.setVisibility(View.VISIBLE);
        mPlayer.setOnClickListener(mMediaButtonListener);
        
        mTag = TAG;
        
        mPlayer.setTrackTitle(R.string.status_bar_settings_mediaplayer);
        mIcon.setImageResource(R.drawable.ic_sysbar_musicplayer);
        
        // dont even bother going any further
        if(!findMusicService())return;
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicUtils.PLAYSTATE_CHANGED);
        filter.addAction(MusicUtils.META_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
           
        loadAlbumsData();
        loadOnlineAlbumsData();
        updateTrackInfo();
        setPauseButtonImage();
    }

    @Override
    public void release() {
        
        mContext.unregisterReceiver(mReceiver);
        if(mLoader != null){
            mLoader.stopLoading();
            mLoader.unregisterListener(mLoaderListener);
        }
        if(mMusicLoader!=null){
            mMusicLoader.stopLoading();
            mMusicLoader.unregisterListener(mLoaderListener);
        }
        if(cAlbums!=null){
            cAlbums.close();
        }
        if(cMusicAlbums!=null){
            cMusicAlbums.close();
        }
    }

    @Override
    public void refreshResources() {
    	if(mLoader != null){
            mLoader.forceLoad();
        }
        if(mMusicLoader!=null){
            mMusicLoader.forceLoad();
        }
    }
    
    private void updateTrackInfo() {

        try {

            
            long songid = mSongId;
            if (songid < 0) return;
            
            String artistName = mArtist;
            if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
                artistName = "Unknown artist";
            }
            
            String trackName = mTrack;
            long albumid = mAlbumId;
            if (MediaStore.UNKNOWN_STRING.equals(trackName)) {
                artistName = "Unknown song";
                albumid = -1;
            }
            if(trackName.trim().equals(EMPTY)){
                trackName = mContext.getString(R.string.status_bar_settings_mediaplayer);
            }
            mPlayer.setArtistAlbumText(artistName);
            mPlayer.setTrackTitle(trackName);
            
            // lets search local music first
            if(cAlbums != null){
                int col_album = cAlbums.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int col_album_id = cAlbums.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                if(col_album >= 0 && col_album_id >= 0){
                    cAlbums.moveToFirst();
                    for(int x = 0; x < cAlbums.getCount(); x++){
                        if(DEBUG) Log.d(TAG, "row:"+x+" album:"+cAlbums.getString(col_album)
                                +" album id:"+cAlbums.getLong(col_album_id));
                        if(cAlbums.getString(col_album).equals(mAlbum)){
                            mAlbumId = cAlbums.getLong(col_album_id);
                            albumid = cAlbums.getLong(col_album_id);
                            break;
                        }
                        cAlbums.moveToNext();
                    }
                }
            }
            
            // now search for online music
            if(cMusicAlbums != null && albumid < 0){
                int col_album = cAlbums.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int col_album_id = cAlbums.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                if(col_album_id >= 0 && col_album >= 0){
                    cMusicAlbums.moveToFirst();
                    for(int x = 0; x < cMusicAlbums.getCount(); x++){
                        if(DEBUG) Log.d(TAG, "Music row:"+x+" album:"+cMusicAlbums.getString(col_album)
                                +" album id:"+cMusicAlbums.getLong(col_album_id));
                        if(cMusicAlbums.getString(col_album).equals(mAlbum)){
                            mSongId = cMusicAlbums.getLong(col_album_id);
                            songid = cMusicAlbums.getLong(col_album_id);
                            break;
                        }
                        cMusicAlbums.moveToNext();
                    }
                }
            }          
            
            /**
             * get the album art using a different thread
             */
            mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
            mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(albumid, songid)).sendToTarget();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void setPauseButtonImage() {
        if(mPlaying){
            mPlayer.setPauseImage();
        }else{
            mPlayer.setPlayImage();
        }
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            
            String action = intent.getAction();
            mArtist = intent.getStringExtra("artist");
            mAlbum = intent.getStringExtra("album");
            mTrack = intent.getStringExtra("track");
            mPlaying = intent.getBooleanExtra("playstate", mPlaying);
            mSongId = intent.getLongExtra("id", -1);
            mAlbumId = -1;
            
            updateTrackInfo();
            setPauseButtonImage();
            mPlayer.invalidate();
        }
        
    };
    
    private final Loader.OnLoadCompleteListener<Cursor> mLoaderListener = 
            new Loader.OnLoadCompleteListener<Cursor>() {

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
            if(loader.getId()==0){
                cAlbums = data;   
            }else{
                cMusicAlbums = data;
            }
        }
    };
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    mIcon.setScaleType(ScaleType.CENTER_INSIDE);
                    mIcon.setImageBitmap((Bitmap)msg.obj);
                    mIcon.getDrawable().setDither(true);
                    break;
                case REFRESH:
                    break;
                default:
                    break;
            }
        }
    };
    
    public class AlbumArtHandler extends Handler {
        private long mAlbumId = -1;
        
        public AlbumArtHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg)
        {
            long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
            long songid = ((AlbumSongIdWrapper) msg.obj).songid;
            if (msg.what == GET_ALBUM_ART && (mAlbumId != albumid || albumid < 0)) {
                // while decoding the new image, show the default album art
                Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
                mHandler.removeMessages(ALBUM_ART_DECODED);
                mHandler.sendMessageDelayed(numsg, 300);
                // Don't allow default artwork here, because we want to fall back to song-specific
                // album art if we can't find anything for the album.
                Bitmap bm = MusicUtils.getArtwork(mContext, songid, albumid, false);
                if (bm == null) {
                    bm = MusicUtils.getArtwork(mContext, songid, -1);
                    albumid = -1;
                }
                if (bm != null) {
                    numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, bm);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessage(numsg);
                }else{
                    Log.d(TAG, "album art returned null for "+songid);
                }
                mAlbumId = albumid;
            }
        }
    }
    
    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;
        
        /**
         * Creates a worker thread with the given name. The thread
         * then runs a {@link android.os.Looper}.
         * @param name A name for the new thread
         */
        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        public Looper getLooper() {
            return mLooper;
        }
        
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }
        
        public void quit() {
            mLooper.quit();
        }
    }
    
    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;
        AlbumSongIdWrapper(long aid, long sid) {
            albumid = aid;
            songid = sid;
        }
    }
    
    private static class MusicUtils {
        private static int sArtId = -2;
        private static Bitmap mCachedBit = null;
        private static final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
        private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
        private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        private static final Uri MUSIC_CONTENT_URI = Uri.parse("content://com.google.android.music.MusicContent");
        
        public static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
        public static final String META_CHANGED = "com.android.music.metachanged";
        
        static {
            // for the cache, 
            // 565 is faster to decode and display
            // and we don't want to dither here because the image will be scaled down later
            sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
            sBitmapOptionsCache.inDither = false;

            sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            sBitmapOptions.inDither = false;
        }
                
        // get album art for specified file
        private static final String sExternalMediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
            Bitmap bm = null;

            if (albumid < 0 && songid < 0) {
                throw new IllegalArgumentException("Must specify an album or a song id");
            }

            try {
                if (albumid < 0) {
                    // for online music
                    Log.d(TAG, "looking here for album art:"+MUSIC_CONTENT_URI+"/albumart/"+songid);
                    Uri uri = Uri.withAppendedPath(MUSIC_CONTENT_URI, "albumart/"+songid);
                    ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        bm = BitmapFactory.decodeFileDescriptor(fd);
                    }
                } else {
                    // local music
                    Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
                    Log.d(TAG, "looking here for album art:"+uri.toSafeString());
                    ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        bm = BitmapFactory.decodeFileDescriptor(fd);
                    }
                }
            } catch (IllegalStateException ex) {
            } catch (FileNotFoundException ex) {
            }
            if (bm != null) {
                mCachedBit = bm;
            }
            return bm;
        }
        
        public static Bitmap getArtwork(Context context, long song_id, long album_id) {
            return getArtwork(context, song_id, album_id, true);
        }
        
        /** Get album art for specified album. You should not pass in the album id
         * for the "unknown" album here (use -1 instead)
         */
        public static Bitmap getArtwork(Context context, long song_id, long album_id,
                boolean allowdefault) {

            if (album_id < 0) {
                // This is something that is not in the database, so get the album art directly
                // from the file.
                if (song_id >= 0) {
                    Bitmap bm = getArtworkFromFile(context, song_id, -1);
                    if (bm != null) {
                        return bm;
                    }
                }
                if (allowdefault) {
                    return getDefaultArtwork(context);
                }
                return null;
            }

            ContentResolver res = context.getContentResolver();
            Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
            if (uri != null) {
                InputStream in = null;
                try {
                    in = res.openInputStream(uri);
                    return BitmapFactory.decodeStream(in, null, sBitmapOptions);
                } catch (FileNotFoundException ex) {
                    // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                    // maybe it never existed to begin with.
                    Bitmap bm = getArtworkFromFile(context, song_id, album_id);
                    if (bm != null) {
                        if (bm.getConfig() == null) {
                            bm = bm.copy(Bitmap.Config.RGB_565, false);
                            if (bm == null && allowdefault) {
                                return getDefaultArtwork(context);
                            }
                        }
                    } else if (allowdefault) {
                        bm = getDefaultArtwork(context);
                    }
                    return bm;
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                    }
                }
            }
            
            return null;
        }
        
        private static Bitmap getArtworkFromUrl(Context context, String address){
            Bitmap bm = null;            
            try{
                URL url = new URL(address);
                InputStream content = (InputStream)url.getContent();
                Drawable d = Drawable.createFromStream(content , "src"); 
                bm = BitmapFactory.decodeStream(content);
            }catch(Exception e){
                e.printStackTrace();
            }
            
            return bm;
        }
        
        private static Bitmap getDefaultArtwork(Context context) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeStream(
                    context.getResources().openRawResource(R.drawable.albumart_mp_unknown), null, opts);
        }
    }
}
