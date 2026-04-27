package ai_server_cafe.network.receiver;

import ai_server_cafe.network.proto.ssl.vision.VisionWrapperTracked;
import ai_server_cafe.updater.UpdaterWorld;
import com.google.protobuf.InvalidProtocolBufferException;

public final class VisionTrackerReceiver extends UDPMulticastReceiver {
    private static VisionTrackerReceiver instance = null;

    private VisionTrackerReceiver() {
        super("vision-tracker-receiver");
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
            UpdaterWorld.getInstance().updateVisionTracker(null, this.receivedData.size());
            return;
        }
        try {
            VisionWrapperTracked.TrackerWrapperPacket packet = VisionWrapperTracked.TrackerWrapperPacket.parseFrom(data);
            UpdaterWorld.getInstance().updateVisionTracker(packet, this.receivedData.size());
        } catch (InvalidProtocolBufferException e) {
            this.logger.warn("Received packet could not parse with an error at {}", this.name);
        }
    }

    public static VisionTrackerReceiver getInstance() {
        if (instance == null) {
            instance = new VisionTrackerReceiver();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }
}
