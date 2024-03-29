package com.gtek.dragon_eye_rc;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

public class VideoActivity  extends Activity implements IVLCVout.Callback    {
    public final static String TAG = "VideoActivity";

    public static final String RTSP_URL = "rtspurl";

    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private AlertDialog.Builder mBuilder;

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
                case MediaPlayer.Event.Playing: Log.d(TAG, "MediaPlayer.Event.Playing");
                    break;
                case MediaPlayer.Event.Paused: Log.d(TAG, "MediaPlayer.Event.Paused");
                    break;
                case MediaPlayer.Event.Stopped: Log.d(TAG, "MediaPlayer.Event.Stopped");
                    break;
                case MediaPlayer.Event.EncounteredError: Log.d(TAG, "MediaPlayer.Event.EncounteredError");
                    mBuilder.show();
                    break;
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

        mBuilder = new AlertDialog.Builder(this)
                .setTitle("Error !!!")
                .setMessage("RTSP Video Error !!!")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

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
        //options.add("-v"); // verbosity
        //options.add("--aout=opensles");
        //options.add("--avcodec-codec=hevc");
        //options.add("--file-logging");
        //options.add("--logfile=vlc-log.txt");
        options.add("--file-caching=150");
        options.add("--log-verbose");
        options.add("--low-delay");
        options.add("--fps=30");
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
        m.addOption(":network-caching=600");
        //m.addOption(":live-caching=0");
        //m.addOption(":file-caching=0");
        //m.addOption(":sout-mux-caching=0");
        m.addOption(":clock-jitter=600");
        m.addOption(":clock-synchro=600");
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

    private static int serNoA = 0, serNoB = 0;

    synchronized void onBaseTrigger(DragonEyeBase b, String s) {
        char base = s.charAt(1);
        int serNo = Integer.parseInt(s.substring(2, s.length() - 1));
        if((base == 'A' && serNo != serNoA)) {
            System.out.println("Play tone ...");
            DragonEyeApplication.getInstance().playTone("smb_jump_small.raw"); // R.raw.r_a
            serNoA = serNo;
        } else if((base == 'B' && serNo != serNoB)) {
            System.out.println("Play tone ...");
            DragonEyeApplication.getInstance().playTone("smb_jump_super.raw"); // R.raw.r_b
            serNoB = serNo;
        }
    }
}

