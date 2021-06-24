package com.gtek.dragon_eye_rc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class MiscSettingsActivity extends AppCompatActivity {
    private Context mContext;
    private androidx.appcompat.app.AlertDialog.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_misc_settings);

        mContext = getApplicationContext();

        mBuilder = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Error !!!")
                .setMessage("Fail run system settings ...")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //finish();
                    }
                });

        Button btnDelFiles = (Button) findViewById(R.id.buttonDeleteAllVideoFiles);
        btnDelFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteVideoFilesDialog();
            }
        });
/*
        TextView t = (TextView) findViewById(R.id.textViewTriggerRelayDebouenceValue);
        View v = (View) findViewById(R.id.textViewTriggerRelayDebouence);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(mContext, view, Gravity.CENTER_HORIZONTAL);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_trigger_relay_debouence, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch(menuItem.getItemId()) {
                            case R.id.t200ms:
                                break;
                            case R.id.t400ms:
                                break;
                            case R.id.t800ms:
                                break;
                            case R.id.t1200ms:
                                break;
                        }
                        return false;
                    }
                });

                popup.show();
            }
        });
*/
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

    private void onUdpRx(String str) {
        System.out.println("UDP RX : " + str);
        int index = str.indexOf(':');
        String addr = str.substring(0, index);
        String s = str.substring(index + 1);
        DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(addr);
        if (b != null) {
            if (TextUtils.equals(s, "#Ack")) {
                b.stopResponseTimer();
                Toast.makeText(mContext, "Update Successful", Toast.LENGTH_SHORT).show();
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

    private void deleteVideoFilesDialog() {
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setTitle("Delete all video files");
        MyAlertDialog.setMessage("Are you sure ?");
        MyAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DragonEyeBase b = DragonEyeApplication.getInstance().getSelectedBase();
                        DragonEyeApplication.getInstance().requestVideoFiles(b, "DeleteAll");
                    }
                });
                thread.start();
            }
        });
        MyAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        MyAlertDialog.show();
    }
}