package com.gtek.dragon_eye_rc;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class DragonEyeApplication extends Application {
    private static DragonEyeApplication mInstance;

    public static synchronized DragonEyeApplication getInstance() {
        return mInstance;
    }

    public UDPClient mUdpClient = null;
    public int mSelectedBaseIndex = -1;

    public TonePlayer mTonePlayer = null;
    private final AtomicBoolean isPriorityPlaying = new AtomicBoolean(false);
    ArrayList<Integer> toneArray = null;

    public void playTone(int resourceId) {
        if(mTonePlayer.isPlaying()) {
            if(isPriorityPlaying.get())
                return;
            else
                mTonePlayer.stopPlay();
        } else {
            isPriorityPlaying.set(false);
        }

        mTonePlayer.startPlay(resourceId);
        Thread t = new Thread(mTonePlayer);
        t.start();
    }

    public void playPriorityTone(int resourceId) {
        if(mTonePlayer.isPlaying()) {
            mTonePlayer.stopPlay();
           if(toneArray != null)
               toneArray.clear();
        }

        isPriorityPlaying.set(true);

        mTonePlayer.startPlay(resourceId);
        Thread t = new Thread(mTonePlayer);
        t.start();
    }

    public void playTone(ArrayList<Integer> a) {
        toneArray = a;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int tone : a) {
                    mTonePlayer.startPlay(tone);
                    Thread t = new Thread(mTonePlayer);
                    t.start();

                    while(mTonePlayer.isPlaying()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(mTonePlayer.interrupt.get())
                        break;
                }
                toneArray = null;
            }
        });
        thread.start();
    }

    public void stopTone() {
        if(mTonePlayer.isPlaying()) {
            mTonePlayer.stopPlay();
            if(toneArray != null)
                toneArray.clear();
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

    @Override
    public void onCreate() {
        super.onCreate();

        NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
        requestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.requestNetwork(requestBuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                cm.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
            }
        });

        mInstance = this;

        mUdpClient = new UDPClient(getApplicationContext());
        mUdpClient.start();

        mTonePlayer = new TonePlayer(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        Log.i("DragonEyeApplication", "onTerminate");
        mUdpClient.stop();
        mTonePlayer.stopPlay();
        super.onTerminate();
    }
}
