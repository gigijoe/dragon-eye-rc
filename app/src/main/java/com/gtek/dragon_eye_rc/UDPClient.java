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
    private static DatagramSocket socket = null;
    private static DatagramPacket packetSend, packetRcv;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

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
        worker.start();
    }

    public void stop() {
        running.set(false);
    }

    public String send(String hostIp, int udpPort, String msgSend) {
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

        packetSend = new DatagramPacket(msgSend.getBytes(), msgSend.getBytes().length, hostAddress, udpPort);

        try {
            socket.send(packetSend);
        } catch (IOException e) {
            e.printStackTrace();
            //Log.i("UDPClient", "Send packet fail !!!");
        }

        return msgSend;
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

        byte[] buf = new byte[1024]; //接收消息
        packetRcv = new DatagramPacket(buf, buf.length);

        @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        while (running.get()) {
            if(wifiManager.isWifiEnabled() == false) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
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
                continue;
            }

            int addr = wifiManager.getDhcpInfo().ipAddress;
            if(addr != localAddr || networkId != wifiInfo.getNetworkId()) {
                localAddr = addr;
                networkId = wifiInfo.getNetworkId();

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

                System.out.println("UDPClient - Wifi SSID : " + wifiInfo.getSSID());
                System.out.println("Socket address " + localInetAddress);

                if(socket != null) {
                    socket.close();
                    socket = null;
                }

                try {
                    socket = new DatagramSocket();
                    socket.setReuseAddress(true);
                    //socket.bind(isa);
                    //socket.setSoTimeout(3000);//设置超时为3s
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
                continue;
            }

            try {
                //socket.setSoTimeout(100);
                socket.receive(packetRcv);
                String s = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());

                System.out.println("UDP receive : " + s);

                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                if(!pm.isInteractive())
                    continue;

                Activity a = DragonEyeApplication.getInstance().getActivity();
                if (s.charAt(0) == '<' && s.charAt(s.length() - 1) == '>') {
                    DragonEyeBase b = DragonEyeApplication.getInstance().findBaseByAddress(packetRcv.getAddress().getHostAddress());
                    if (a != null && b != null) {
                        if (TextUtils.equals(a.getClass().getSimpleName(), "MainActivity")) {
                            ((MainActivity) a).onBaseTrigger(b, s);
                        } else if (TextUtils.equals(a.getClass().getSimpleName(), "TimerActivity")) {
                            ((TimerActivity) a).onBaseTrigger(b, s);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(socket != null) {
            socket.close();
            socket = null;
        }

        localAddr = 0;
        networkId = -1;

        System.out.println("UDPClient - Stopped ...");

        running.set(false);
    }
}