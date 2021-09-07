package com.gtek.dragon_eye_rc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
    private byte[] msgRcv = new byte[1024]; //接收消息
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
/*
            Intent intent = new Intent();
            intent.setAction("udpMsg");
            intent.putExtra("udpSendMsg", msgSend);
            mContext.sendBroadcast(intent);
*/
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

        packetRcv = new DatagramPacket(msgRcv, msgRcv.length);

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

            if(socket == null)
                continue;
            
            try {
                socket.setSoTimeout(3000);//设置超时为3s
                socket.receive(packetRcv);
                String s = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());

                System.out.println("UDP receive : " + s);

                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                intent.setAction("udpMsg");
                intent.putExtra("udpRcvMsg", packetRcv.getAddress().getHostAddress() + ":" + s);
                mContext.sendBroadcast(intent);
                //Log.i("Rcv", RcvMsg);
            } catch (SocketTimeoutException e) {
                /*
                Intent intent = new Intent();
                intent.setAction("udpMsg");
                intent.putExtra("udpPollTimeout", 0);
                mContext.sendBroadcast(intent);
                */
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