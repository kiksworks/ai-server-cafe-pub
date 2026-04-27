package ai_server_cafe.network.receiver;

import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.updater.UpdaterRefBox;
import com.google.protobuf.InvalidProtocolBufferException;

public final class RefBoxReceiver extends UDPMulticastReceiver {
    private static RefBoxReceiver instance = null;

    private RefBoxReceiver() {
        super("refbox_receiver");
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
            UpdaterRefBox.getInstance().updateRefBox(null, this.receivedData.size());
            return;
        }
        try {
            GcRefereeMessage.Referee packet = GcRefereeMessage.Referee.parseFrom(data);
            UpdaterRefBox.getInstance().updateRefBox(packet, this.receivedData.size());
        } catch (InvalidProtocolBufferException e) {
            this.logger.warn("Received packet could not parse with an error at {}", this.name);
        }
    }

    public static RefBoxReceiver getInstance() {
        if (instance == null) {
            instance = new RefBoxReceiver();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }
}
