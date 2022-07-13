package com.gtek.dragon_eye_rc;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class FirmwareActivity extends AppCompatActivity {
    private HttpServer mHttpd;
    private ProgressBar mProgressBar;
    private TextView mUpgradeStatus;
    private Button mUpgradeButton;
    private AlertDialog.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_upgrade);
        mUpgradeStatus = (TextView) findViewById(R.id.text_upgrade_status);
        mUpgradeButton = (Button) findViewById(R.id.button_upgrade);

        mHttpd = new HttpServer(getApplicationContext(), 8080);
        mHttpd.setOnStatusUpdateListener(new HttpServer.OnStatusUpdateListener() {
            @Override
            public void onUploadingProgressUpdate(final int progress) {
                FirmwareActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mProgressBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void onUploadingFile(final File file, final boolean done) {
                FirmwareActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (done) {
                            //mUploadInfo.setText("Upload file " + file.getName() + " done!");
                        } else {
                            //mProgressBar.setProgress(0);
                            //mUploadInfo.setText("Uploading file " + file.getName() + "...");
                        }
                    }
                });
            }

            @Override
            public void onDownloadingFile(final boolean done) {
                FirmwareActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (done) {
                            //mDownloadInfo.setText("Download file " + file.getName() + " done!") ;
                        } else {
                            //mDownloadInfo.setText("Downloading file " + file.getName() + " ...");
                        }
                    }
                });
            }
        });

        try {
            mHttpd.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mBuilder = new AlertDialog.Builder(this)
                .setTitle("Error !!!")
                .setMessage("Firmware upgrade fail !!!")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //finish();
                    }
                });

        DragonEyeBase b = DragonEyeApplication.getInstance().getSelectedBase();
        if(b != null) {
            TextView remoteVersion = (TextView) findViewById(R.id.text_remote_version);
            remoteVersion.setText(b.getFirmwareVersion());

            //Firmware version format : v[dec].[dec].[hex]

            boolean upgradeRequired = false;
            String app_version = getResources().getString(R.string.version);
            String base_version = b.getFirmwareVersion();
            if(app_version.startsWith("v") == false || base_version.startsWith("v") == false) {
                upgradeRequired = true;
            } else {
                String av[] = app_version.substring(1).split("\\.");
                String bv[] = base_version.substring(1).split("\\.");

                if(av.length != 3 || bv.length != 3) {
                    upgradeRequired = true;
                } else {
                    if(Integer.parseInt(av[0]) > Integer.parseInt(bv[0]))
                        upgradeRequired = true;
                    else if(Integer.parseInt(av[1]) > Integer.parseInt(bv[1]))
                        upgradeRequired = true;
                    else if(Integer.parseInt(av[2], 16) > Integer.parseInt(bv[2], 16))
                        upgradeRequired = true;
                }
            }

            //if(TextUtils.equals(base_version, app_version)) {
            if(upgradeRequired == false) {
                mUpgradeStatus.setText("Latest version already ...");
            } else {
                mUpgradeStatus.setText("Upgrade required ...");
                mUpgradeButton.setEnabled(true);
            }
        }

        mUpgradeButton = (Button) findViewById(R.id.button_upgrade);
        mUpgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upgradeDialog();
            }
        });

        IntentFilter udpRcvIntentFilter = new IntentFilter("udpMsg");
        registerReceiver(broadcastReceiver, udpRcvIntentFilter);

        IntentFilter baseRcvIntentFilter = new IntentFilter("baseMsg");
        registerReceiver(broadcastReceiver, baseRcvIntentFilter);
    }

    private void upgradeDialog() {
        android.app.AlertDialog.Builder MyAlertDialog = new android.app.AlertDialog.Builder(this);
        MyAlertDialog.setTitle("Upgrade firmware !!!");
        MyAlertDialog.setMessage("Are you sure ?");
        MyAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(DragonEyeApplication.getInstance().mBaseList.isEmpty())
                    return;

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DragonEyeBase b = DragonEyeApplication.getInstance().getSelectedBase();
                        DragonEyeApplication.getInstance().requestFirmwareUpgrade(b);
                    }
                });
                thread.start();

                mUpgradeStatus.setText("Waiting for upgrade ...");
                mUpgradeButton.setEnabled(false);
            }
        });
        MyAlertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        MyAlertDialog.show();
    }

    private void onUdpRx(String str) {
        System.out.println("UDP RX : " + str);
        int index = str.indexOf(':');
        String addr = str.substring(0, index);
        String s = str.substring(index + 1);
        DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(addr);
        if(b != DragonEyeApplication.getInstance().getSelectedBase())
            return;
        if (b != null && s != null) {
            if(TextUtils.equals(s, "#Ack")) {
                b.stopResponseTimer();
            } else if(s.startsWith("#UpgradeProgress")) {
                String ss[] = s.split(":");
                if(ss.length >= 2) {
                    mUpgradeStatus.setText("Downloading " + ss[1] + "%");
                    mProgressBar.setProgress(Integer.parseInt(ss[1]));
                }
            } else if(TextUtils.equals(s, "#FirmwareUpgrade:Success")) {
                mUpgradeStatus.setText("Upgrade successful ...");
                mProgressBar.setProgress(100);
                b.setFirmwareVersion(getResources().getString(R.string.version));
            } else if(TextUtils.equals(s, "#FirmwareUpgrade:Failed")) {
                mUpgradeStatus.setText("Upgrade failed !!!");
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
                if (intent.hasExtra("baseResponseTimeout")) { // base has no response of UDP request
                    try {
                        mBuilder.show();
                    } catch (WindowManager.BadTokenException e) {
                        //use a log message
                    }
                } else if(intent.hasExtra("baseStatusUpdate")) { // base status changed
                }
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        mHttpd.stop();
        super.onDestroy();
    }
}