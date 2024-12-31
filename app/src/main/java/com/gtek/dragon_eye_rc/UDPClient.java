package com.gtek.dragon_eye_rc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by lenovo on 2016/2/23.
 */
public class UDPClient implements Runnable {
    private Context mContext;
    private DatagramSocket socket = null;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean doFlush = new AtomicBoolean(false);

    private int localAddr = 0;
    private int networkId = -1;

    public UDPClient(Context context) {
        super();
        mContext = context;
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
        System.out.println("UDPClient - Stopped ...");
/*
        if(socket != null) {
            socket.disconnect();
            socket.close(); // Trigger exception
        }
*/
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

    public void flush() {
        doFlush.set(true);
    }

    public DatagramPacket send(String hostIp, int udpPort, String msgSend) {
        InetAddress hostAddress = null;

        if(socket == null)
            return null;

        try {
            hostAddress = InetAddress.getByName(hostIp);
        } catch (UnknownHostException e) {
            //Log.i("UDPClient", "Unknown host");
            e.printStackTrace();
            return null;
        }

        DatagramPacket packetSend = new DatagramPacket(msgSend.getBytes(), msgSend.getBytes().length, hostAddress, udpPort);

        try {
            socket.send(packetSend);
        } catch (IOException e) {
            e.printStackTrace();
            //Log.i("UDPClient", "Send packet fail !!!");
        }

        return packetSend;
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


    @Override
    public void run() {
        running.set(true);

        System.out.println("UDPClient - Started ...");

        byte[] buf = new byte[4096]; //接收消息
        DatagramPacket packetRcv = new DatagramPacket(buf, buf.length);

        @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        while (running.get()) {
/*
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if(wifiManager.isWifiEnabled() == false) {
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
                System.out.println("UDPClient - Supplicant NOT Complete !!!");
                continue;
            }

            if (DragonEyeApplication.getInstance().WifiIpAddress() == 0 ||
                    DragonEyeApplication.getInstance().WifiNetworkId() == -1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
*/
            //int addr = wifiManager.getDhcpInfo().ipAddress;
            int addr = DragonEyeApplication.getInstance().WifiIpAddress();
            //if(addr != localAddr || networkId != wifiInfo.getNetworkId()) {
            if (localAddr != addr || networkId != DragonEyeApplication.getInstance().WifiNetworkId()) {
                localAddr = addr;
                networkId = DragonEyeApplication.getInstance().WifiNetworkId();
/*
                ByteBuffer tmp = ByteBuffer.allocate(4);
                tmp.putInt(addr);
                InetAddress localInetAddress = null;
                InetSocketAddress isa = null;
                try {
                    localInetAddress = intToInet(addr);
                    isa = new InetSocketAddress(InetAddress.getByAddress(tmp.array()), 0);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
*/
                try {
                    InetAddress localInetAddress = intToInet(addr);

                    NetworkInterface ni = NetworkInterface.getByInetAddress(localInetAddress);
                    if(ni != null)
                        System.out.println("UDPClient - Wifi interface : " + ni.getDisplayName() + ", address :" + localInetAddress);
                    else
                        System.out.println("UDPClient - NetworkInterface.getByInetAddress " + localInetAddress + " fail !!!");

                    //System.out.println("UDPClient - Wifi SSID : " + wifiInfo.getSSID());
                    System.out.println("UDPClient - Socket address " + localInetAddress);

                    if(socket != null) {
                        socket.close();
                        socket = null;
                    }

                    socket = new DatagramSocket();
                    socket.setReuseAddress(true);
                    //socket.bind(isa);
                    socket.setSoTimeout(200);//设置超时为1s
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }

            if(socket == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("UDPClient - Null socket");
                continue;
            }

            try {
                //socket.setSoTimeout(20);
                socket.receive(packetRcv);
                String s = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());
                System.out.println("UDP receive : " + s);

                if(doFlush.get()) {
                    System.out.println("Flush out ...");
                    doFlush.set(false);
                    continue;
                }

                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                if(!pm.isInteractive()) { // Is screen on ?
                    System.out.println("Screen off. Flush out ...");
                    continue;
                }

                Activity a = DragonEyeApplication.getInstance().getActivity();
                if (s.charAt(0) == '<' && s.charAt(s.length() - 1) == '>') {
                    DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(packetRcv.getAddress().getHostAddress());
                    if (a != null && b != null) {
                        //if (a != null) { /* Not necessary a dragon-eye base */
                        if (TextUtils.equals(a.getClass().getSimpleName(), "MainActivity")) {
                            ((MainActivity) a).onBaseTrigger(b, s);
                        } else if (TextUtils.equals(a.getClass().getSimpleName(), "F3fTimerActivity")) {
                            ((F3fTimerActivity) a).onBaseTrigger(b, s);
                        } else if (TextUtils.equals(a.getClass().getSimpleName(), "F3bTimerActivity")) {
                            ((F3bTimerActivity) a).onBaseTrigger(b, s);
                        } else if(TextUtils.equals(a.getClass().getSimpleName(), "VideoActivity")) {
                            ((VideoActivity)a).onBaseTrigger(b, s);
                        }
                    }
                } else {
                    Intent intent = new Intent();
                    intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    intent.setAction("udpMsg");
                    intent.putExtra("udpRcvMsg", packetRcv.getAddress().getHostAddress() + ":" + s);
                    mContext.sendBroadcast(intent);
                }
                //Log.i("Rcv", RcvMsg);
            } catch (SocketTimeoutException e) {
                //e.printStackTrace();
                //System.out.println("UDPClient - RX timeout");
                //if(doFlush.get())
                //    doFlush.set(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(socket != null) {
            socket.disconnect();
            socket.close();
            socket = null;
        }

        localAddr = 0;
        networkId = -1;

        System.out.println("UDPClient - Stopped ...");

        running.set(false);
    }
}