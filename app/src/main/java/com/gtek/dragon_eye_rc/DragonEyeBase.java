package com.gtek.dragon_eye_rc;

import android.text.TextUtils;

import androidx.annotation.NonNull;

public class DragonEyeBase {

    public static final int UDP_REMOTE_PORT = 4999;

    DragonEyeBase(String baseType, String address) {
        if(TextUtils.equals(baseType, "BASE_A"))
            mType = Type.BASE_A;
        else if(TextUtils.equals(baseType, "BASE_B"))
            mType = Type.BASE_B;
        else
            mType = Type.BASE_UNKNOWN;

        mAddress = address;
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

    private String mAddress;

    private String mSystemSettings;
    private String mCameraSettings;

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

    public void Reset() {
        mType = Type.BASE_UNKNOWN;
        mStatus = Status.OFFLINE;
        mAddress = "";
        mSystemSettings = "";
        mCameraSettings = "";
    }

    public void setSystemSettings(String s) { mSystemSettings = s; }
    public void setCameraSettings(String s) { mCameraSettings = s; }

    final String getSystemSettings() { return mSystemSettings; }
    final String getCameraSettings() { return mCameraSettings; }
}
