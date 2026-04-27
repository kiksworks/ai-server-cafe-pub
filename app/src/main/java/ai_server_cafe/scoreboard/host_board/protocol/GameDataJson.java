package ai_server_cafe.scoreboard.host_board.protocol;

// フロントエンドに送信するJSONの型
public class GameDataJson {
    private String gameID;
    private int gameDuration;
    private int scoreA;
    private int scoreB;
    private int infoCode;

    public GameDataJson(String gameID, int GameDuration, int scoreA, int scoreB, int infoCode) {
        this.gameID = gameID;
        this.gameDuration = GameDuration;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.infoCode = infoCode;
    }


    public String getID() {
        return gameID;
    }

    public int getDuration() {
        return gameDuration;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public int getInfoCode() {
        return infoCode;
    }
}
