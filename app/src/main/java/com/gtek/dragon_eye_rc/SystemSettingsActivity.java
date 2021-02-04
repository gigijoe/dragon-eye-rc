package com.gtek.dragon_eye_rc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SystemSettingsActivity extends AppCompatActivity {

    private static final int UDP_REMOTE_PORT = 4999;
    private static final int RTP_REMOTE_PORT = 5000;
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
                    //Toast.makeText(mContext,"UDP TX.",Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.system_settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.system_settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = getApplicationContext();

        FloatingActionButton fab = findViewById(R.id.system_settings_apply);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
/*
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
*/
                StringBuffer udpPayload = new StringBuffer();

                udpPayload.append("#SystemSettings\n");

                //SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
                //SharedPreferences sp = getSharedPreferences("SystemSettings", MODE_PRIVATE);
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                String s = sp.getString("base_type", "");
                switch(s) {
                    case "base_a": udpPayload.append("base.type=A");
                        break;
                    case "base_b": udpPayload.append("base.type=B");
                        break;
                }

                udpPayload.append("\n");

                udpPayload.append("base.hwswitch=no\n");

                //@SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                //final DhcpInfo dhcp = manager.getDhcpInfo();

                udpPayload.append("base.udp.remote.host=" + DragonEyeApplication.getInstance().mBaseAddress);
                udpPayload.append("\n");
                udpPayload.append("base.udp.remote.port=" + UDP_REMOTE_PORT);
                udpPayload.append("\n");
                udpPayload.append("base.rtp.remote.host=" + DragonEyeApplication.getInstance().mBaseAddress);
                udpPayload.append("\n");
                udpPayload.append("base.rtp.remote.port=" + RTP_REMOTE_PORT);
                udpPayload.append("\n");

                Integer i = sp.getInt("mog2_threshold", 0);
                udpPayload.append("base.mog2.threshold=" + i);
                udpPayload.append("\n");

                Boolean b = sp.getBoolean("new_target_restriction", false);
                if(b)
                    udpPayload.append("base.new.target.restriction=yes");
                else
                    udpPayload.append("base.new.target.restriction=no");

                udpPayload.append("\n");

                s = sp.getString("video_output", "");
                if(s.equals("screen"))
                    udpPayload.append("video.output.screen=yes");
                else
                    udpPayload.append("video.output.screen=no");

                udpPayload.append("\n");

                if(s.equals("rtp"))
                    udpPayload.append("video.output.rtp=yes");
                else
                    udpPayload.append("video.output.rtp=no");

                udpPayload.append("\n");

                if(s.equals("hls"))
                    udpPayload.append("video.output.hls=yes");
                else
                    udpPayload.append("video.output.hls=no");

                udpPayload.append("\n");

                if(s.equals("rtsp"))
                    udpPayload.append("video.output.rtsp=yes");
                else
                    udpPayload.append("video.output.rtsp=no");

                udpPayload.append("\n");

                b = sp.getBoolean("save_file", false);
                if(b)
                    udpPayload.append("video.output.file=yes");
                else
                    udpPayload.append("video.output.file=no");

                udpPayload.append("\n");

                b = sp.getBoolean("show_result", false);
                if(b)
                    udpPayload.append("video.output.result=yes");
                else
                    udpPayload.append("video.output.result=no");

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

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
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
            setPreferencesFromResource(R.xml.system_preferences, rootKey);
/*
            ListPreference base_type = (ListPreference) getPreferenceManager().findPreference("base_type");
            if(base_type != null) {
                System.out.println("Base Type : " + base_type.getValue());
            }
*/
        }
    }
}