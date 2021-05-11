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
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.InetAddresses;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
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
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static final int UDP_REMOTE_PORT = 4999;

    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    private Context mContext;
    private Activity mActivity;

    private static int serNoA = -1, serNoB = -1;
    
    public boolean isWifiConnected = false;

    public ListView mListView;
    public ListViewAdapter mListViewAdapter;

    public TextView mStatusView;

    public MulticastThread mMulticastThread2 = null;
    public MulticastThread mMulticastThread3 = null;

    private boolean isPaused = false;

    private HttpServer mHttpd;

    private void compassCalibrationDialog(DragonEyeBase b) {
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setTitle("Compass Calibration");
        MyAlertDialog.setMessage("Are you sure ?");
        MyAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DragonEyeApplication.getInstance().requestCompassLock(b);
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

    public class ListViewAdapter extends ArrayAdapter<DragonEyeBase> implements View.OnClickListener {

        private ArrayList<DragonEyeBase> mBaseList;
        Context mContext;

        // View lookup cache
        private class ViewHolder {
            TextView txtName;
            TextView txtAddress;
            TextView txtStatus;
            ImageView imgCompassCalibration;
            ImageView imgSystemSettings;
            ImageView imgCameraSettings;
            ImageView imgRtspVideo;
            TextView txtTelemetry;
        }

        ListViewAdapter(ArrayList<DragonEyeBase> baseList, Context context) {
            super(context, R.layout.list_content, baseList);
            mBaseList = baseList;
            mContext = context;
        }

        @Override
        public void onClick(View v) {
            int position=(Integer) v.getTag();
            DragonEyeApplication.getInstance().selectBaseByIndex(position);
            Object object= getItem(position);
            DragonEyeBase b = (DragonEyeBase)object;

            switch (v.getId())
            {
                case R.id.iv_explore: System.out.println("iv_explore OnClick...");
                    if(b.getStatus() == DragonEyeBase.Status.OFFLINE || b.getStatus() == DragonEyeBase.Status.STARTED)
                        break;
                    if(b.isCompassLocked()) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                DragonEyeApplication.getInstance().requestCompassUnlock(b);
                                DragonEyeApplication.getInstance().requestCompassSaveSettings(b);
                            }
                        });
                        thread.start();
                    } else {
                        compassCalibrationDialog(b);
                    }
                    /*
                    Intent i = new Intent(getApplicationContext(), CompassActivity.class);
                    startActivity(i);
                    */
                    break;
                case R.id.iv_system_settings: System.out.println("iv_system_settings OnClick...");
                    if(b.getStatus() == DragonEyeBase.Status.OFFLINE || b.getStatus() == DragonEyeBase.Status.STARTED)
                        break;
                    if(b.getSystemSettings() == null)
                        break;
                    Intent intent = new Intent(getApplicationContext(), SystemSettingsActivity.class);
                    startActivity(intent);
                    break;
                case R.id.iv_camera_settings: System.out.println("iv_camera_settings OnClick...");
                    if(b.getStatus() == DragonEyeBase.Status.OFFLINE || b.getStatus() == DragonEyeBase.Status.STARTED)
                        break;
                    if(b.getCameraSettings() == null)
                        break;
                    intent = new Intent(getApplicationContext(), CameraSettingsActivity.class);
                    startActivity(intent);
                    break;
                case R.id.iv_rtsp_video: System.out.println("iv_rtsp_video OnClick...");
                    if(b.getStatus() != DragonEyeBase.Status.STARTED)
                        break;
                    intent = new Intent(getApplicationContext(), VideoActivity.class);
                    //intent.putExtra(VideoActivity.RTSP_URL, "RTSP://172.16.0.1:8554/test");
                    intent.putExtra(VideoActivity.RTSP_URL, "RTSP://" + b.getAddress() + ":8554/test");
                    startActivity(intent);
                    break;
            }
        }

        private int lastPosition = -1;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            DragonEyeBase base = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            ViewHolder viewHolder; // view lookup cache stored in tag

            final View result;

            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_content, parent, false);
                viewHolder.txtName = (TextView) convertView.findViewById(R.id.iv_name);
                viewHolder.txtAddress = (TextView) convertView.findViewById(R.id.tv_ip);
                viewHolder.txtStatus = (TextView) convertView.findViewById(R.id.tv_status);
                viewHolder.imgCompassCalibration = (ImageView) convertView.findViewById(R.id.iv_explore);
                viewHolder.imgSystemSettings = (ImageView) convertView.findViewById(R.id.iv_system_settings);
                viewHolder.imgCameraSettings = (ImageView) convertView.findViewById(R.id.iv_camera_settings);
                viewHolder.imgRtspVideo = (ImageView) convertView.findViewById(R.id.iv_rtsp_video);
                viewHolder.txtTelemetry = (TextView)  convertView.findViewById(R.id.iv_telemetry);

                result=convertView;

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                result=convertView;
            }

            Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            result.startAnimation(animation);
            lastPosition = position;

            viewHolder.txtName.setText(base.getType().toString());
            viewHolder.txtAddress.setText(base.getAddress());
            viewHolder.txtStatus.setText(base.getStatus().toString());
            switch(base.getStatus()) {
                case OFFLINE: viewHolder.txtStatus.setTextColor(Color.parseColor("#D3D3D3")); /* grey */
                    break;
                case ONLINE: viewHolder.txtStatus.setTextColor(Color.parseColor("#000000"));
                    break;
                case STOPPED: viewHolder.txtStatus.setTextColor(Color.parseColor("#FF0000"));
                    break;
                case STARTED: viewHolder.txtStatus.setTextColor(Color.parseColor("#00FF00"));
                    break;
            }

            viewHolder.imgCompassCalibration.setOnClickListener(this);
            viewHolder.imgCompassCalibration.setTag(position);
            switch(base.getStatus()) {
                case OFFLINE:
                case STARTED: viewHolder.imgCompassCalibration.setColorFilter(Color.parseColor("#D3D3D3")); /* grey */
                    break;
                default:
                    if(base.isCompassLocked())
                        viewHolder.imgCompassCalibration.setColorFilter(Color.parseColor("#00ff00"));
                    else
                        viewHolder.imgCompassCalibration.setColorFilter(Color.parseColor("#000000"));
                    break;
            }

            viewHolder.imgSystemSettings.setOnClickListener(this);
            viewHolder.imgSystemSettings.setTag(position);
            switch(base.getStatus()) {
                case OFFLINE:
                case STARTED: viewHolder.imgSystemSettings.setColorFilter(Color.parseColor("#D3D3D3"));
                    break;
                default: viewHolder.imgSystemSettings.setColorFilter(Color.parseColor("#000000"));
            }

            viewHolder.imgCameraSettings.setOnClickListener(this);
            viewHolder.imgCameraSettings.setTag(position);
            switch(base.getStatus()) {
                case OFFLINE:
                case STARTED: viewHolder.imgCameraSettings.setColorFilter(Color.parseColor("#D3D3D3"));
                    break;
                default: viewHolder.imgCameraSettings.setColorFilter(Color.parseColor("#000000"));
            }

            viewHolder.imgRtspVideo.setOnClickListener(this);
            viewHolder.imgRtspVideo.setTag(position);
            switch(base.getStatus()) {
                case STARTED: viewHolder.imgRtspVideo.setColorFilter(Color.parseColor("#000000"));
                    break;
                default: viewHolder.imgRtspVideo.setColorFilter(Color.parseColor("#D3D3D3"));
            }

            String s = "Yaw " + base.yaw() + "\u00b0 / " + "Temp " + (float)base.temperature() / 1000.0 + "\u2103" + " / GPU " + base.gpuLoad() / 10 + "%";
            viewHolder.txtTelemetry.setText(s);

            // Return the completed view to render on screen
            return convertView;
        }
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

    public static byte byteOfInt(int value, int which) {
        int shift = which * 8;
        return (byte)(value >> shift);
    }

    public static InetAddress intToInet(int value) {
        byte[] bytes = new byte[4];
        for(int i = 0; i<4; i++) {
            bytes[i] = byteOfInt(value, i);
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            // This only happens if the byte array has a bad length
            return null;
        }
    }

    public class MulticastThread implements Runnable {
        private Thread worker;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private int localAddr = 0;
        private int networkId = -1;

        private String mAddress = null;
        private int mPort = 0;

        public MulticastThread(String address, int port) {
            super();
            mAddress = new String(address);
            mPort = port;
        }

        public boolean isRunning() {
            return running.get();
        }

        public void interrupt() {
            running.set(false);
            worker.interrupt();
        }

        public void start() {
            worker = new Thread(this);
            worker.start();
        }

        public void stop() {
            running.set(false);
        }

        @Override
        public void run() {
            running.set(true);

            System.out.println("multicastThread - Started ...");

            MulticastSocket socket = null;
            InetAddress group = null;
            try {
                group = InetAddress.getByName(mAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            WifiManager.MulticastLock lock = wifiManager.createMulticastLock("mylock");
            lock.setReferenceCounted(true);
            lock.acquire();

            DatagramPacket packet;
            while (running.get()) {
                if (wifiManager.isWifiEnabled() == false)
                    continue;

                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getNetworkId() == -1)
                    continue;

                if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED)
                    continue;

                int addr = wifiManager.getDhcpInfo().ipAddress;
                if (localAddr != addr || networkId != wifiInfo.getNetworkId()) {
                    localAddr = addr;
                    networkId = wifiInfo.getNetworkId();

                    try {
                        InetAddress localInetAddress = intToInet(addr);

                        NetworkInterface ni = NetworkInterface.getByInetAddress(localInetAddress);
                        if(ni != null)
                            System.out.println("multicastThread - Wifi interface : " + ni.getDisplayName() + ", address :" + localInetAddress);
                        else
                            System.out.println("multicastThread - NetworkInterface.getByInetAddress " + localInetAddress + " fail !!!");
/*
                        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                        while (nis.hasMoreElements())
                            System.out.println(nis.nextElement());
*/
                        System.out.println("multicastThread - Wifi SSID : " + wifiInfo.getSSID());
                        System.out.println("multicastThread - Bind address " + localInetAddress);

                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }

                        socket = new MulticastSocket(mPort);
                        socket.setReuseAddress(true);
                        //socket.bind(isa);
                        if(ni != null)
                            socket.setNetworkInterface(ni);
                        socket.joinGroup(group);
                        //socket.setSoTimeout(2000);
                    } catch (UnknownHostException | SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(socket == null)
                    continue;

                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.setSoTimeout(2000);
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    //e.printStackTrace();
                    System.out.println("multicastThread - RX timeout");
/*
                    Intent intent = new Intent();
                    intent.setAction("mcastMsg");
                    intent.putExtra("mcastPollTimeout", 0);
                    mContext.sendBroadcast(intent);
*/
                    continue;
                } catch (SocketException e) {
                    e.printStackTrace();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                if (packet.getLength() == 0)
                    continue;

                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                System.out.println("Multicast receive : " + msg);

                String baseHost[] = msg.split(":");
                if (baseHost.length >= 2) {
                    DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(baseHost[1]);
                    //System.out.println(baseHost[0]);
                    //System.out.println(baseHost[1]);
                    if (TextUtils.equals(baseHost[0], "BASE_A") ||
                            TextUtils.equals(baseHost[0], "BASE_B")) {
                        try {
                            InetAddress.getByName(baseHost[1]); // Test if valid ip address
                        } catch (UnknownHostException e) {
                            Log.i("UDPClient", "Unknown host : " + baseHost[1]);
                            e.printStackTrace();
                            continue;
                        }

                        if(TextUtils.equals(baseHost[1], "127.0.0.1"))
                            continue;

                        if(b == null) { /* New base */
                            b = new DragonEyeBase(mContext, baseHost[0], baseHost[1]); /* Type, Address */
                            b.multicastReceived();
                            b.online();
                            DragonEyeApplication.getInstance().addBase(b);
                            mListView.deferNotifyDataSetChanged();

                            DragonEyeApplication.getInstance().requestSystemSettings(b);
                            DragonEyeApplication.getInstance().requestCameraSettings(b);
                            DragonEyeApplication.getInstance().requestStatus(b);
                        } else { /* Base exist ... */
                            b.multicastReceived();
                            if(b.getStatus() == DragonEyeBase.Status.OFFLINE) {
                                b.online();
                                DragonEyeApplication.getInstance().requestSystemSettings(b);
                                DragonEyeApplication.getInstance().requestCameraSettings(b);
                                DragonEyeApplication.getInstance().requestStatus(b);
                            }
                        }

                        if(baseHost.length >= 5) {
                            b.setYaw(Integer.parseInt(baseHost[2]));
                            b.setTemperature(Integer.parseInt(baseHost[3]));
                            b.setGpuLoad(Integer.parseInt(baseHost[4]));

                            Intent intent = new Intent();
                            intent.setAction("udpMsg");
                            intent.putExtra("udpRcvMsg", b.getAddress() + ":#Telemetry");
                            mContext.sendBroadcast(intent);
                        }
                    } else if (TextUtils.equals(baseHost[0], "TRIGGER_A")) {
                        int i = Integer.parseInt(baseHost[1]);
                        if (i != serNoA) {
                            if(!isPaused) {
                                System.out.println("Play tone ...");
                                DragonEyeApplication.getInstance().playTone(R.raw.r_a);
                                serNoA = i;
                            }
                        }
                    } else if (TextUtils.equals(baseHost[0], "TRIGGER_B")) {
                        int i = Integer.parseInt(baseHost[1]);
                        if (i != serNoB) {
                            if(!isPaused) {
                                System.out.println("Play tone ...");
                                DragonEyeApplication.getInstance().playTone(R.raw.r_b);
                                serNoB = i;
                            }
                        }
                    }

                    Intent intent = new Intent();
                    intent.setAction("mcastMsg");
                    intent.putExtra("mcastRcvMsg", msg);
                    mContext.sendBroadcast(intent);
                }
            }

            if(socket != null) {
                try {
                    socket.leaveGroup(group);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.close();
                socket = null;
            }

            localAddr = 0;
            networkId = -1;

            System.out.println("multicastThread - Stopped ...");

            lock.release();

            running.set(false);
        }
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

        DragonEyeApplication.getInstance().mTonePlayer = new TonePlayer(mContext);

        mListView = (ListView) findViewById(R.id.lv);
        mListViewAdapter = new ListViewAdapter(DragonEyeApplication.getInstance().mBaseList, getApplicationContext());
        mListView.setAdapter(mListViewAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                DragonEyeApplication.getInstance().selectBaseByIndex(position);
            }
        });

        checkPermission();

        FloatingActionButton bs = findViewById(R.id.button_start);
        bs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(DragonEyeApplication.getInstance().mBaseList.isEmpty())
                    return;

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
                            DragonEyeApplication.getInstance().requestStart(b);
                        }
                    }
                });
                thread.start();
            }
        });

        FloatingActionButton bp = findViewById(R.id.button_pause);
        bp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(DragonEyeApplication.getInstance().mBaseList.isEmpty())
                    return;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
                            DragonEyeApplication.getInstance().requestStop(b);
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

        /*
        @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final DhcpInfo dhcp = wifiManager.getDhcpInfo();

        System.out.println("IP : " + stringAddress(dhcp.ipAddress));
        System.out.println("Netmask : " + stringAddress(dhcp.netmask));
        System.out.println("Gateway : " + stringAddress(dhcp.gateway));
        System.out.println("DNS 1 : " + stringAddress(dhcp.dns1));
        System.out.println("DNS 2 : " + stringAddress(dhcp.dns2));
*/
        TextView wifi_ssid = (TextView) findViewById(R.id.textview_ssid);
        mStatusView = (TextView) findViewById(R.id.textview_status);

        ConnectionUtil mConnectionMonitor = new ConnectionUtil(this);
        mConnectionMonitor.onInternetStateListener(new ConnectionUtil.ConnectionStateListener() {
            @Override
            public void onWifiConnection(boolean connected) {
                System.out.println("onWifiConnection ...");

                isWifiConnected = connected;
                if (connected) {
                    System.out.println("On Wifi Connected ...");
                    @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                        System.out.println("Wifi SSID : " + wifiInfo.getSSID());

                        String ssid = wifiInfo.getSSID().replace("\"", "");
                        if(!TextUtils.equals(wifi_ssid.getText(), ssid)) /* Connection changed */
                            onWifiConnectionChanged();
                        wifi_ssid.setText(ssid);
                        mStatusView.setText("Searching ...");

                        if(mMulticastThread2.isRunning() == false)
                            mMulticastThread2.start();
                        if(mMulticastThread3.isRunning() == false)
                            mMulticastThread3.start();
                        if(DragonEyeApplication.getInstance().mUdpClient.isRunning() == false)
                            DragonEyeApplication.getInstance().mUdpClient.start();
                    }
                } else {
                    System.out.println("On Wifi Disconnected ...");
                    wifi_ssid.setText("Wifi disconnected !!!");
                    mStatusView.setText("Standby ...");

                    onWifiConnectionChanged();

                    if(DragonEyeApplication.getInstance().mUdpClient.isRunning())
                        DragonEyeApplication.getInstance().mUdpClient.stop();
                    if(mMulticastThread2.isRunning())
                        mMulticastThread2.stop();
                    if(mMulticastThread3.isRunning())
                        mMulticastThread3.stop();
                }
            }

            private void onWifiConnectionChanged() {
                for(DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
                    b.destroy();
                }
                DragonEyeApplication.getInstance().mBaseList.clear();
                mListViewAdapter.notifyDataSetChanged();
            }
        });

        mMulticastThread2 = new MulticastThread("224.0.0.2", 9002);
        mMulticastThread2.start();

        mMulticastThread3 = new MulticastThread("224.0.0.3", 9003);
        mMulticastThread3.start();
