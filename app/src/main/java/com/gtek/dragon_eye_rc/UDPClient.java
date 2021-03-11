package com.gtek.dragon_eye_rc;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by lenovo on 2016/2/23.
 */
public class UDPClient implements Runnable {
    //final static int udpPort = 9999;
    //final static String hostIp = "192.168.1.4";
    private Context mContext;
    private static DatagramSocket socket = null;
    private static DatagramPacket packetSend, packetRcv;
    private boolean udpLife = true; //udp生命线程
    private byte[] msgRcv = new byte[1024]; //接收消息

    public UDPClient(Context context) {
        super();
        mContext = context;
    }

    //返回udp生命线程因子是否存活
    public boolean isUdpLife() {
        if (udpLife) {
            return true;
        }

        return false;
    }

    //更改UDP生命线程因子
    public void setUdpLife(boolean b) {
        udpLife = b;
    }

    //发送消息
    public String send(String hostIp, int udpPort, String msgSend) {
        InetAddress hostAddress = null;

        try {
            hostAddress = InetAddress.getByName(hostIp);
        } catch (UnknownHostException e) {
            Log.i("udpClient", "未找到服务器");
            e.printStackTrace();
        }

/*      try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            Log.i("udpClient","建立发送数据报失败");
            e.printStackTrace();
        }
*/

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
            Log.i("udpClient", "发送失败");
        }
        //   socket.close();
        return msgSend;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(3000);//设置超时为3s
        } catch (SocketException e) {
            Log.i("udpClient", "建立接收数据报失败");
            e.printStackTrace();
        }
        packetRcv = new DatagramPacket(msgRcv, msgRcv.length);
        while (udpLife) {
            try {
                Log.i("udpClient", "UDP监听");
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
                Intent intent = new Intent();
                intent.setAction("udpMsg");
                intent.putExtra("udpPollTimeout", 0);
                mContext.sendBroadcast(intent);
                //e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i("udpClient", "UDP监听关闭");
        socket.close();
    }
}