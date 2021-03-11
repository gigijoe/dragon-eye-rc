package com.gtek.dragon_eye_rc;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final int UDP_REMOTE_PORT = 4999;

    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    private Context mContext;
    private Activity mActivity;

    private static int serNo = 0;
    private static int serNoA = 0, serNoB = 0;

    private HandlerThread mHandlerThread;
    private Handler mTonePlayerHandler;
    private TonePlayer tonePlayer;
    public boolean isRingTonePlaying = false;
    public boolean isWifiConnected = false;

    public enum BaseType {
        BASE_UNKNOWN,
        BASE_A,
        BASE_B;
        
        @NonNull
        @Override
        public String toString() {
            switch(this) {
                case BASE_UNKNOWN: return "Base X";
                case BASE_A: return "Base A";
                case BASE_B: return "Base B";
            }
            return "Base X";
        }
    }

    public BaseType mBaseType = BaseType.BASE_UNKNOWN;
    public boolean mTriggerBaseA = true;

    public boolean mBaseStartedA = false;
    public boolean mBaseStartedB = false;

    public boolean mSystemSettingsFetched = false;
    public boolean mCameraSettingsFetched = false;

    private WifiManager.MulticastLock mMulticastLock = null;

    public ListView mListView;
    public lvAdapter mListAdapter;
    public TextView mStatusView;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: /* RX */
                    String str = msg.obj.toString();
                    System.out.println("UDP RX : " + str);
                    int index = str.indexOf(':');
                    String addr = str.substring(0, index);
                    String s = str.substring(index+1);
                    //udpRcvStrBuf.append(msg.obj.toString());
                    //txt_Recv.setText(udpRcvStrBuf.toString());
                    //System.out.println("UDP RX : " + msg.obj.toString());
                    index = -1;
                    if(TextUtils.equals(DragonEyeApplication.getInstance().mBaseAddressA, addr))
                        index = 0;
                    else if(TextUtils.equals(DragonEyeApplication.getInstance().mBaseAddressB, addr))
                        index = 1;
                    //TextView status = (TextView) findViewById(R.id.textview_status);
                    if(TextUtils.equals(s, "#Started")) {
                        if(index == 0 || index == 1) {
                            mListAdapter.updateView(index, mListView, null, "Started ...");
                            switch(index) {
                                case 0: mBaseStartedA = true;
                                    break;
                                case 1: mBaseStartedB = true;
                                    break;
                            }
                        }
                        //Toast.makeText(mContext,"F3F Base Started", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.equals(s, "#Stopped")) {
                        if(index == 0 || index == 1) {
                            mListAdapter.updateView(index, mListView, null, "Stopped !!!");
                            switch(index) {
                                case 0: mBaseStartedA = false;
                                    break;
                                case 1: mBaseStartedB = false;
                                    break;
                            }
                        }
                        //Toast.makeText(mContext, "F3F Base Stopped", Toast.LENGTH_SHORT).show();
                    } else if(s != null) {
                        if(s.startsWith("#SystemSettings")) {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("video_output", "off");

                            String lines[] = s.split("\\r?\\n");
                            for(int i=0;i<lines.length;i++) {
                                if(lines[i].length() > 0) {
                                    if(lines[i].startsWith("#"))
                                        continue;
                                }
                                //System.out.println(i + " " + lines[i]);
                                String keyValue[] = lines[i].split("=");
                                if (keyValue.length == 2) {

                                    System.out.println("[ " + keyValue[0] + " ] = " + keyValue[1]);

                                    if (TextUtils.equals(keyValue[0], "base.type")) {
                                        if (TextUtils.equals(keyValue[1], "A")) {
                                            editor.putString("base_type", "base_a");
                                            mBaseType = BaseType.BASE_A;
                                        } else if (TextUtils.equals(keyValue[1], "B")) {
                                            editor.putString("base_type", "base_b");
                                            mBaseType = BaseType.BASE_B;
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

                            mSystemSettingsFetched = true;

                        } else if(s.startsWith("#CameraSettings")) {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = sp.edit();

                            String lines[] = s.split("\\r?\\n");
                            for(int i=0;i<lines.length;i++) {
                                if(lines[i].length() > 0) {
                                    if(lines[i].startsWith("#"))
                                        continue;
                                }
                                //System.out.println(i + " " + lines[i]);
                                String keyValue[] = lines[i].split("=");
                                if (keyValue.length == 2) {

                                    System.out.println("[ " + keyValue[0] + " ] = " + keyValue[1]);

                                    if(TextUtils.equals(keyValue[0], "sensor-id")) {
                                        if(Integer.parseInt(keyValue[1]) == 0)
                                            editor.putString("camera_id", "camera_1");
                                        if(Integer.parseInt(keyValue[1]) == 1)
                                            editor.putString("camera_id", "camera_2");
                                    } else if(TextUtils.equals(keyValue[0], "wbmode")) {
                                        editor.putString("wbmode", keyValue[1]);
                                    } else if(TextUtils.equals(keyValue[0], "tnr-mode")) {
                                        editor.putString("tnr_mode", keyValue[1]);
                                    } else if(TextUtils.equals(keyValue[0], "tnr-strength")) {
                                        //editor.putInt("tnr_strength", Integer.parseInt(keyValue[1]) * 100);
                                        editor.putInt("tnr_strength", Math.round(Float.parseFloat(keyValue[1]) * 10));
                                    } else if(TextUtils.equals(keyValue[0], "ee-mode")) {
                                        editor.putString("ee_mode", keyValue[1]);
                                    } else if(TextUtils.equals(keyValue[0], "ee-strength")) {
                                        //editor.putInt("ee_strength", Integer.parseInt(keyValue[1]) * 100);
                                        editor.putInt("ee_strength", Math.round(Float.parseFloat(keyValue[1]) * 10));
                                    } else if(TextUtils.equals(keyValue[0], "exposurecompensation")) {
                                        //editor.putInt("exposure_compensation", Integer.parseInt(keyValue[1]) * 200);
                                        editor.putInt("exposure_compensation", Math.round(Float.parseFloat(keyValue[1]) * 20));
                                    } else if(TextUtils.equals(keyValue[0], "exposurethreshold")) {
                                        editor.putInt("exposure_threshold", Integer.parseInt(keyValue[1]));
                                    }
                                }
                            }
                            editor.commit();

                            mCameraSettingsFetched = true;
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

    String iv_name[] = {"A", "B"};

    private class lvAdapter extends BaseAdapter {
        LayoutInflater loi;
        public lvAdapter(){
            loi = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            return iv_name.length;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        public void updateView(int index, ListView listview, String ip, String status) {
            System.out.println("Update view with index " + index);
            int visiblePosition = listview.getFirstVisiblePosition();
            System.out.println("First visible position is " + visiblePosition);
            //得到指定位置的視圖，對listview的緩存機制不清楚的可以去瞭解下
            View view = listview.getChildAt(index - visiblePosition);
            //View view = listview.getChildAt(index);
            if(view == null)
                view = listview.getAdapter().getView(index - visiblePosition, null, listview);
            //    return;
            viewHolder vh = (viewHolder) view.getTag();
            if(ip != null) {
                vh.tv_ip = (TextView) view.findViewById(R.id.tv_ip);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vh.tv_ip.setText(ip);
                    }
                });
            }
            if(status != null) {
                vh.tv_status = (TextView) view.findViewById(R.id.tv_status);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vh.tv_status.setText(status);
                    }
                });
            }
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            final viewHolder vh;
            //判斷view是否為空，為空才設置view
            if (view == null) {
                //將自訂的list_contnet灌進view裡
                view = loi.inflate(R.layout.list_content, null);
                //new一個VH，並將iv,tv1,tv2裝進去
                vh = new viewHolder();
                vh.iv_name = (TextView) view.findViewById(R.id.iv_name);
                vh.tv_ip = (TextView) view.findViewById(R.id.tv_ip);
                vh.tv_status = (TextView) view.findViewById(R.id.tv_status);
                //再透過view.setTag把vh裝進去
                view.setTag(vh);
            } else {
                vh = (viewHolder) view.getTag();
            }
            //設定iv,tv1,tv2要放置的內容
            vh.iv_name.setText(iv_name[position]);
            vh.tv_ip.setText("0.0.0.0");
            vh.tv_status.setText("Off line ...");
            return view;
        }
    }

    //宣告類別 viewHolder，記住list_content.xml中的item
    private class viewHolder {
        TextView iv_name;
        TextView tv_ip,tv_status;
    }

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

    Thread multicastReceiverThread = new Thread(new Runnable() { /* Multicast receiver */
        @Override
        public void run() {
            MulticastSocket socket2 = null;
            MulticastSocket socket3 = null;

            try {
                socket2 = new MulticastSocket(9002);
                socket2.joinGroup(InetAddress.getByName("224.0.0.2"));
                socket2.setSoTimeout(2000); /* 2000 ms */

                socket3 = new MulticastSocket(9003);
                socket3.joinGroup(InetAddress.getByName("224.0.0.3"));
                socket3.setSoTimeout(2000); /* 2000 ms */

                DatagramPacket packet;
                while (true) {
                    byte[] buf = new byte[256];
                    packet = new DatagramPacket(buf, buf.length);

                    socket2.receive(packet);
                    if(packet.getLength() == 0) { /* timeout */
                        socket3.receive(packet);
                    }
                    if(packet.getLength() == 0)
                        continue;

                    String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    System.out.println("Multicast receive : " + msg);

                    String baseHost[] = msg.split(":");
                    if (baseHost.length >= 2) {
                        //System.out.println(baseHost[0]);
                        //System.out.println(baseHost[1]);
                        if (TextUtils.equals(baseHost[0], "BASE_A") ||
                                TextUtils.equals(baseHost[0], "BASE_B")) { /* Base found ... */
                            int index = -1;
                            if (TextUtils.equals(baseHost[0], "BASE_A")) {
                                if (!TextUtils.equals(DragonEyeApplication.getInstance().mBaseAddressA, baseHost[1])) {
                                    DragonEyeApplication.getInstance().mBaseAddressA = baseHost[1];
                                    index = 0;
                                }
                            } else if (TextUtils.equals(baseHost[0], "BASE_B")) {
                                if (!TextUtils.equals(DragonEyeApplication.getInstance().mBaseAddressB, baseHost[1])) {
                                    DragonEyeApplication.getInstance().mBaseAddressB = baseHost[1];
                                    index = 1;
                                }
                            }

                            if (index >= 0) { /* Update listview required */
                                mListAdapter.updateView(index, mListView, baseHost[1], null);

                                mSystemSettingsFetched = false;
                                String payloadString = "#SystemSettings";
                                DragonEyeApplication.getInstance().mUdpClient.send(baseHost[1], UDP_REMOTE_PORT, payloadString);

                                mCameraSettingsFetched = false;
                                payloadString = "#CameraSettings";
                                DragonEyeApplication.getInstance().mUdpClient.send(baseHost[1], UDP_REMOTE_PORT, payloadString);

                                payloadString = "#Status";
                                DragonEyeApplication.getInstance().mUdpClient.send(baseHost[1], UDP_REMOTE_PORT, payloadString);
                            }
                        } else if (TextUtils.equals(baseHost[0], "TRIGGER_A")) {
                            int i = Integer.parseInt(baseHost[1]);
                            if (i != serNoA) {
                                if (isRingTonePlaying == false) {
                                    System.out.println("Play tone ...");
                                    isRingTonePlaying = true;
                                    tonePlayer.startPlay();
                                    mTriggerBaseA = true;
                                    mTonePlayerHandler.post(ringTonePlayerThread);
                                    //Toast.makeText(mContext, "F3F Base Trigger", Toast.LENGTH_SHORT).show();
                                }
                                serNoA = i;
                            }
                        } else if (TextUtils.equals(baseHost[0], "TRIGGER_B")) {
                            int i = Integer.parseInt(baseHost[1]);
                            if (i != serNoB) {
                                if (isRingTonePlaying == false) {
                                    System.out.println("Play tone ...");
                                    isRingTonePlaying = true;
                                    tonePlayer.startPlay();
                                    mTriggerBaseA = false;
                                    mTonePlayerHandler.post(ringTonePlayerThread);
                                    //Toast.makeText(mContext, "F3F Base Trigger", Toast.LENGTH_SHORT).show();
                                }
                                serNoB = i;
                            }
                        }
                    }
                }
            } catch(IOException e) {
                System.out.println(e.toString());
            } finally {
                if(socket2 != null) {
                    try {
                        socket2.leaveGroup(InetAddress.getByName("224.0.0.2"));
                        socket2.close();
                    } catch(IOException e) {
                        System.out.println(e.toString());
                    }
                }
                if(socket3 != null) {
                    try {
                        socket3.leaveGroup(InetAddress.getByName("224.0.0.3"));
                        socket3.close();
                    } catch(IOException e) {
                        System.out.println(e.toString());
                    }
                }
            }
        }
    });

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

        mListView = (ListView) findViewById(R.id.lv);
        mListAdapter = new lvAdapter();
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                switch(position) {
                    case 0: DragonEyeApplication.getInstance().mBaseAddress = DragonEyeApplication.getInstance().mBaseAddressA;
                        if(DragonEyeApplication.getInstance().mBaseAddress != null)
                            mStatusView.setText("BASE A on line");
                        else
                            mStatusView.setText("BASE A off line ...");
                        break;
                    case 1: DragonEyeApplication.getInstance().mBaseAddress = DragonEyeApplication.getInstance().mBaseAddressB;
                        if(DragonEyeApplication.getInstance().mBaseAddress != null)
                            mStatusView.setText("BASE B on line");
                        else
                            mStatusView.setText("BASE B off line ...");
                        break;
                }
                if(DragonEyeApplication.getInstance().mBaseAddress != null) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mSystemSettingsFetched = false;
                            String payloadString = "#SystemSettings";
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddress, UDP_REMOTE_PORT, payloadString);
                            mCameraSettingsFetched = false;
                            payloadString = "#CameraSettings";
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddress, UDP_REMOTE_PORT, payloadString);
                            payloadString = "#Status";
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddress, UDP_REMOTE_PORT, payloadString);
                        }
                    });
                    thread.start();
                }
            }
        });

        checkPermission();

        FloatingActionButton bs = findViewById(R.id.button_start);
        bs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(DragonEyeApplication.getInstance().mBaseAddressA == null &&
                        DragonEyeApplication.getInstance().mBaseAddressB == null)
                    return;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String payloadString = "#Start";
                        if(DragonEyeApplication.getInstance().mBaseAddressA != null)
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddressA, UDP_REMOTE_PORT, payloadString);
                        if(DragonEyeApplication.getInstance().mBaseAddressB != null)
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddressB, UDP_REMOTE_PORT, payloadString);
                    }
                });
                thread.start();
            }
        });

        FloatingActionButton bp = findViewById(R.id.button_pause);
        bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(DragonEyeApplication.getInstance().mBaseAddressA == null &&
                        DragonEyeApplication.getInstance().mBaseAddressB == null)
                    return;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String payloadString = "#Stop";
                        if(DragonEyeApplication.getInstance().mBaseAddressA != null)
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddressA, UDP_REMOTE_PORT, payloadString);
                        if(DragonEyeApplication.getInstance().mBaseAddressB != null)
                            DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddressB, UDP_REMOTE_PORT, payloadString);
                    }
                });
                thread.start();
            }
        });

        IntentFilter udpRcvIntentFilter = new IntentFilter("udpMsg");
        registerReceiver(broadcastReceiver, udpRcvIntentFilter);

        @SuppressLint("WifiManagerLeak") final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        mMulticastLock = manager.createMulticastLock("mylock");
        mMulticastLock.setReferenceCounted(true);
        mMulticastLock.acquire();
        final DhcpInfo dhcp = manager.getDhcpInfo();

        System.out.println("IP : " + stringAddress(dhcp.ipAddress));
        System.out.println("Netmask : " + stringAddress(dhcp.netmask));
        System.out.println("Gateway : " + stringAddress(dhcp.gateway));
        System.out.println("DNS 1 : " + stringAddress(dhcp.dns1));
        System.out.println("DNS 2 : " + stringAddress(dhcp.dns2));

        TextView wifi_ssid = (TextView) findViewById(R.id.textview_ssid);
        mStatusView = (TextView) findViewById(R.id.textview_status);

        ConnectionUtil mConnectionMonitor = new ConnectionUtil(this);
        mConnectionMonitor.onInternetStateListener(new ConnectionUtil.ConnectionStateListener() {
            @Override
            public void onWifiConnection(boolean connected) {
                isWifiConnected = connected;
                if (connected) {
                    WifiInfo wifiInfo = manager.getConnectionInfo();
                    if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                        System.out.println("Wifi SSID : " + wifiInfo.getSSID());
                        wifi_ssid.setText(wifiInfo.getSSID());
                    }
                } else {
                    wifi_ssid.setText("Wifi disconnected !!!");
                    mStatusView.setText("Select a Base");
                    DragonEyeApplication.getInstance().mBaseAddress = null;
                    DragonEyeApplication.getInstance().mBaseAddressA = null;
                    DragonEyeApplication.getInstance().mBaseAddressB = null;
                    mSystemSettingsFetched = false;
                    mCameraSettingsFetched = false;
                    mBaseStartedA = false;
                    mBaseStartedB = false;

                    //mListAdapter.updateView(0, mListView, "0.0.0.0", "Off line ...");
                    //mListAdapter.updateView(1, mListView, "0.0.0.0", "Off line ...");
                }
            }
        });

        multicastReceiverThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo.getNetworkId() != -1) {
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    System.out.println("Wifi SSID : " + wifiInfo.getSSID());
                    TextView wifi_ssid = (TextView) findViewById(R.id.textview_ssid);
                    wifi_ssid.setText(wifiInfo.getSSID());
                }

                if(DragonEyeApplication.getInstance().mBaseAddressA == null && DragonEyeApplication.getInstance().mBaseAddressB == null)
                    return;

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(DragonEyeApplication.getInstance().mBaseAddress != null) {
                            if(mSystemSettingsFetched == false) {
                                String payloadString = "#SystemSettings";
                                DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddress, UDP_REMOTE_PORT, payloadString);
                            }

                            if(mCameraSettingsFetched == false) {
                                String payloadString = "#CameraSettings";
                                DragonEyeApplication.getInstance().mUdpClient.send(DragonEyeApplication.getInstance().mBaseAddress, UDP_REMOTE_PORT, payloadString);
                            }
                        }
                    }
                });
                thread.start();

            }
        }
    }

    @Override
    protected void onDestroy() {
        if(mMulticastLock != null) {
            mMulticastLock.release();
            mMulticastLock = null;
        }
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
            if(mTriggerBaseA)
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
                if(DragonEyeApplication.getInstance().mBaseAddress == null)
                    break;
                if(TextUtils.equals(DragonEyeApplication.getInstance().mBaseAddress, DragonEyeApplication.getInstance().mBaseAddressA)) {
                    if (mBaseStartedA == false)
                        break;
                }
                if(TextUtils.equals(DragonEyeApplication.getInstance().mBaseAddress, DragonEyeApplication.getInstance().mBaseAddressB)) {
                    if (mBaseStartedB == false)
                        break;
                }
                Intent intent = new Intent(getApplicationContext(), VideoActivity.class);
                //intent.putExtra(VideoActivity.RTSP_URL, "RTSP://172.16.0.1:8554/test");
                intent.putExtra(VideoActivity.RTSP_URL, "RTSP://" + DragonEyeApplication.getInstance().mBaseAddress + ":8554/test");
                startActivity(intent);
                break;
            case R.id.item_system_setup:
                if(DragonEyeApplication.getInstance().mBaseAddress == null)
                    break;
                if(mSystemSettingsFetched == false) {
                    Toast.makeText(mContext,"Fail to fetch System Settings !!!",Toast.LENGTH_SHORT).show();
                    break;
                }
                intent = new Intent(getApplicationContext(), SystemSettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.item_camera_setup:
                if(DragonEyeApplication.getInstance().mBaseAddress == null)
                    break;
                if(mCameraSettingsFetched == false) {
                    Toast.makeText(mContext,"Fail to fetch Camera Settings !!!",Toast.LENGTH_SHORT).show();
                    break;
                }
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
                builder.setMessage("Access internet Wifi state permissions are required to do the task.");
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
            //Toast.makeText(mContext,"Permissions already granted",Toast.LENGTH_SHORT).show();
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
                    //Toast.makeText(mContext,"Permissions granted.",Toast.LENGTH_SHORT).show();
                }else {
                    // Permissions are denied
                    Toast.makeText(mContext,"Permissions denied.",Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}