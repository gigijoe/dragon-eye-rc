package com.gtek.dragon_eye_rc;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class DragonEyeBase {

    public static final int UDP_REMOTE_PORT = 4999;

    private Context mContext;
    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private int mMulticastReceiveCount = 0;
    private boolean mCompassLocked = false;
    private int mYaw = 0;
    private int mTemperature = -40000;
    private int mGpuLoad = 0;

    DragonEyeBase(Context context, String baseType, String address) {
        mContext = context;
        if(TextUtils.equals(baseType, "BASE_A"))
            mType = Type.BASE_A;
        else if(TextUtils.equals(baseType, "BASE_B"))
            mType = Type.BASE_B;
        else
            mType = Type.BASE_UNKNOWN;

        mAddress = address;

        isAlive.set(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while(isAlive.get()) {
                    try {
                        Thread.sleep(6000);
                        if(count == mMulticastReceiveCount) {
                            offline();
                            Intent intent = new Intent();
                            intent.setAction("baseMsg");
                            intent.putExtra("baseStatusUpdate", 0);
                            mContext.sendBroadcast(intent);
                        } else
                            count = mMulticastReceiveCount;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public enum Type {
        BASE_UNKNOWN,
        BASE_A,
        BASE_B;

        @NonNull
        @Override
        public String toString() {
            switch(this) {
                case BASE_UNKNOWN: return "X";
                case BASE_A: return "A";
                case BASE_B: return "B";
            }
            return "X";
        }
    }

    private Type mType = Type.BASE_UNKNOWN;

    public enum Status {
        OFFLINE,
        ONLINE,
        STOPPED,
        STARTED;

        @NonNull
        @Override
        public String toString() {
            switch(this) {
                case OFFLINE: return "Off Line";
                case ONLINE: return "On Line";
                case STOPPED: return "Stopped";
                case STARTED: return "Started";
            }
            return "Unknown Status";
        }
    }

    private Status mStatus = Status.OFFLINE;

    public class ResponseTimer implements Runnable {
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);
        public void run() {
            isCancelled.set(false);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
                isCancelled.set(true);
            }

            if(!isCancelled.get()) { /* Timeout */
                mStatus = Status.OFFLINE;
                Intent intent = new Intent();
                intent.setAction("baseMsg");
                intent.putExtra("baseResponseTimeout", 0);
                mContext.sendBroadcast(intent);
            } else {
                Intent intent = new Intent();
                intent.setAction("baseMsg");
                intent.putExtra("baseResponsed", 0);
                mContext.sendBroadcast(intent);
            }
        }

        public void cancel() { isCancelled.set(true); }
    }

    private ResponseTimer mResponseTimer = new ResponseTimer();
    private Thread mResponseTimerThread = null;

    public void startResponseTimer() {
        if(mResponseTimerThread != null) {
            if (mResponseTimerThread.isAlive()) {
                mResponseTimerThread.interrupt();
            }
        }
        mResponseTimerThread = new Thread(mResponseTimer);
        mResponseTimerThread.start();
    }

    public void stopResponseTimer() {
        mResponseTimer.cancel();
    }

    private String mAddress;
    private String mSystemSettings;
    private String mCameraSettings;
    private String mFirmwareVersion;

    public Type getType() { return mType; }
    public void setTypeBaseA() { mType = Type.BASE_A; }
    public void setTypeBaseB() { mType = Type.BASE_B; }

    public String getAddress() { return mAddress; }

    public Status getStatus() {
        return mStatus;
    }
    public void offline() { mStatus = Status.OFFLINE; }
    public void online() { mStatus = Status.ONLINE; }
    public void stopped() { mStatus = Status.STOPPED; }
    public void started() { mStatus = Status.STARTED; }

    public void reset() {
        mType = Type.BASE_UNKNOWN;
        mStatus = Status.OFFLINE;
        mAddress = "";
        mSystemSettings = "";
        mCameraSettings = "";
        mFirmwareVersion = "";
    }

    public void destroy() {
        isAlive.set(false);
    }

    public void setSystemSettings(String s) { mSystemSettings = s; }
    public void setCameraSettings(String s) { mCameraSettings = s; }
    public void setFirmwareVersion(String s) { mFirmwareVersion = s; }

    final String getSystemSettings() { return mSystemSettings; }
    final String getCameraSettings() { return mCameraSettings; }
    final String getFirmwareVersion() { return mFirmwareVersion; }

    public void multicastReceived() { mMulticastReceiveCount++; }

    public void compassLock() { mCompassLocked = true; }
    public void compassUnlock() { mCompassLocked = false; }
    public boolean isCompassLocked() { return mCompassLocked; }

    public void setYaw(int yaw) {
        mYaw = yaw;
    }

    public int yaw() { return mYaw; }

    public void setTemperature(int temp) {
        mTemperature = temp;
    }

    public int temperature() { return mTemperature; }

    public void setGpuLoad(int load) {
        mGpuLoad = load;
    }

    public int gpuLoad() { return mGpuLoad; }
}
