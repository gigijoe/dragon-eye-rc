package com.gtek.dragon_eye_rc;

import static com.gtek.dragon_eye_rc.DragonEyeBase.Status.STARTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
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
import android.os.PowerManager;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.videolan.BuildConfig;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    private Context mContext;
    private Activity mActivity;

    private static int serNoA = 0, serNoB = 0;
    private static int triggerLossA = 0;
    private static int triggerLossB = 0;

    public ListView mListView;
    public ListViewAdapter mListViewAdapter;

    public TextView mWifiSsid;
    public TextView mStatusView;

    public MulticastThread mMulticastThread2 = null;
    public MulticastThread mMulticastThread3 = null;

    private WifiManager.WifiLock highPerfWifiLock;
    private WifiManager.WifiLock lowLatencyWifiLock;

    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private UsbSerialPort mUsbSerialPort = null;
    private UsbSerialThread mUsbSerialThread = null;

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
            //ImageView imgCompassCalibration;
            ImageView imgRun;
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
/*
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
                    break;
 */
                case R.id.iv_run:
                    if(b.getStatus() == DragonEyeBase.Status.STOPPED) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                DragonEyeApplication.getInstance().requestStart(b);
                                b.trying();
                            }
                        });
                        thread.start();

                    } else if(b.getStatus() == STARTED) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                DragonEyeApplication.getInstance().requestStop(b);
                                b.trying();
                            }
                        });
                        thread.start();
                    }
                    break;
                case R.id.iv_system_settings: //System.out.println("iv_system_settings OnClick...");
                    if(b.getStatus() == DragonEyeBase.Status.OFFLINE || b.getStatus() == STARTED)
                        break;
                    if(b.getSystemSettings() == null)
                        break;
                    Intent intent = new Intent(getApplicationContext(), SystemSettingsActivity.class);
                    startActivity(intent);
                    break;
                case R.id.iv_camera_settings: //System.out.println("iv_camera_settings OnClick...");
                    if(b.getStatus() == DragonEyeBase.Status.OFFLINE || b.getStatus() == STARTED)
                        break;
                    if(b.getCameraSettings() == null)
                        break;
                    intent = new Intent(getApplicationContext(), CameraSettingsActivity.class);
                    startActivity(intent);
                    break;
                case R.id.iv_rtsp_video: //System.out.println("iv_rtsp_video OnClick...");
                    if(b.getStatus() != STARTED)
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
            DragonEyeBase b = getItem(position);
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
                //viewHolder.imgCompassCalibration = (ImageView) convertView.findViewById(R.id.iv_explore);
                viewHolder.imgRun = (ImageView) convertView.findViewById(R.id.iv_run);
                viewHolder.imgSystemSettings = (ImageView) convertView.findViewById(R.id.iv_system_settings);
                viewHolder.imgCameraSettings = (ImageView) convertView.findViewById(R.id.iv_camera_settings);
                viewHolder.imgRtspVideo = (ImageView) convertView.findViewById(R.id.iv_rtsp_video);
                viewHolder.txtTelemetry = (TextView)  convertView.findViewById(R.id.iv_telemetry);

                result=convertView;

                convertView.setTag(viewHolder);

                Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
                result.startAnimation(animation);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                result=convertView;
            }

            lastPosition = position;

            viewHolder.txtName.setText(b.getType().toString());
            if(b.isBaseTrigger()) {
                viewHolder.txtName.setTextColor(Color.parseColor("#FF0000"));
            } else {
                viewHolder.txtName.setTextColor(Color.parseColor("#000000"));
            }
            viewHolder.txtAddress.setText(b.getAddress());
            viewHolder.txtStatus.setText(b.getStatus().toString());
            switch(b.getStatus()) {
                case OFFLINE: viewHolder.txtStatus.setTextColor(Color.parseColor("#D3D3D3")); // grey
                    break;
                case ONLINE: viewHolder.txtStatus.setTextColor(Color.parseColor("#000000")); // black
                    break;
                case STOPPED: viewHolder.txtStatus.setTextColor(Color.parseColor("#FF0000")); // red
                    break;
                case STARTED: viewHolder.txtStatus.setTextColor(Color.parseColor("#00FF00")); // green
                    break;
                case TRYING: viewHolder.txtStatus.setTextColor(Color.parseColor("#0000FF")); // blue
                    break;
                case ERROR: viewHolder.txtStatus.setTextColor(Color.parseColor("#FF0000")); // red
                    break;
            }
