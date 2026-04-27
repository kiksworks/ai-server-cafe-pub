package ai_server_cafe.network.transmitter;

import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.model.network.SendCommand;
import ai_server_cafe.network.radio.EnumRadioType;
import ai_server_cafe.updater.UpdaterOptionalCommand;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class BroadcastTransmitter extends UDPMulticastTransmitter {
    private static BroadcastTransmitter instance = null;
    private final List<SendCommand> buffer;

    protected BroadcastTransmitter() {
        super("broadcast transmitter");
        this.buffer = new ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public void startWith(int port, String hostAddress, String interfaceAddress) {
        boolean startFlag = true;
        try {
            this.socket = new DatagramSocket();
            this.socket.setBroadcast(true);
            this.socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            try {
                this.socket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            } catch (UnsupportedOperationException e) {
                this.logger.warn("Datagram socket option is not available : SO_REUSEPORT");
            }
        } catch (IOException e) {
            this.logger.error("Datagram socket cannot bind this port : {}", port);
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
                if (ni == null)
                    throw new IOException();
            } catch (SocketException | UnknownHostException e) {
                this.logger.error("socket error", interfaceAddress);
                startFlag = false;
            } catch (IOException e) {
                this.logger.error("Interface address {} is not exist", interfaceAddress);
                startFlag = false;
            }
        }
        try {
            String[] ss = interfaceAddress.split("\\.");
            if (ss.length == 4) {
                hostAddress = ss[0] + "." + ss[1] + "." + ss[2] + ".255";
            }
            this.hostAddress = InetAddress.getByName(hostAddress);
            this.port = port;
        } catch (IOException e) {
            this.logger.error("Datagram socket cannot join group with : [port : {}, host : {}]", port, hostAddress);
            startFlag = false;
        }
        if(startFlag) {
            this.logger.info("start with [{}:{}, {}]", hostAddress, port, interfaceAddress);
            super.start();
        }
        else this.logger.warn("This thread will not start with error");
    }

    public static BroadcastTransmitter getInstance() {
        if (instance == null) {
            instance = new BroadcastTransmitter();
        }
        return instance;
    }

    @Override
    public synchronized void sendCommands(@Nonnull List<SendCommand> commandList) {
        for (SendCommand command : commandList) {
            this.sendCommand(command);
        }
    }

    @Override
    public synchronized void sendCommand(@Nonnull SendCommand command) {
        this.buffer.add(command);
    }

    @Override
    public EnumRadioType getType() {
        return EnumRadioType.BROADCAST;
    }

    @Override
    protected int getTimeout() {
        return 0;
    }

    public synchronized void sendBuffer() {
        if (this.buffer.isEmpty())
            return;
        int kPacketSize = 11;
        int size = this.buffer.size();
        byte[] sendBuf = new byte[kPacketSize * this.buffer.size()];
        int i;
        List<Integer> ids = new ArrayList<>();
        for (i = 0; i < this.buffer.size(); i++) {
            SendCommand command = this.buffer.get(i);
            ids.add(command.getId());
            sendBuf[0 + i * kPacketSize] = (byte) ((command.getId() + 1) & 0b1111);
            switch(command.getKickFlag().getKey().getId()) {
                case 0:
                    sendBuf[0 + i * kPacketSize] |= 0b00000000;
                    break;
                case 1:
                    sendBuf[0 + i * kPacketSize] |= 0b00100000;
                    break;
                default:
                    sendBuf[0 + i * kPacketSize] |= 0b00110000;
            }
            sendBuf[0 + i * kPacketSize] |= (byte) (command.getOmega() >= 0.0 ? 0b00000000 : 0b10000000);
            int vel = (int)(FastMath.hypot(command.getVx(), command.getVy()));
            sendBuf[1 + i * kPacketSize]  = (byte)((vel & 0xff00) >> 8);
            sendBuf[2 + i * kPacketSize]  = (byte)(vel & 0x00ff);

            double dir = MathHelper.wrap2PI(FastMath.atan2(command.getVy(), command.getVx()) + MathHelper.HALF_PI);
            sendBuf[3 + i * kPacketSize]  = (byte) (((int)((dir / MathHelper.TWO_PI) * 0xffff) & 0xff00) >> 8);
            sendBuf[4 + i * kPacketSize]  = (byte) ((int)((dir / MathHelper.TWO_PI) * 0xffff) & 0x00ff);

            int o  = (int)(FastMath.abs(command.getOmega()) * 1000.0);
            sendBuf[5 + i * kPacketSize] = (byte) ((o & 0xff00) >> 8);
            sendBuf[6 + i * kPacketSize] = (byte) (o & 0x00ff);

            sendBuf[7 + i * kPacketSize] = (byte)(command.getDribble() + 3);
            sendBuf[8 + i * kPacketSize] = (byte)((int)(command.getKickFlag().getValue()));

            sendBuf[9 + i * kPacketSize]  = '\r';//(byte)(i + 1 < size ? 0 : '\r');
            sendBuf[10 + i * kPacketSize] = '\n';//(byte)(i + 1 < size ? 0 : '\n');
        }
        try {
            this.socket.setSendBufferSize(sendBuf.length);
            final DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, this.hostAddress, this.port);
            this.socket.send(packet);
        } catch (SocketException e) {
            e.printStackTrace();
            this.logger.error("socket error occurred");
        } catch (IOException e) {
            this.logger.error("socket io error occurred");
        }
        this.buffer.clear();
    }

    public void sendOptionalCommand(OptionalCommand optionalCommand) {
        UpdaterOptionalCommand.getInstance().update(optionalCommand);
    }

    public static void reset() {
        instance = null;
    }
}
