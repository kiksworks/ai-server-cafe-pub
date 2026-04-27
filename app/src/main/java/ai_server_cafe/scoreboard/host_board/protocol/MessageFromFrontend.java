package ai_server_cafe.scoreboard.host_board.protocol;

public class MessageFromFrontend {
    private String type;
    private String payload;

    // デフォルトコンストラクタ
    public MessageFromFrontend() {}

    // パラメータ付きコンストラクタ
    public MessageFromFrontend(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    // getter/setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
