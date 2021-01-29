package com.gtek.dragoneye;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraSettingsActivity extends AppCompatActivity {

    private static final int UDP_REMOTE_PORT = 4999;
    private Context mContext;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: /* RX */
                    //udpRcvStrBuf.append(msg.obj.toString());
                    //txt_Recv.setText(udpRcvStrBuf.toString());
                    System.out.println("UDP RX : " + msg.obj.toString());
                    String s = msg.obj.toString();
                    if(TextUtils.equals(s, "#Ack")) {
                        Toast.makeText(mContext,"Update Successful", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2: /* TX */
                    //udpSendStrBuf.append(msg.obj.toString());
                    //txt_Send.setText(udpSendStrBuf.toString());
                    System.out.println("UDP TX : " + msg.obj.toString());
                    break;
                case 3: /* Timeout */
                    //txt_Recv.setText(udpRcvStrBuf.toString());
                    System.out.println("UDP RX Timeout");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.camera_settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = getApplicationContext();

        FloatingActionButton fab = findViewById(R.id.camera_settings_apply);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
/*
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
*/
                StringBuffer udpPayload = new StringBuffer();

                udpPayload.append("#CameraSettings\n");

                //SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(CameraSettingsActivity.this);
                //SharedPreferences sp = getSharedPreferences("CameraSettings", MODE_PRIVATE);
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                String s = sp.getString("camera_id", "");
                switch(s) {
                    case "camera_1": udpPayload.append("sensor-id=0");
                        break;
                    case "camera_2": udpPayload.append("sensor-id=1");
                        break;
                }

                udpPayload.append("\n");

                s = sp.getString("wbmode", "0"); /* Default value 0 */
                udpPayload.append("wbmode=" + Integer.parseInt(s));

                udpPayload.append("\n");

                s = sp.getString("tnr_mode", "2"); /* Default value 2 */
                udpPayload.append("tnr-mode=" + Integer.parseInt(s));

                udpPayload.append("\n");

                Integer i = sp.getInt("tnr_strength", 100); /* Default value 100 */
                udpPayload.append("tnr-strength=" + (float)i / 100);

                udpPayload.append("\n");

                s = sp.getString("ee_mode", "1"); /* Default value 1 */
                udpPayload.append("ee-mode=" + Integer.parseInt(s));

                udpPayload.append("\n");

                i = sp.getInt("ee_strength", 0); /* Default value 0 */
                udpPayload.append("ee-strength=" + i / 100);

                udpPayload.append("\n");

                udpPayload.append("gainrange=\"1 16\"\n");
                udpPayload.append("ispdigitalgainrange=\"1 8\"\n");
                udpPayload.append("exposuretimerange=\"5000000 20000000\"\n");

                i = sp.getInt("exposure_compensation", 0); /* Default value 0 */
                udpPayload.append("exposurecompensation=" + (float)i / 200);

                udpPayload.append("\n");

                i = sp.getInt("exposure_threshold", 255); /* Default value 255 */
                udpPayload.append("exposurethreshold=" + i);

                udpPayload.append("\n");

                //System.out.println(udpPayload.toString());

                /*
                 *
                 */

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(udpPayload.toString())){
                            //@SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                            //final DhcpInfo dhcp = manager.getDhcpInfo();

                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddress, UDP_REMOTE_PORT, udpPayload.toString());
                            //DragonEyeApplication.getInstance().mUdpClient.send("192.168.168.76", UDP_REMOTE_PORT, udpPayload.toString());
                        }
                    }
                });
                thread.start();
            }
        });

        IntentFilter udpRcvIntentFilter = new IntentFilter("udpMsg");
        registerReceiver(broadcastReceiver, udpRcvIntentFilter);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("udpRcvMsg")) {
                Message message = new Message();
                message.obj = intent.getStringExtra("udpRcvMsg");
                message.what = 1;
                //Log.i("主界面Broadcast", "收到" + message.obj.toString());
                mHandler.sendMessage(message);
            } else if(intent.hasExtra("udpSendMsg")) {
                Message message = new Message();
                message.obj = intent.getStringExtra("udpSendMsg");
                message.what = 2;
                //Log.i("主界面Broadcast", "發送" + message.obj.toString());
                mHandler.sendMessage(message);
            } else if(intent.hasExtra("udpPollTimeout")) {
                Message message = new Message();
                message.what = 3;
                //Log.i("主界面Broadcast","逾時");
                mHandler.sendMessage(message);
            }
        }
    };

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.camera_preferences, rootKey);
        }
    }
}