/*
            viewHolder.imgCompassCalibration.setOnClickListener(this);
            viewHolder.imgCompassCalibration.setTag(position);
            switch(b.getStatus()) {
                case OFFLINE:
                case STARTED: viewHolder.imgCompassCalibration.setColorFilter(Color.parseColor("#D3D3D3")); // grey
                    break;
                default:
                    if(b.isCompassLocked())
                        viewHolder.imgCompassCalibration.setColorFilter(Color.parseColor("#00ff00"));
                    else
                        viewHolder.imgCompassCalibration.setColorFilter(Color.parseColor("#000000"));
                    break;
            }
*/
            viewHolder.imgRun.setOnClickListener(this);
            viewHolder.imgRun.setTag(position);
            switch(b.getStatus()) {
                case STARTED: //viewHolder.imgRun.setColorFilter(Color.parseColor("#D3D3D3"));
                    viewHolder.imgRun.setColorFilter(Color.parseColor("#FF0000"));
                    //viewHolder.imgRun.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_pause_circle_outline_48));
                    viewHolder.imgRun.setImageResource(R.drawable.ic_baseline_pause_circle_outline_48);
                    break;
                default: viewHolder.imgRun.setColorFilter(Color.parseColor("#00FF00"));
                    //viewHolder.imgRun.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_play_circle_outline_48));
                    viewHolder.imgRun.setImageResource(R.drawable.ic_baseline_play_circle_outline_48);
            }

            viewHolder.imgSystemSettings.setOnClickListener(this);
            viewHolder.imgSystemSettings.setTag(position);
            switch(b.getStatus()) {
                case OFFLINE:
                case STARTED: viewHolder.imgSystemSettings.setColorFilter(Color.parseColor("#D3D3D3"));
                    break;
                default: viewHolder.imgSystemSettings.setColorFilter(Color.parseColor("#000000"));
            }

            viewHolder.imgCameraSettings.setOnClickListener(this);
            viewHolder.imgCameraSettings.setTag(position);
            switch(b.getStatus()) {
                case OFFLINE:
                case STARTED: viewHolder.imgCameraSettings.setColorFilter(Color.parseColor("#D3D3D3"));
                    break;
                default: viewHolder.imgCameraSettings.setColorFilter(Color.parseColor("#000000"));
            }

            viewHolder.imgRtspVideo.setOnClickListener(this);
            viewHolder.imgRtspVideo.setTag(position);
            switch(b.getStatus()) {
                case STARTED: viewHolder.imgRtspVideo.setColorFilter(Color.parseColor("#000000"));
                    break;
                default: viewHolder.imgRtspVideo.setColorFilter(Color.parseColor("#D3D3D3"));
            }

            int serNo = 0;

            if(b.getType() == DragonEyeBase.Type.BASE_A)
                serNo = serNoA;
            else if(b.getType() == DragonEyeBase.Type.BASE_B)
                serNo = serNoB;

            //String s = "Yaw " + b.yaw() + "\u00b0 / " + "Temp " + (float)b.temperature() / 1000.0 + "\u2103" + " / GPU " + b.gpuLoad() / 10 + "% / FPS " + b.fps();
            String s = "FPS " + b.fps() + " | " + "Temp " + (float)b.temperature() / 1000.0 + "\u2103" +
                    " | GPU " + b.gpuLoad() / 10 + "% | " + b.triggerLoss() + "/" + serNo;
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

    private static byte byteOfInt(int value, int which) {
        int shift = which * 8;
        return (byte)(value >> shift);
    }

    private static InetAddress intToInet(int value) {
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
        private MulticastSocket socket = null;
        private InetAddress group = null;

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
            worker.setPriority(Thread.MAX_PRIORITY);
            worker.start();
        }

        public void stop() {
            if(socket != null) {
                socket.close(); /* Trigger exception */
            }
            running.set(false);
            try {
                worker.join();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void restart() {
            stop();
            start();
        }

        @Override
        public void run() {
            running.set(true);

            System.out.println("multicastThread - Started ...");

            try {
                group = InetAddress.getByName(mAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            @SuppressLint("WifiManagerLeak") final WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            WifiManager.MulticastLock lock = wifiMgr.createMulticastLock("mylock");
            lock.setReferenceCounted(true);
            lock.acquire();

            byte[] buf = new byte[4096];
            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length);

            while (running.get()) {
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                if (wifiMgr.isWifiEnabled() == false) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (wifiInfo.getNetworkId() == -1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("multicastThread - Supplicant NOT Complete !!!");
                    continue;
                }

                int addr = wifiMgr.getDhcpInfo().ipAddress;
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
                        System.out.println("multicastThread - Socket address " + localInetAddress);

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

                if(socket == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("multicastThread - Null socket");
                    continue;
                }

                try {
                    //socket.setSoTimeout(100);
                    socket.receive(packet);
                    String s = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    System.out.println("Multicast receive : " + s);
/*
                    PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    if(!pm.isInteractive())
                        continue;
*/
                    Activity a = DragonEyeApplication.getInstance().getActivity();
                    if (s.charAt(0) == '<' && s.charAt(s.length() - 1) == '>') {
                        DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(packet.getAddress().getHostAddress());
                        if (a != null && b != null) {
                            //if (a != null) { /* Not necessary a dragon-eye base */
                            if (TextUtils.equals(a.getClass().getSimpleName(), "MainActivity")) {
                                //MainActivity.this.onBaseTrigger(b, s);
                                ((MainActivity) a).onBaseTrigger(b, s);
                            } else if (TextUtils.equals(a.getClass().getSimpleName(), "TimerActivity")) {
                                ((TimerActivity) a).onBaseTrigger(b, s);
                            }
                        }
                    } else {
                        MainActivity.this.onMulticastRx(packet.getAddress().getHostAddress(), s);
                    }
                } catch (SocketTimeoutException e) {
                    //e.printStackTrace();
                    //System.out.println("multicastThread - RX timeout");
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
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

    private void updateWifiStatus() {
    }
/*
    BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (checkWifiConnect()) {
                //Log.d(TAG, "wifi has connected");
                // TODO
            }
        }

        private boolean checkWifiConnect() {
            ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null
                    && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected()) {
                return true;
            }
            return false;
        }
    };
*/

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        mActivity = MainActivity.this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean hasLowLatencyFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        System.out.println("android.hardware.audio.low_latenc = " + hasLowLatencyFeature);

        boolean hasProFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);

        System.out.println("android.hardware.audio.pro = " + hasProFeature);

        //DragonEyeApplication.getInstance().mTonePlayer = new TonePlayer(mContext);

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
/*
        FloatingActionButton bs = findViewById(R.id.button_start);
        bs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DragonEyeApplication.getInstance().mBaseList.isEmpty())
                    return;

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
                            DragonEyeApplication.getInstance().requestStart(b);
                            b.trying();

                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mListViewAdapter.notifyDataSetChanged();
                                }
                            });
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
                if (DragonEyeApplication.getInstance().mBaseList.isEmpty())
                    return;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
                            DragonEyeApplication.getInstance().requestStop(b);
                            b.trying();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mListViewAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                });
                thread.start();
            }
        });
*/
/*
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
        //registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
*/
        registerReceiver(broadcastReceiver, new IntentFilter("udpMsg"));
        registerReceiver(broadcastReceiver, new IntentFilter("baseMsg"));
        registerReceiver(broadcastReceiver, new IntentFilter("wifiMsg"));

        registerReceiver(usbBroadcastReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(usbBroadcastReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        registerReceiver(usbBroadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

        mWifiSsid = (TextView) findViewById(R.id.textview_ssid);
        mStatusView = (TextView) findViewById(R.id.textview_status);

        // Make sure Wi-Fi is fully powered up
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        highPerfWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "High Perf Lock");
        highPerfWifiLock.setReferenceCounted(false);
        highPerfWifiLock.acquire();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            lowLatencyWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Low Latency Lock");
            lowLatencyWifiLock.setReferenceCounted(false);
            lowLatencyWifiLock.acquire();
        }

        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        req.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET); // No internet

        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull final Network network) {
                super.onAvailable(network);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.bindProcessToNetwork(network);
                }
                //connectivityManager.unregisterNetworkCallback(this);

                @SuppressLint("WifiManagerLeak") final WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                //System.out.println("wifiInfo.getNetworkId() = " + wifiInfo.getNetworkId());
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    System.out.println("Wifi SSID : " + wifiInfo.getSSID());
                    String ssid = wifiInfo.getSSID().replace("\"", "");

                    Intent intent = new Intent(); /* Broadcast to as UDP message ... */
                    intent.setAction("wifiMsg");
                    intent.putExtra("wifiConnected", ssid);
                    mContext.sendBroadcast(intent);
                }
            }

            @Override
            public void onLost(Network network) {
                System.out.println("Wifi Disconnected ...");

                Intent intent = new Intent(); /* Broadcast to as UDP message ... */
                intent.setAction("wifiMsg");
                intent.putExtra("wifiDisconnected", "");
                mContext.sendBroadcast(intent);
            }
        });

        mMulticastThread2 = new MulticastThread("224.0.0.2", 9002);
        mMulticastThread3 = new MulticastThread("224.0.0.3", 9003);

        mUsbSerialThread = new UsbSerialThread();

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (!availableDrivers.isEmpty()) {
            try {
                ConnectUsbDevice(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearAllDragonEyeBase() {
        for(DragonEyeBase b : DragonEyeApplication.getInstance().mBaseList) {
            b.destroy();
        }
        DragonEyeApplication.getInstance().mBaseList.clear();
        mListViewAdapter.notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    private void onWifiConnected(String ssid) {
        if (TextUtils.equals(mWifiSsid.getText(), ssid)) { // Connection changed
            //clearAllDragonEyeBase();

            if(DragonEyeApplication.getInstance().mUdpClient.isRunning())
                DragonEyeApplication.getInstance().mUdpClient.stop();
            if(mMulticastThread2.isRunning())
                mMulticastThread2.stop();
            if(mMulticastThread3.isRunning())
                mMulticastThread3.stop();
        }

        @SuppressLint("WifiManagerLeak") final WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final DhcpInfo dhcp = wifiMgr.getDhcpInfo();

        System.out.println("IP : " + stringAddress(dhcp.ipAddress));
        //System.out.println("Netmask : " + stringAddress(dhcp.netmask));
        System.out.println("Gateway : " + stringAddress(dhcp.gateway));
        System.out.println("DNS 1 : " + stringAddress(dhcp.dns1));
        //System.out.println("DNS 2 : " + stringAddress(dhcp.dns2));

        mWifiSsid.setText(ssid);
        //mStatusView.setText("Base Scanning ...");

        if(!mMulticastThread2.isRunning())
            mMulticastThread2.start();
        if(!mMulticastThread3.isRunning())
            mMulticastThread3.start();
        if(!DragonEyeApplication.getInstance().mUdpClient.isRunning())
            DragonEyeApplication.getInstance().mUdpClient.start();
    }

    @SuppressLint("SetTextI18n")
    private void onWifiDisconnected() {
        mWifiSsid.setText("WIFI Disconnected !!!");
        //mStatusView.setText("All bases off line ...");

        //clearAllDragonEyeBase();

        if(DragonEyeApplication.getInstance().mUdpClient.isRunning())
            DragonEyeApplication.getInstance().mUdpClient.stop();
        if(mMulticastThread2.isRunning())
            mMulticastThread2.stop();
        if(mMulticastThread3.isRunning())
            mMulticastThread3.stop();
    }

    @Override
    protected void onStart() {
        System.out.println("onStart()");
        super.onStart();
        isPaused.set(false);
    }

    @Override
    protected void onPause() {
        System.out.println("onPause()");
        super.onPause();
        isPaused.set(true);
    }

    @Override
    protected void onPostResume() {
        System.out.println("onPostResume()");
        super.onPostResume();
        isPaused.set(false);

        if(mUsbSerialPort != null && mUsbSerialPort.isOpen()) {
            if (!mUsbSerialThread.isRunning())
                mUsbSerialThread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(DragonEyeApplication.getInstance().mUdpClient.isRunning())
            DragonEyeApplication.getInstance().mUdpClient.stop();
        if(mMulticastThread2.isRunning())
            mMulticastThread2.stop();
        if(mMulticastThread3.isRunning())
            mMulticastThread3.stop();
        if (mUsbSerialThread.isRunning())
            mUsbSerialThread.stop();

        unregisterReceiver(usbBroadcastReceiver);
        unregisterReceiver(broadcastReceiver);

        if (lowLatencyWifiLock != null) {
            lowLatencyWifiLock.release();
        }
        if (highPerfWifiLock != null) {
            highPerfWifiLock.release();
        }
    }

    synchronized void onBaseTrigger(DragonEyeBase b, String s) {
        char base = s.charAt(1);
        int serNo = Integer.parseInt(s.substring(2, s.length() - 1));
        if((base == 'A' && serNo != serNoA)) {
            if (!isPaused.get()) {
                System.out.println("Play tone ...");
                DragonEyeApplication.getInstance().playTone("smb_jump_small.raw"); // R.raw.r_a
            }
            if(b == null) {
                DragonEyeApplication.getInstance().triggerBase(DragonEyeBase.Type.BASE_A);
            } else {
                if (!b.isBaseTrigger()) {
                    b.baseTrigger(); // Color RED of base latter A or B only
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListViewAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
            if(serNoA != 0 && serNo - serNoA > 1) {
                System.out.println("Packet loss : " + serNoA + " - " + serNo);
                triggerLossA += (serNo - serNoA);
                b.setTriggerLoss(triggerLossA);
            }
            serNoA = serNo;
        } else if((base == 'B' && serNo != serNoB)) {
            if (!isPaused.get()) {
                System.out.println("Play tone ...");
                DragonEyeApplication.getInstance().playTone("smb_jump_super.raw"); // R.raw.r_b
            }
            if(b == null) {
                DragonEyeApplication.getInstance().triggerBase(DragonEyeBase.Type.BASE_B);
            } else {
                if (!b.isBaseTrigger()) {
                    b.baseTrigger(); // Color RED of base latter A or B only
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListViewAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
            if(serNoB != 0 && serNo - serNoB > 1) {
                System.out.println("Packet loss : " + serNoB + " - " + serNo);
                triggerLossB += (serNo - serNoB);
                b.setTriggerLoss(triggerLossB);
            }
            serNoB = serNo;
        }
    }

    private void onUdpRx(String str) {
        System.out.println("UDP RX : " + str);
        int index = str.indexOf(':');
        String addr = str.substring(0, index); // Address insert front by UDPClient
        String s = str.substring(index+1);
        if(TextUtils.isEmpty(addr) || TextUtils.isEmpty(s))
            return;
        DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(addr);
        if(b != null) {
            if(TextUtils.equals(s, "#Started")) {
                b.stopResponseTimer();
                if(b.getStatus() != STARTED) {
                    b.started();
                    mListViewAdapter.notifyDataSetChanged();
                }
            } else if(TextUtils.equals(s, "#Stopped")) {
                b.stopResponseTimer();
                if(b.getStatus() != DragonEyeBase.Status.STOPPED) {
                    b.stopped();
                    mListViewAdapter.notifyDataSetChanged();
                }
            } else if(s.startsWith("#Error:")) {
                b.stopResponseTimer();
                if(b.getStatus() != DragonEyeBase.Status.ERROR) {
                    b.error();
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
                } else if (s.startsWith("#FirmwareVersion")) {
                    String ss[] = s.split(":");
                    if (ss.length >= 2) {
                        b.stopResponseTimer();
                        b.setFirmwareVersion(ss[1]);
                    }
                } else if(TextUtils.equals(s, "#CompassLock")) {
                    b.stopResponseTimer();
                    b.compassLock();
                    mListViewAdapter.notifyDataSetChanged();
                } else if(TextUtils.equals(s, "#CompassUnlock")) {
                    b.stopResponseTimer();
                    b.compassUnlock();
                    mListViewAdapter.notifyDataSetChanged();
                } else if(TextUtils.equals(s, "#Ack")) {
                    b.stopResponseTimer();
                } else if (s.startsWith("#Error")) {
                    if (b.getStatus() != DragonEyeBase.Status.ERROR) {
                        b.started();
                        mListViewAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    void onMulticastRx(String addr, String s) {
        if (s.startsWith("BASE_A:") || s.startsWith("BASE_B:")) {
            String baseHost[] = s.split(":");
            if (baseHost.length >= 5) {
                //System.out.println(baseHost[0]);
                try {
                    InetAddress.getByName(baseHost[1]); // Test if valid ip address
                } catch (UnknownHostException e) {
                    Log.i("multicastThread", "Unknown host : " + baseHost[1]);
                    e.printStackTrace();
                    return;
                }

                if(TextUtils.equals(baseHost[1], "127.0.0.1"))
                    return;

                DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(baseHost[1]);
                if(b == null) { /* New base */
                    System.out.println("New base " + baseHost[0] + " " + baseHost[1]);
                    b = new DragonEyeBase(mContext, baseHost[0], baseHost[1]); /* Type, Address */
                    b.multicastReceived();
                    b.online();
                    DragonEyeApplication.getInstance().addBase(b);
                    mListView.deferNotifyDataSetChanged();

                    DragonEyeApplication.getInstance().requestSystemSettings(b);
                    DragonEyeApplication.getInstance().requestCameraSettings(b);
                    DragonEyeApplication.getInstance().requestStatus(b);
                    DragonEyeApplication.getInstance().requestFirmwareVersion(b);
                } else { /* Base exist ... */
                    b.multicastReceived();
                    b.setFps(Integer.parseInt(baseHost[2]));
                    b.setTemperature(Integer.parseInt(baseHost[3]));
                    b.setGpuLoad(Integer.parseInt(baseHost[4]));
                    b.stopResponseTimer();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListViewAdapter.notifyDataSetChanged();
                        }
                    });

                    if(b.getStatus() == DragonEyeBase.Status.OFFLINE ||
                            b.getStatus() == DragonEyeBase.Status.ONLINE) {
                        b.online();
                        DragonEyeApplication.getInstance().requestSystemSettings(b);
                        DragonEyeApplication.getInstance().requestCameraSettings(b);
                        DragonEyeApplication.getInstance().requestFirmwareVersion(b);
                        DragonEyeApplication.getInstance().requestStatus(b);
                    }
                    // Alway request while receive multicast from base ...
                }
            }
        } else if(s.startsWith("WIND:")) {
            String baseHost[] = s.split(":");
            if (baseHost.length >= 3) {
                float speed = Float.parseFloat(baseHost[1]);
                int dir = Integer.parseInt(baseHost[2]);
                mStatusView.setText(String.format("Wind %.2f / Dir %03d", speed, dir));

                Activity a = DragonEyeApplication.getInstance().getActivity();
                if(a != null) {
                    if (TextUtils.equals(a.getClass().getSimpleName(), "TimerActivity")) {
                        ((TimerActivity) a).onWindStatus(speed, dir);
                    }
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
                }
            } else if(intent.getAction().equals("baseMsg")) {
                if (intent.hasExtra("baseResponseTimeout")) { // base has no response of UDP request
                    mListViewAdapter.notifyDataSetChanged();
                } else if(intent.hasExtra("baseStatusUpdate")) { // base status changed
                    mListViewAdapter.notifyDataSetChanged();
                } else if (intent.hasExtra("baseTriggerTimeout")) { // Trigger timeout
                    mListViewAdapter.notifyDataSetChanged();
                }
            } else if(intent.getAction().equals("wifiMsg")) {
                if(intent.hasExtra("wifiConnected")) {
                    onWifiConnected(intent.getStringExtra("wifiConnected"));
                } else if(intent.hasExtra("wifiDisconnected")) {
                    onWifiDisconnected();
                }
            }
        }
    };

    public class UsbSerialThread implements Runnable {
        private Thread worker;
        private final AtomicBoolean running = new AtomicBoolean(false);

        public UsbSerialThread() {
            super();
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
            worker.setPriority(Thread.MAX_PRIORITY);
            worker.start();
        }

        public void stop() {
            running.set(false);
        }

        @Override
        public void run() {
            running.set(true);

            System.out.println("UsbSerialThread - Started ...");

            while(running.get()) {
                byte[] buffer = new byte[1024];
                int r = 0;
                try {
                    r = mUsbSerialPort.read(buffer, 1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(r <= 0)
                    continue;

                if(buffer[0] != '<')
                    continue;

                String s = new String(buffer, StandardCharsets.UTF_8).split("\0")[0];

                System.out.println("UsbSerial RX : " + s);

                String ss[] = s.split(">");
                for(String cmd : ss) {
                    int h = cmd.indexOf("<");
                    if(h < 0)
                        continue; // Command format is <...>

                    char base = cmd.charAt(h + 1);
                    int serNo = Integer.parseInt(cmd.substring(h + 2, cmd.length()));

                    Activity a = DragonEyeApplication.getInstance().getActivity();

                    if (s.charAt(0) == '<' && s.charAt(s.length() - 1) == '>') {
                        if(TextUtils.equals(a.getClass().getSimpleName(), "MainActivity")) {
                            ((MainActivity) a).onBaseTrigger(null, s);
                        } else if(TextUtils.equals(a.getClass().getSimpleName(), "TimerActivity")) {
                            ((TimerActivity)a).onBaseTrigger(null, s);
                        }
                    }

                    String msg = new String(cmd.substring(h) + ">");

                    if ((base == 'A' && serNo != serNoA) || (base == 'B' && serNo != serNoB)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mActivity, "UsbSerial RX : " + msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            running.set(false);

            System.out.println("UsbSerialThread - Stopped ...");
        }
    }

    private void ConnectUsbDevice(Boolean permissionGranted) throws IOException {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            Toast.makeText(mContext,"No USB Device available !!!",Toast.LENGTH_SHORT).show();
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());

        if(usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            // USB request permission
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                Toast.makeText(mContext,"USB permission denied !!!",Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(mContext,"USB open failed !!!",Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(mContext,"USB Device Connected !!!",Toast.LENGTH_SHORT).show();

        mUsbSerialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
        mUsbSerialPort.open(usbConnection);
        mUsbSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        if(!mUsbSerialThread.isRunning())
            mUsbSerialThread.start();
    }

    private void UsbSerialWrite(byte [] data) {
        if(!mUsbSerialThread.isRunning())
            return;

        try {
            mUsbSerialPort.write(data, 100);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver usbBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Toast.makeText(mContext, "USB Device Attached", Toast.LENGTH_SHORT).show();
                    try {
                        ConnectUsbDevice(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Toast.makeText(mContext, "USB Device Detached", Toast.LENGTH_SHORT).show();
                    if (mUsbSerialPort != null && mUsbSerialPort.isOpen()) {
                        if (mUsbSerialThread.isRunning())
                            mUsbSerialThread.stop();
                        try {
                            mUsbSerialPort.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case INTENT_ACTION_GRANT_USB:
                    //Toast.makeText(mContext, "INTENT_ACTION_GRANT_USB", Toast.LENGTH_SHORT).show();
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    try {
                        ConnectUsbDevice(granted);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onStop() {
        //DragonEyeApplication.getInstance().mTonePlayer.stopPlay();
        DragonEyeApplication.getInstance().stopPlaying();
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
        String version = getResources().getString(R.string.version);
        final String AboutDialogMessage = "\t dragon-eye-rc " + version + "\n\t All bugs made by Steve Chang \n\t Website for contact :\n";
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
            case R.id.item_restart:
                clearAllDragonEyeBase();

                if(DragonEyeApplication.getInstance().mUdpClient.isRunning())
                    DragonEyeApplication.getInstance().mUdpClient.restart();
                if(mMulticastThread2.isRunning())
                    mMulticastThread2.restart();
                if(mMulticastThread3.isRunning())
                    mMulticastThread3.restart();

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