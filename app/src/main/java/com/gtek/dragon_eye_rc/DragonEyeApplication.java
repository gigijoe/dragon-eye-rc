package com.gtek.dragon_eye_rc;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DragonEyeApplication extends Application {
    private static DragonEyeApplication mInstance;

    public static synchronized DragonEyeApplication getInstance() {
        return mInstance;
    }

    public UDPClient mUdpClient = null;
    public String mBaseAddress = "0.0.0.0";

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        ExecutorService exec = Executors.newCachedThreadPool();
        mUdpClient = new UDPClient(this);
        exec.execute(mUdpClient);
    }

    @Override
    public void onTerminate() {
        mUdpClient.setUdpLife(false);
        super.onTerminate();
    }
}
