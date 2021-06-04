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
    //final static int udpPort = 9999;
    //final static String hostIp = "192.168.1.4";
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

    //发送消息
    public String send(String hostIp, int udpPort, String msgSend) {
        InetAddress hostAddress = null;

        if(socket == null)
            return null;

        try {
            hostAddress = InetAddress.getByName(hostIp);
        } catch (UnknownHostException e) {
            Log.i("UDPClient", "Unknown host");
            e.printStackTrace();
            return null;
        }

        packetSend = new DatagramPacket(msgSend.getBytes(), msgSend.getBytes().length, hostAddress, udpPort);

        try {
            socket.send(packetSend);

            Intent intent = new Intent();
            intent.setAction("udpMsg");
            intent.putExtra("udpSendMsg", msgSend);
            mContext.sendBroadcast(intent);
            Log.i("Send", msgSend);

        } catch (IOException e) {
            e.printStackTrace();
            Log.i("UDPClient", "Send packet fail !!!");
        }

        return msgSend;
    }

    @Override
    public void run() {
        running.set(true);

        System.out.println("UDPClient - Started ...");

        packetRcv = new DatagramPacket(msgRcv, msgRcv.length);

        @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        while (running.get()) {
            if(wifiManager.isWifiEnabled() == false)
                continue;

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getNetworkId() == -1)
                continue;

            if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED)
                continue;

            int addr = wifiManager.getDhcpInfo().ipAddress;
            if(addr != localAddr || networkId != wifiInfo.getNetworkId()) {
                localAddr = addr;
                networkId = wifiInfo.getNetworkId();

                ByteBuffer tmp = ByteBuffer.allocate(4);
                tmp.putInt(addr);
                InetAddress localInetAddress = null;
                InetSocketAddress isa = null;
                try {
                    localInetAddress = InetAddress.getByAddress(tmp.array());
                    isa = new InetSocketAddress(InetAddress.getByAddress(tmp.array()), 0);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                System.out.println("UDPClient - Wifi SSID : " + wifiInfo.getSSID());
                System.out.println("Bind address " + localInetAddress);

                if(socket != null) {
                    socket.close();
                    socket = null;
                }

                try {
                    socket = new DatagramSocket();
                    socket.setReuseAddress(true);
                    socket.bind(isa);
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
                //packetRcv.getAddress().getHostAddress()
                String RcvMsg = packetRcv.getAddress().getHostAddress() + ":" + new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());
                //String RcvMsg = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());
                //将收到的消息发给主界面
                Intent intent = new Intent();
                intent.setAction("udpMsg");
                intent.putExtra("udpRcvMsg", RcvMsg);
                mContext.sendBroadcast(intent);
                Log.i("Rcv", RcvMsg);
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