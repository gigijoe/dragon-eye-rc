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
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DragonEyeApplication extends Application {
    private static DragonEyeApplication mInstance;

    public static synchronized DragonEyeApplication getInstance() {
        return mInstance;
    }

    public UDPClient mUdpClient = null;
    public String mBaseAddress = null;
    public String mBaseAddressA = null;
    public String mBaseAddressB = null;

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
    }

    @Override
    public void onTerminate() {
        Log.i("DragonEyeApplication", "onTerminate");
        mUdpClient.stop();
        super.onTerminate();
    }
/*
    public void StartUdpClient() {
        ExecutorService exec = Executors.newCachedThreadPool();
        if(mUdpClient.isRunning() == false)
            exec.execute(mUdpClient);
    }
 */
}
