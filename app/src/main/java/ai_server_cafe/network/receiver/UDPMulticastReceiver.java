package ai_server_cafe.network.receiver;

import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.interfaces.ReceivedData;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class UDPMulticastReceiver extends AbstractLoopThreadCafe {
    private MulticastSocket socket = null;

    protected List<ReceivedData> receivedData;

    protected UDPMulticastReceiver(String name) {
        super(name);
    }

    @Override
    @Deprecated
    public void start() {}

    public void startWith(int port, String hostAddress, String interfaceAddress) {
        boolean startFlag = true;
        try {
            this.socket = new MulticastSocket(new InetSocketAddress(port));
            this.socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            try {
                this.socket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            } catch (UnsupportedOperationException e) {
                this.logger.warn("Multicast socket option is not available : SO_REUSEPORT");
            }
        } catch (IOException e) {
            this.logger.error("Multicast socket cannot bind this port : {}", port);
            startFlag = false;
        }
        NetworkInterface ni = null;
        try {
            ni = NetworkInterface.getNetworkInterfaces().nextElement();
        } catch (SocketException e) {
            this.logger.error("No interface address");
            startFlag = false;
        }
        if(!interfaceAddress.isEmpty()) {
            try {
                ni = NetworkInterface.getByInetAddress(InetAddress.getByName(interfaceAddress));
            } catch (SocketException | UnknownHostException e) {
                this.logger.error("Interface address [%s] is not exist", interfaceAddress);
                startFlag = false;
            }
        }
        try {
            this.socket.joinGroup(new InetSocketAddress(hostAddress, port), ni);
        } catch (IOException e) {
            this.logger.error("Multicast socket cannot join group with : [port : {}, host : {}]", port, hostAddress);
            startFlag = false;
        }
        if(startFlag) super.start();
        else this.logger.warn("This thread will not start with error");
    }

    @Override
    protected void loop() {
        try {
            byte[] buf = new byte[this.socket.getReceiveBufferSize()];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);
            this.socket.receive(packet);
            final byte[] packetData = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
            this.receivedData.addLast(new ReceivedData(packetData, TimeHelper.now()));
            this.onReceive(packetData);
        } catch (SocketTimeoutException exception) {
            this.onReceive(null);
            //this.logger.warn("UDP multicast could not receive in {} milliseconds", this.getTimeout());
        } catch (IOException e) {
            this.logger.error("UDP multicast could not receive with IOException");
        } catch (Exception e) {
            e.printStackTrace();
            this.logger.error("Unknown error occurred");
        }
        while (!this.receivedData.isEmpty() && TimeHelper.now() - this.receivedData.getFirst().receivedTime > this.dataStackingTime()) {
            this.receivedData.removeFirst();
        }
    }

    @Override
    protected void onTerminate() {
        this.socket.close();
        super.onTerminate();
    }

    @Override
    protected void init() {
        int timeout = this.getTimeout();
        this.receivedData = new ArrayList<>();
        if (timeout > 0) {
            try {
                this.socket.setSoTimeout(timeout);
            } catch (SocketException e) {
                this.logger.warn("Receiver timeout couldn't set : {} milliseconds", timeout);
            }
        }
    }

    // millisecond
    protected abstract int getTimeout();

    protected abstract double dataStackingTime();

    protected abstract void onReceive(final byte[] data);
}
