package com.gtek.dragoneye;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.util.Log;

import androidx.annotation.RequiresApi;

import org.videolan.libvlc.MediaPlayer;
import java.lang.ref.WeakReference;

public class VideoActivity  extends Activity implements IVLCVout.Callback    {
    public final static String TAG = "VideoActivity";

    public static final String RTSP_URL = "rtspurl";

    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    class MyPlayerListener implements MediaPlayer.EventListener {
        //private String TAG = "PlayerListener";
        private WeakReference<VideoActivity> mOwner;

        public MyPlayerListener(VideoActivity owner) {
            mOwner = new WeakReference<VideoActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            VideoActivity player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    //Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    private String rtspUrl;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // Get URL
        Intent intent = getIntent();
        rtspUrl = intent.getExtras().getString(RTSP_URL);
        Log.d(TAG, "Playing back " + rtspUrl);

        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        //holder.addCallback(this);

        ArrayList<String> options = new ArrayList<String>();
        //options.add("--aout=opensles");
        //options.add("--audio-time-stretch"); // time stretching
        options.add("-vvv"); // verbosity
        //options.add("--aout=opensles");
        //options.add("--avcodec-codec=hevc");
        //options.add("--file-logging");
        //options.add("--logfile=vlc-log.txt");
        options.add("--log-verbose");
        //options.add("--rtsp-tcp");
        options.add("--rtsp-timeout=600"); //  Timeout in 10 minutes.

        libvlc = new LibVLC(getApplicationContext(), options);
        holder.setKeepScreenOn(true);

        // Create media player
        mMediaPlayer = new MediaPlayer(libvlc);
        mMediaPlayer.setEventListener(mPlayerListener);

        // Set up video output
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        //vout.setSubtitlesView(mSurfaceSubtitles);
        vout.addCallback(this);
        vout.attachViews();

        Media m = new Media(libvlc, Uri.parse(rtspUrl));
        m.setHWDecoderEnabled(true, false);
        m.addOption(":network-caching=0");
        m.addOption(":clock-jitter=0");
        m.addOption(":clock-synchro=0");
        m.addOption(":no-audio");

        mMediaPlayer.setMedia(m);
        mMediaPlayer.play();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
        vlcVout.setWindowSize(mSurface.getWidth(), mSurface.getHeight());
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;
    }
}

