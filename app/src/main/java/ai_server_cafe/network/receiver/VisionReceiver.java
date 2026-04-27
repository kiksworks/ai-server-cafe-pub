package ai_server_cafe.network.receiver;

import ai_server_cafe.network.proto.ssl.vision.VisionWrapper;
import ai_server_cafe.updater.UpdaterWorld;
import com.google.protobuf.InvalidProtocolBufferException;

public final class VisionReceiver extends UDPMulticastReceiver {
    private static VisionReceiver instance = null;

    private VisionReceiver() {
        super("vision_receiver");
    }

    @Override
    protected int getTimeout() {
        return 500;
    }

    @Override
    protected double dataStackingTime() {
        return 1.0;
    }

    @Override
    protected void onReceive(final byte[] data) {
        if (data == null) {
            UpdaterWorld.getInstance().updateVision(null, this.receivedData.size());
            return;
        }
        try {
            VisionWrapper.Packet packet = VisionWrapper.Packet.parseFrom(data);
            UpdaterWorld.getInstance().updateVision(packet, this.receivedData.size());
        } catch (InvalidProtocolBufferException e) {
            this.logger.warn("Received packet could not parse with an error at {}", this.name);
        }
    }

    public static VisionReceiver getInstance() {
        if (instance == null) {
            instance = new VisionReceiver();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }
}
