package ai_server_cafe.util.interfaces;

public class ReceivedData {
    public final double receivedTime;
    public final byte[] data;

    public ReceivedData(final byte[] data, double receivedTime) {
        this.data = data;
        this.receivedTime = receivedTime;
    }
}