/*
        DragonEyeApplication.getInstance().addBase(new DragonEyeBase("BASE_A", "10.0.0.1"));
        DragonEyeApplication.getInstance().addBase(new DragonEyeBase("BASE_B", "172.16.0.1"));
        DragonEyeApplication.getInstance().addBase(new DragonEyeBase("BASE_B", "192.168.0.1"));
        DragonEyeApplication.getInstance().addBase(new DragonEyeBase("BASE_A", "192.168.168.1"));
*/
        mHttpd = new HttpServer(8080);
        mHttpd.setOnStatusUpdateListener(new HttpServer.OnStatusUpdateListener() {
            @Override
            public void onUploadingProgressUpdate(final int progress) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mProgressBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void onUploadingFile(final File file, final boolean done) {
                MainActivity.this.runOnUiThread(new Runnable() {
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
            public void onDownloadingFile(final File file, final boolean done) {
                MainActivity.this.runOnUiThread(new Runnable() {
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        isPaused = false;

        @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo.getNetworkId() != -1) {
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    System.out.println("Wifi SSID : " + wifiInfo.getSSID());
                    TextView wifi_ssid = (TextView) findViewById(R.id.textview_ssid);
                    wifi_ssid.setText(wifiInfo.getSSID().replace("\"", ""));

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
                                DragonEyeApplication.getInstance().requestStatus(b);
                            }
                        }
                    });
                    thread.start();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        isPaused = true;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        mHttpd.stop();
        super.onDestroy();
    }

    private void onUdpRx(String str) {
        System.out.println("UDP RX : " + str);
        int index = str.indexOf(':');
        String addr = str.substring(0, index);
        String s = str.substring(index+1);
        DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(addr);
        if(b != null) {
            if(TextUtils.equals(s, "#Started")) {
                b.stopResponseTimer();
                if(b.getStatus() != DragonEyeBase.Status.STARTED) {
                    b.started();
                    mListViewAdapter.notifyDataSetChanged();
                }
            } else if(TextUtils.equals(s, "#Stopped")) {
                b.stopResponseTimer();
                if(b.getStatus() != DragonEyeBase.Status.STOPPED) {
                    b.stopped();
                    mListViewAdapter.notifyDataSetChanged();
                }
            } else if(s != null) {
                if (s.startsWith("#SystemSettings")) {
                    b.stopResponseTimer();
                    String lines[] = s.split("\\r?\\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].length() > 0) {
                            if (lines[i].startsWith("#"))
                                continue;
                        }
                        //System.out.println(i + " " + lines[i]);
                        String keyValue[] = lines[i].split("=");
                        if (keyValue.length == 2) {
                            //System.out.println("[ " + keyValue[0] + " ] = " + keyValue[1]);
                            if (TextUtils.equals(keyValue[0], "base.type")) {
                                if (TextUtils.equals(keyValue[1], "A")) {
                                    b.setTypeBaseA();
                                } else if (TextUtils.equals(keyValue[1], "B")) {
                                    b.setTypeBaseB();
                                }
                            }
                        }
                    }
                    b.setSystemSettings(s);
                } else if (s.startsWith("#CameraSettings")) {
                    b.stopResponseTimer();
                    b.setCameraSettings(s);
                } else if(TextUtils.equals(s, "#CompassLock")) {
                    b.stopResponseTimer();
                    b.compassLock();
                    mListViewAdapter.notifyDataSetChanged();
                } else if(TextUtils.equals(s, "#CompassUnlock")) {
                    b.stopResponseTimer();
                    b.compassUnlock();
                    mListViewAdapter.notifyDataSetChanged();
                } else if(TextUtils.equals(s, "#Telemetry")) {
                    b.stopResponseTimer();
                    mListViewAdapter.notifyDataSetChanged();
                } else if(TextUtils.equals(s, "#Ack")) {
                    b.stopResponseTimer();
                }
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
                    mListViewAdapter.notifyDataSetChanged();
                } else if(intent.hasExtra("baseStatusUpdate")) { // base status changed
                    mListViewAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onStop() {
        DragonEyeApplication.getInstance().mTonePlayer.stopPlay();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void AboutWindow()
    {
        AlertDialog.Builder aboutWindow = new AlertDialog.Builder(this);//creates a new instance of a dialog box
        final String website = "\t https://stevegigijoe.blogspot.com";
        final String AboutDialogMessage = "\t dragon-eye-rc v0.1.3\n\t All bugs made by Steve Chang \n\t Website for contact :\n";
        final TextView tx = new TextView(this);//we create a textview to store the dialog text/contents
        tx.setText(AboutDialogMessage + website);//we set the text/contents
        tx.setAutoLinkMask(RESULT_OK);//to linkify any website or email links
        tx.setTextColor(Color.BLACK);//setting the text color
        tx.setTextSize(15);//setting the text size
        //again to enable any website urls or email addresses to be clickable links
        tx.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(tx, Linkify.WEB_URLS);

        aboutWindow.setTitle("About");//set the title of the about box to say "About"
        aboutWindow.setView(tx);//set the textview on the dialog box

        aboutWindow.setPositiveButton("OK", new DialogInterface.OnClickListener()//creates the OK button of the dialog box
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();//when the OK button is clicked, dismiss() is called to close it
            }
        });
        aboutWindow.show();//this method call will bring up the dialog box when the user calls the AboutDialog() method
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.item_f3f_timer:
                Intent intent = new Intent(getApplicationContext(), TimerActivity.class);
                startActivity(intent);
                break;
            case R.id.item_about: AboutWindow();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void checkPermission(){
        if(ContextCompat.checkSelfPermission(mActivity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                ||  ContextCompat.checkSelfPermission(mActivity,Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                ||  ContextCompat.checkSelfPermission(mActivity,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ||  ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||  ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Do something, when permissions not granted
            if(ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.INTERNET)
                    || ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.ACCESS_WIFI_STATE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
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
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
                                        + grantResults[3]
                                        + grantResults[4]
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