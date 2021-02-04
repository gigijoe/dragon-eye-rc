package com.gtek.dragon_eye_rc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

class ConnectionUtil implements LifecycleObserver {

    private Context mContext;
    private NetworkStateReceiver mNetworkStateReceiver;

    interface ConnectionStateListener {
        //void onAvailable(boolean isAvailable);
        void onWifiConnection(boolean connected);
    }

    ConnectionUtil(Context context) {
        mContext = context;
        ((AppCompatActivity) mContext).getLifecycle().addObserver(this);
    }

    void onInternetStateListener(ConnectionStateListener listener) {
        mNetworkStateReceiver = new NetworkStateReceiver(listener);
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        // Registering the Context Registered Receiver
        mContext.registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {

        // Removing lifecycler owner observer
        ((AppCompatActivity) mContext).getLifecycle().removeObserver(this);

        // Unregistering the Context Registered Receiver
        if (mNetworkStateReceiver != null)
            mContext.unregisterReceiver(mNetworkStateReceiver);

    }

    public class NetworkStateReceiver extends BroadcastReceiver {

        ConnectionStateListener mListener;

        public NetworkStateReceiver(ConnectionStateListener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network nw = connectivityManager.getActiveNetwork();
                    if (nw != null) {
                        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
                        if(actNw != null) {
                            if(actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                //System.out.println("Wifi connected ...");
                                mListener.onWifiConnection(true);
                            } else {
                                //System.out.println("Wifi connection lost !!!");
                                mListener.onWifiConnection(false);
                            }
                        }
                    }
                }
            }
        }
    }

}