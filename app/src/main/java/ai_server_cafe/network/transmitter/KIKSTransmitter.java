package ai_server_cafe.network.transmitter;

import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.model.network.SendCommand;
import ai_server_cafe.network.radio.EnumRadioType;
import ai_server_cafe.updater.UpdaterOptionalCommand;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.List;

public final class KIKSTransmitter extends UDPMulticastTransmitter {
    private static KIKSTransmitter instance = null;
    private final Logger logger = LogManager.getLogger("kiks transmitter");

    private KIKSTransmitter() {
        super("kiks transmitter");
    }

    @Override
    synchronized public void sendCommands(@Nonnull List<SendCommand> commandList) {
        for (SendCommand command : commandList) {
            this.sendCommand(command);
        }
    }

    @Override
    synchronized public void sendCommand(@Nonnull SendCommand command) {
        byte[] sendBuf = new byte[11];
        sendBuf[0] = (byte) ((command.getId() + 1) & 0b1111);
        switch(command.getKickFlag().getKey().getId()) {
            case 0:
                sendBuf[0] |= 0b00000000;
                break;
            case 1:
                sendBuf[0] |= 0b00100000;
                break;
            default:
                sendBuf[0] |= 0b00110000;
        }
        sendBuf[0] |= (byte) (command.getOmega() >= 0.0 ? 0b00000000 : 0b10000000);
        int vel = (int)(FastMath.hypot(command.getVx(), command.getVy()));
        sendBuf[1]  = (byte)((vel & 0xff00) >> 8);
        sendBuf[2]  = (byte)(vel & 0x00ff);

        double dir = MathHelper.wrap2PI(FastMath.atan2(command.getVy(), command.getVx()) + MathHelper.HALF_PI);
        sendBuf[3]  = (byte) (((int)((dir / MathHelper.TWO_PI) * 0xffff) & 0xff00) >> 8);
        sendBuf[4]  = (byte) ((int)((dir / MathHelper.TWO_PI) * 0xffff) & 0x00ff);

        int o  = (int)(FastMath.abs(command.getOmega()) * 1000.0);
        sendBuf[5] = (byte) ((o & 0xff00) >> 8);
        sendBuf[6] = (byte) (o & 0x00ff);

        sendBuf[7] = (byte)(command.getDribble() + 3);
        sendBuf[8] = (byte)((int)(command.getKickFlag().getValue()));

        sendBuf[9]  = '\r';
        sendBuf[10] = '\n';

        try {
            this.socket.setSendBufferSize(sendBuf.length);
            final DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, this.hostAddress, this.port);
            this.socket.send(packet);
        } catch (SocketException e) {
            this.logger.error("socket error occurred");
        } catch (IOException e) {
            this.logger.error("socket io error occurred");
        }
    }

    @Override
    public EnumRadioType getType() {
        return EnumRadioType.KIKS;
    }

    @Override
    protected int getTimeout() {
        return 10;
    }

    public static KIKSTransmitter getInstance() {
        if (instance == null) {
            instance = new KIKSTransmitter();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public void sendOptionalCommand(OptionalCommand optionalCommand) {
        UpdaterOptionalCommand.getInstance().update(optionalCommand);
    }
}
