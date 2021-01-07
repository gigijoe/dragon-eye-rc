package com.gtek.dragoneye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private static final int UDP_REMOTE_PORT = 4999;

    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    private Context mContext;
    private Activity mActivity;

    private static int serNo = 0;

    private HandlerThread mHandlerThread;
    private Handler mTonePlayerHandler;
    private TonePlayer tonePlayer;
    public boolean isRingTonePlaying = false;

    public boolean mBaseTypeA = true;

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
                    TextView status = (TextView) findViewById(R.id.textview_status);
                    if(TextUtils.equals(s, "#Started")) {
                        status.setText("Started ...");
                        Toast.makeText(mContext,"F3F Base Started", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.equals(s, "#Stopped")) {
                        status.setText("Stopped !!!");
                        Toast.makeText(mContext, "F3F Base Stopped", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.equals(s, "#BaseTypeA")) {
                        mBaseTypeA = true;
                    } else if(TextUtils.equals(s, "#BaseTypeB")) {
                        mBaseTypeA = false;
                    } else if(s != null) {
                        if(s.contains("#Trigger:")) {
                            System.out.println(s);
                            int i = Integer.parseInt(s.substring(9));
                            if(i != serNo) {
                                if(isRingTonePlaying == false) {
                                    System.out.println("Play tone ...");
                                    isRingTonePlaying = true;
                                    tonePlayer.startPlay();
                                    mTonePlayerHandler.post(ringTonePlayerThread);
                                }
                                Toast.makeText(mContext, "F3F Base Trigger", Toast.LENGTH_SHORT).show();
                                serNo = i;
                            }
                        }
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

    public static String stringAddress(int ipAddress) {
        ipAddress = (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ?
                Integer.reverseBytes(ipAddress) : ipAddress;
        byte[] ipAddressByte = BigInteger.valueOf(ipAddress).toByteArray();
        try {
            InetAddress myAddr = InetAddress.getByAddress(ipAddressByte);
            return myAddr.getHostAddress();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            Log.e("Wifi : ", "Error getting IP address ", e);
        }
        return "null";
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        mActivity = MainActivity.this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHandlerThread = new HandlerThread("ringing_ht");
        mHandlerThread.start();
        mTonePlayerHandler = new Handler(mHandlerThread.getLooper());
        tonePlayer = new TonePlayer();

        checkPermission();

        FloatingActionButton bs = findViewById(R.id.button_start);
        bs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                        final DhcpInfo dhcp = manager.getDhcpInfo();
                        String payloadString = "#Start";
                        DragonEyeApplication.getInstance().mUdpClient.send(stringAddress(dhcp.gateway), UDP_REMOTE_PORT, payloadString);
                    }
                });
                thread.start();
            }
        });

        FloatingActionButton bp = findViewById(R.id.button_pause);
        bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                        final DhcpInfo dhcp = manager.getDhcpInfo();
                        String payloadString = "#Stop";
                        DragonEyeApplication.getInstance().mUdpClient.send(stringAddress(dhcp.gateway), UDP_REMOTE_PORT, payloadString);
                    }
                });
                thread.start();
            }
        });

        IntentFilter udpRcvIntentFilter = new IntentFilter("udpMsg");
        registerReceiver(broadcastReceiver, udpRcvIntentFilter);

        @SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        final DhcpInfo dhcp = manager.getDhcpInfo();

        System.out.println("IP : " + stringAddress(dhcp.ipAddress));
        System.out.println("Netmask : " + stringAddress(dhcp.netmask));
        System.out.println("Gateway : " + stringAddress(dhcp.gateway));
        System.out.println("DNS 1 : " + stringAddress(dhcp.dns1));
        System.out.println("DNS 2 : " + stringAddress(dhcp.dns2));

        TextView wifi_ssid = (TextView) findViewById(R.id.textview_ssid);
        wifi_ssid.setText(stringAddress(dhcp.gateway));

        ConnectionUtil mConnectionMonitor = new ConnectionUtil(this);
        mConnectionMonitor.onInternetStateListener(new ConnectionUtil.ConnectionStateListener() {
            @Override
            public void onWifiConnection(boolean connected) {
                if(connected) {
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                        System.out.println("Wifi SSID : " + wifiInfo.getSSID());
                        wifi_ssid.setText(wifiInfo.getSSID());

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                @SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                                final DhcpInfo dhcp = manager.getDhcpInfo();
                                String payloadString = "#Status";
                                DragonEyeApplication.getInstance().mUdpClient.send(stringAddress(dhcp.gateway), UDP_REMOTE_PORT, payloadString);
                                payloadString = "#BaseType";
                                DragonEyeApplication.getInstance().mUdpClient.send(stringAddress(dhcp.gateway), UDP_REMOTE_PORT, payloadString);
                            }
                        });
                        thread.start();
                    }
                } else {
                    wifi_ssid.setText("Unknown SSID");
                }
            }
        });
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

    final Runnable ringTonePlayerThread = new Runnable() {
        @Override
        public void run() {
            byte[] buffer = new byte[160];
            InputStream is;
            if(mBaseTypeA)
                is = getResources().openRawResource(R.raw.r_a);
            else
                is = getResources().openRawResource(R.raw.r_b);
            try {
                while (is.read(buffer) != -1) {
                    tonePlayer.play(buffer);
                    if(!isRingTonePlaying)
                        break;
                }
                isRingTonePlaying = false;
                System.out.println("Finish tone ...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onStop() {
        //System.out.println("R.onStop");
        isRingTonePlaying = false;
        tonePlayer.stopPlay();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.item_rtsp_video:
                Intent intent = new Intent(getApplicationContext(), VideoActivity.class);
                @SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
                final DhcpInfo dhcp = manager.getDhcpInfo();
                //intent.putExtra(VideoActivity.RTSP_URL, "RTSP://10.0.0.1:8554/test");
                intent.putExtra(VideoActivity.RTSP_URL, "RTSP://" + stringAddress(dhcp.gateway) + ":8554/test");
                startActivity(intent);
                break;
            case R.id.item_system_setup:
                intent = new Intent(getApplicationContext(), SystemSettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.item_camera_setup:
                intent = new Intent(getApplicationContext(), CameraSettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void checkPermission(){
        if(ContextCompat.checkSelfPermission(mActivity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                ||  ContextCompat.checkSelfPermission(mActivity,Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                ||  ContextCompat.checkSelfPermission(mActivity,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Do something, when permissions not granted
            if(ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.INTERNET)
                    || ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.ACCESS_WIFI_STATE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.ACCESS_FINE_LOCATION)){
                // If we should give explanation of requested permissions

                // Show an alert dialog here with request explanation
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setMessage("Use SIP, Access internet" +
                        " Wifi state permissions are required to do the task.");
                builder.setTitle("Please grant those permissions");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(
                                mActivity,
                                new String[]{
                                        Manifest.permission.INTERNET,
                                        Manifest.permission.ACCESS_WIFI_STATE,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                },
                                MY_PERMISSIONS_REQUEST_CODE
                        );
                    }
                });
                builder.setNeutralButton("Cancel",null);
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                // Directly request for required permissions, without explanation
                ActivityCompat.requestPermissions(
                        mActivity,
                        new String[]{
                                Manifest.permission.INTERNET,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                        },
                        MY_PERMISSIONS_REQUEST_CODE
                );
            }
        } else {
            // Do something, when permissions are already granted
            Toast.makeText(mContext,"Permissions already granted",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_CODE:{
                // When request is cancelled, the results array are empty
                if((grantResults.length > 0) &&
                                (grantResults[0]
                                        + grantResults[1]
                                        + grantResults[2]
                                        == PackageManager.PERMISSION_GRANTED
                                )
                ){
                    // Permissions are granted
                    Toast.makeText(mContext,"Permissions granted.",Toast.LENGTH_SHORT).show();
                }else {
                    // Permissions are denied
                    Toast.makeText(mContext,"Permissions denied.",Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}