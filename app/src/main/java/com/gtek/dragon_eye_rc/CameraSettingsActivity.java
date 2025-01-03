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

import static com.gtek.dragon_eye_rc.DragonEyeBase.Type.BASE_A;
import static com.gtek.dragon_eye_rc.DragonEyeBase.Type.BASE_B;

public class CameraSettingsActivity extends AppCompatActivity {
    private Context mContext;
    private AlertDialog.Builder mBuilder;

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

        mBuilder = new AlertDialog.Builder(this)
                .setTitle("Error !!!")
                .setMessage("Base response timeout !!!")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //finish();
                    }
                });

        DragonEyeBase b = DragonEyeApplication.getInstance().getSelectedBase();
        if(b != null) {
            String s = b.getCameraSettings();
            if (s.startsWith("#CameraSettings")) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sp.edit();

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

                        if (TextUtils.equals(keyValue[0], "sensor-id")) {
                            if (Integer.parseInt(keyValue[1]) == 0)
                                editor.putString("camera_id", "camera_1");
                            if (Integer.parseInt(keyValue[1]) == 1)
                                editor.putString("camera_id", "camera_2");
                            if (Integer.parseInt(keyValue[1]) == 2)
                                editor.putString("camera_id", "camera_3");
                        } else if (TextUtils.equals(keyValue[0], "wbmode")) {
                            editor.putString("wbmode", keyValue[1]);
                        } else if (TextUtils.equals(keyValue[0], "tnr-mode")) {
                            editor.putString("tnr_mode", keyValue[1]);
                        } else if (TextUtils.equals(keyValue[0], "tnr-strength")) {
                            //editor.putInt("tnr_strength", Integer.parseInt(keyValue[1]) * 100);
                            editor.putInt("tnr_strength", Math.round(Float.parseFloat(keyValue[1]) * 10));
                        } else if (TextUtils.equals(keyValue[0], "ee-mode")) {
                            editor.putString("ee_mode", keyValue[1]);
                        } else if (TextUtils.equals(keyValue[0], "ee-strength")) {
                            //editor.putInt("ee_strength", Integer.parseInt(keyValue[1]) * 100);
                            editor.putInt("ee_strength", Math.round(Float.parseFloat(keyValue[1]) * 10));
                        } else if (TextUtils.equals(keyValue[0], "exposurecompensation")) {
                            //editor.putInt("exposure_compensation", Integer.parseInt(keyValue[1]) * 200);
                            editor.putInt("exposure_compensation", Math.round(Float.parseFloat(keyValue[1]) * 20));
                        } else if (TextUtils.equals(keyValue[0], "exposurethreshold")) {
                            editor.putInt("exposure_threshold", Integer.parseInt(keyValue[1]));
                        } else if (TextUtils.equals(keyValue[0], "flip-mode")) {
                            editor.putInt("flip_mode", Integer.parseInt(keyValue[1]));
                        }
                    }
                }
                editor.commit();
            }
        }

        FloatingActionButton fab = findViewById(R.id.camera_settings_apply);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringBuffer udpPayload = new StringBuffer();
                udpPayload.append("#CameraSettings\n");
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

                String s = sp.getString("camera_id", "");
                switch(s) {
                    case "camera_1": udpPayload.append("sensor-id=0");
                        break;
                    case "camera_2": udpPayload.append("sensor-id=1");
                        break;
                    case "camera_3": udpPayload.append("sensor-id=2");
                        break;
                }

                udpPayload.append("\n");

                s = sp.getString("wbmode", "0"); /* Default value 0 */
                udpPayload.append("wbmode=" + Integer.parseInt(s));

                udpPayload.append("\n");

                s = sp.getString("tnr_mode", "2"); /* Default value 2 */
                udpPayload.append("tnr-mode=" + Integer.parseInt(s));

                udpPayload.append("\n");

                Integer i = sp.getInt("tnr_strength", 10); /* Default value 100 */
                udpPayload.append("tnr-strength=" + (float)i / 10);

                udpPayload.append("\n");

                s = sp.getString("ee_mode", "1"); /* Default value 1 */
                udpPayload.append("ee-mode=" + Integer.parseInt(s));

                udpPayload.append("\n");

                i = sp.getInt("ee_strength", 0); /* Default value 0 */
                udpPayload.append("ee-strength=" + (float)i / 10);

                udpPayload.append("\n");

                udpPayload.append("gainrange=\"1 16\"\n");
                udpPayload.append("ispdigitalgainrange=\"1 8\"\n");
                udpPayload.append("exposuretimerange=\"5000000 10000000\"\n");

                i = sp.getInt("exposure_compensation", 0); /* Default value 0 */
                udpPayload.append("exposurecompensation=" + (float)i / 20);

                udpPayload.append("\n");

                i = sp.getInt("exposure_threshold", 5); /* Default value 5 */
                udpPayload.append("exposurethreshold=" + i);

                udpPayload.append("\n");

                i = sp.getInt("flip_mode", 3); /* Default value 3 */
                udpPayload.append("flip-mode=" + i);

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

                            b.setCameraSettings(udpPayload.toString());
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

    private void onUdpRx(String str) {
        System.out.println("UDP RX : " + str);
        int index = str.indexOf(':');
        String addr = str.substring(0, index);
        String s = str.substring(index + 1);
        DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(addr);
        if (b != null) {
            if(TextUtils.equals(s, "#Ack")) {
                Toast.makeText(mContext,"Okay", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("udpMsg")) {
                if (intent.hasExtra("udpRcvMsg")) {
                    onUdpRx(intent.getStringExtra("udpRcvMsg"));
                } else if (intent.hasExtra("udpSendMsg")) {
                    System.out.println("UDP TX : " + intent.getStringExtra("udpSendMsg"));
                } else if (intent.hasExtra("udpPollTimeout")) {
                    System.out.println("UDP RX Timeout");
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
            setPreferencesFromResource(R.xml.camera_preferences, rootKey);
        }
    }
}