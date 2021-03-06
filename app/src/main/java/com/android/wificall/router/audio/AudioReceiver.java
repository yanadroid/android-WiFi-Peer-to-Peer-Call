package com.android.wificall.router.audio;

import android.util.Log;

import com.android.wificall.data.Packet;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.view.activity.CallActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import io.reactivex.FlowableEmitter;

/**
 * Created by Serhii Slobodyanuk on 02.09.2016.
 */
public class AudioReceiver {

    private boolean isReceiving = false;
    private DatagramSocket mReceivingSocket = null;
    private byte[] buffer;
    private CallActivity mActivity;
    private OnReceiveAudioListener mOnReceiveDataListener;

    public AudioReceiver(CallActivity activity, int bufferSize, OnReceiveAudioListener listener) {
        this.mActivity = activity;
        this.mOnReceiveDataListener = listener;
        buffer = new byte[bufferSize];
    }

    public void receiveData(FlowableEmitter mSubscriber) {
        try {
            if (mReceivingSocket == null) {
                DatagramChannel mSocketChannel = DatagramChannel.open();
                mReceivingSocket = mSocketChannel.socket();
                mReceivingSocket.setReuseAddress(true);
                mReceivingSocket.bind(new InetSocketAddress(Configuration.RECEIVE_PORT));
            }
            buffer = new byte[mReceivingSocket.getReceiveBufferSize() / 10];

            byte[] routingTable = NetworkManager.serializeRoutingTable();
            Packet ack = new Packet(Packet.TYPE.HELLO_ACK, routingTable, NetworkManager.getSelf().getGroupOwnerMac(), NetworkManager.getSelf()
                    .getMac());
            Sender.queuePacket(ack);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        isReceiving = true;

        while (isReceiving /*&& !mActivity.isThreadStopped()*/) {
            try {
                mReceivingSocket.receive(packet);
                mSubscriber.onNext(packet);
            } catch (IOException e) {
                Log.e("VR", "IOException");
                e.printStackTrace();
                isReceiving = false;
                mActivity.setThreadStopped(true);
                break;
            } catch (Exception e) {
                e.printStackTrace();
                mActivity.setThreadStopped(true);
                isReceiving = false;
                break;
            }
        }
    }

    public void stopReceiving() {
        this.isReceiving = false;
        if (mReceivingSocket != null) {
            mReceivingSocket.disconnect();
            mReceivingSocket.close();
            mReceivingSocket = null;
        }
    }
}
