package com.gtek.dragon_eye_rc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static com.gtek.dragon_eye_rc.DragonEyeBase.UDP_REMOTE_PORT;

public class SystemSettingsActivity extends AppCompatActivity {
    private static final int RTP_REMOTE_PORT = 5000;
    private Context mContext;
    private AlertDialog.Builder mBuilder;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: /* RX */
                    //udpRcvStrBuf.append(msg.obj.toString());
                    //txt_Recv.setText(udpRcvStrBuf.toString());
                    //System.out.println("UDP RX : " + msg.obj.toString());
                    //String s = msg.obj.toString();
                    String str = msg.obj.toString();
                    System.out.println("UDP RX : " + str);
                    int index = str.indexOf(':');
                    String addr = str.substring(0, index);
                    String s = str.substring(index+1);
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

        mBuilder = new AlertDialog.Builder(this)
                .setTitle("Error !!!")
                .setMessage("Fail to save system settings ...")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //finish();
                    }
                });

        DragonEyeBase b = DragonEyeApplication.getInstance().getSelectedBase();
        if(b != null) {
            String s = b.getSystemSettings();
            if (s.startsWith("#SystemSettings")) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("video_output", "off");

                String lines[] = s.split("\\r?\\n");
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].length() > 0) {
                        if (lines[i].startsWith("#"))
                            continue;
                    }
                    //System.out.println(i + " " + lines[i]);
                    String keyValue[] = lines[i].split("=");
                    if (keyValue.length == 2) {

                        System.out.println("[ " + keyValue[0] + " ] = " + keyValue[1]);

                        if (TextUtils.equals(keyValue[0], "base.type")) {
                            if (TextUtils.equals(keyValue[1], "A")) {
                                editor.putString("base_type", "base_a");
                            } else if (TextUtils.equals(keyValue[1], "B")) {
                                editor.putString("base_type", "base_b");
                            }
                        } else if (TextUtils.equals(keyValue[0], "base.mog2.threshold")) {
                            editor.putInt("mog2_threshold", Integer.parseInt(keyValue[1]));
                        } else if (TextUtils.equals(keyValue[0], "base.new.target.restriction")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putBoolean("new_target_restriction", true);
                            else
                                editor.putBoolean("new_target_restriction", false);
                        } else if (TextUtils.equals(keyValue[0], "video.output.screen")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putString("video_output", "screen");
                        } else if (TextUtils.equals(keyValue[0], "video.output.file")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putBoolean("save_file", true);
                            else
                                editor.putBoolean("save_file", false);
                        } else if (TextUtils.equals(keyValue[0], "video.output.rtp")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putString("video_output", "rtp");
                        } else if (TextUtils.equals(keyValue[0], "video.output.hls")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putString("video_output", "hls");
                        } else if (TextUtils.equals(keyValue[0], "video.output.rtsp")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putString("video_output", "rtsp");
                        } else if (TextUtils.equals(keyValue[0], "video.output.result")) {
                            if (TextUtils.equals(keyValue[1], "yes"))
                                editor.putBoolean("show_result", true);
                            else
                                editor.putBoolean("show_result", false);
                        }
                    }
                }
                editor.commit();
            }
        }

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
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

                String s = sp.getString("base_type", "");
                switch(s) {
                    case "base_a": udpPayload.append("base.type=A");
                        break;
                    case "base_b": udpPayload.append("base.type=B");
                        break;
                }

                udpPayload.append("\n");

                udpPayload.append("base.rtp.remote.host=" + DragonEyeApplication.getInstance().getSelectedBase().getAddress());
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

                b = sp.getBoolean("fake_target_detection", false);
                if(b)
                    udpPayload.append("base.fake.target.detection=yes");
                else
                    udpPayload.append("base.fake.target.detection=no");
                udpPayload.append("\n");

                b = sp.getBoolean("bug_trigger", false);
                if(b)
                    udpPayload.append("base.bug.trigger=yes");
                else
                    udpPayload.append("base.bug.trigger=no");
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
                            DragonEyeBase b = DragonEyeApplication.getInstance().getSelectedBase();
                            DragonEyeApplication.getInstance().mUdpClient.send(b.getAddress(), DragonEyeBase.UDP_REMOTE_PORT, udpPayload.toString());
                            b.startResponseTimer();

                            b.setSystemSettings(udpPayload.toString());
                        }
                    }
                });
                thread.start();
            }
        });

        IntentFilter udpRcvIntentFilter = new IntentFilter("udpMsg");
        registerReceiver(broadcastReceiver, udpRcvIntentFilter);

        IntentFilter baseRcvIntentFilter = new IntentFilter("baseMsg");
        registerReceiver(broadcastReceiver, baseRcvIntentFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("udpMsg")) {
                if (intent.hasExtra("udpRcvMsg")) {
                    Message message = new Message();
                    message.obj = intent.getStringExtra("udpRcvMsg");
                    message.what = 1;
                    //Log.i("主界面Broadcast", "收到" + message.obj.toString());
                    mHandler.sendMessage(message);
                } else if (intent.hasExtra("udpSendMsg")) {
                    Message message = new Message();
                    message.obj = intent.getStringExtra("udpSendMsg");
                    message.what = 2;
                    //Log.i("主界面Broadcast", "發送" + message.obj.toString());
                    mHandler.sendMessage(message);
                } else if (intent.hasExtra("udpPollTimeout")) {
                    Message message = new Message();
                    message.what = 3;
                    //Log.i("主界面Broadcast","逾時");
                    mHandler.sendMessage(message);
                }
            } else if(intent.getAction().equals("baseMsg")) {
                if (intent.hasExtra("baseResponseTimeout")) {
                    try {
                        mBuilder.show();
                    } catch (WindowManager.BadTokenException e) {
                        //use a log message
                    }
                } else if(intent.hasExtra("baseResponsed")) {

                }
            }
        }
    };

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.system_preferences, rootKey);
        }
    }
}