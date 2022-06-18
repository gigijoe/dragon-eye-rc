package com.gtek.dragon_eye_rc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DragonEyeApplication extends Application {
    private static DragonEyeApplication mInstance;

    public static synchronized DragonEyeApplication getInstance() {
        return mInstance;
    }

    public UDPClient mUdpClient = null;
    public int mSelectedBaseIndex = -1;

    private final AtomicBoolean isPriorityPlaying = new AtomicBoolean(false);
    ArrayList<String> mToneArray = null;
    private final String audioAssetFolder = "48k/";

    synchronized public void playTone(String asset) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(!pm.isInteractive())
            return;

        System.out.println("playTone " + asset);

        if(isPlaying()) {
            System.out.println("isPlaying ...");
            if(isPriorityPlaying.get())
                return;
            else
                stopPlaying();
        } else {
            isPriorityPlaying.set(false);
        }

        startPlaying(audioAssetFolder + asset);
    }

    synchronized public void playPriorityTone(String asset) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(!pm.isInteractive())
            return;

        System.out.println("playPriorityTone " + asset);

        stopTone();

        isPriorityPlaying.set(true);

        startPlaying(audioAssetFolder + asset);
    }

    synchronized public void playTone(ArrayList<String> a) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(!pm.isInteractive())
            return;

        if(a.size() == 0)
            return;

        for(int i=0;i<a.size();i++) {
            System.out.println("playTone " + a.get(i));
        }

        stopTone();

        mToneArray = a;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //for(int tone : a) {
                for(int i=0;i<mToneArray.size();i++) {
                    //startPlaying(audioAssetFolder + tone);
                    startPlaying(audioAssetFolder + mToneArray.get(i));

                    while(isLoading()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    while(isPlaying()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    stopPlaying();
                }
                mToneArray.clear();
                //mToneArray = null;
            }
        });
        //thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void stopTone() {
        System.out.println("stopTone");
        if(isPlaying()) {
            stopPlaying();
            if(mToneArray != null)
                mToneArray.clear();
        }
    }

    public ArrayList<DragonEyeBase> mBaseList = new ArrayList<>();

    public DragonEyeBase findBaseByAddress(String addr) {
        for(DragonEyeBase b : mBaseList) {
            if(TextUtils.equals(b.getAddress(), addr))
                return b;
        }
        return null;
    }

    public void triggerBase(DragonEyeBase.Type t) {
        for(DragonEyeBase b : mBaseList) {
            if(b.getType() == t) {
                if (!b.isBaseTrigger())
                    b.baseTrigger();
            }
        }
    }

    public void addBase(DragonEyeBase b) {
        mBaseList.add(b);
    }

    public void selectBaseByIndex(int index) {
        if(mBaseList.get(index) != null)
            mSelectedBaseIndex = index;
    }

    public DragonEyeBase getSelectedBase() {
        if(mSelectedBaseIndex == -1)
            return null;
        return mBaseList.get(mSelectedBaseIndex);
    }

    public void requestSystemSettings(DragonEyeBase b) {
        final String payloadString = "#SystemSettings";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestCameraSettings(DragonEyeBase b) {
        final String payloadString = "#CameraSettings";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestFirmwareVersion(DragonEyeBase b) {
        final String payloadString = "#FirmwareVersion";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestStatus(DragonEyeBase b) {
        final String payloadString = "#Status";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestStart(DragonEyeBase b) {
        final String payloadString = "#Start";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestStop(DragonEyeBase b) {
        final String payloadString = "#Stop";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestCompassLock(DragonEyeBase b) {
        final String payloadString = "#CompassLock";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestCompassUnlock(DragonEyeBase b) {
        final String payloadString = "#CompassUnlock";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestCompassSaveSettings(DragonEyeBase b) {
        final String payloadString = "#CompassSaveSettings";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestCompassSuspend(DragonEyeBase b) {
        final String payloadString = "#CompassSuspend";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestCompassResume(DragonEyeBase b) {
        final String payloadString = "#CompassResume";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestFirmwareUpgrade(DragonEyeBase b) {
        final String payloadString = "#FirmwareUpgrade";
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestVideoFiles(DragonEyeBase b, String cmd) {
        final String payloadString = "#VideoFiles:" + cmd;
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    public void requestSystemCommand(DragonEyeBase b, String cmd) {
        final String payloadString = "#SystemCommand:" + cmd;
        mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, payloadString);
        b.startResponseTimer();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
        requestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.requestNetwork(requestBuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cm.bindProcessToNetwork(network);
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
            }
        });

        mInstance = this;

        mUdpClient = new UDPClient(getApplicationContext());

        setDefaultStreamValues(this);

        audioInit(getAssets());
        audioCache(getAssets(), audioAssetFolder + "r_go.raw");
        audioCache(getAssets(), audioAssetFolder + "r_outside.raw");
        audioCache(getAssets(), audioAssetFolder + "r_a.raw");
        audioCache(getAssets(), audioAssetFolder + "r_b.raw");
        audioCache(getAssets(), audioAssetFolder + "r_final.raw");
        audioCache(getAssets(), audioAssetFolder + "r_e.raw");
        audioCache(getAssets(), audioAssetFolder + "smb_jump_small.raw");
        audioCache(getAssets(), audioAssetFolder + "smb_jump_super.raw");
    }

    @Override
    public void onTerminate() {
        Log.i("DragonEyeApplication", "onTerminate");
        mUdpClient.stop();
        stopPlaying();
        super.onTerminate();
    }

    @SuppressWarnings("unchecked")
    public Activity getActivity() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }

            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    static void setDefaultStreamValues(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            String sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            int defaultSampleRate = Integer.parseInt(sampleRateStr);
            String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);
            native_setDefaultStreamValues(defaultSampleRate, defaultFramesPerBurst);
        }
    }

    public native String stringFromJNI();
    public native void audioInit(AssetManager assetManager);
    public native void audioCache(AssetManager assetManager, String fileName);
    public native void startCachePlaying(String fileName);
    public native void startPlaying(String fileName);
    public native void stopPlaying();
    public native boolean isPlaying();
    public native boolean isLoading();

    private static native void native_setDefaultStreamValues(int defaultSampleRate,
                                                             int defaultFramesPerBurst);

    static {
        System.loadLibrary("native-lib");
    }
}
