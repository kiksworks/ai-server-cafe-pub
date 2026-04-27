package ai_server_cafe.updater;

import ai_server_cafe.config.Config;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperWeakCloneableList;
import org.apache.commons.math3.util.FastMath;
import org.eclipse.jetty.websocket.api.Session;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class UpdaterScoreBoard {
    private static UpdaterScoreBoard instance = null;

    // スコア情報
    private int scoreYellow;    // ScoreAに対応
    private int scoreBlue;      // ScoreBに対応
    // AIの強さ
    private int blueAiLevel;
    private int yellowAiLevel;

    private String gameID;
    private int gameDuration;
    private int infoCode;

    private boolean isDirty;
    private final List<ScoreUpdateListener> listeners;

    private UpdaterScoreBoard() {
        Config config = ConfigManager.getInstance().getConfig();
        this.scoreYellow = 0;
        this.scoreBlue = 0;
        this.blueAiLevel = 0;
        this.yellowAiLevel = 0;
        this.gameID = "";
        this.gameDuration = config.demoMatchTime;
        this.infoCode = 0;
        this.isDirty = false;
        this.listeners = new ArrayList<>();
    }

    // Singleton インスタンス取得
    public static synchronized UpdaterScoreBoard getInstance() {
        if (instance == null) {
            instance = new UpdaterScoreBoard();
        }
        return instance;
    }

    // リスナーの登録と削除
    public synchronized void addListener(ScoreUpdateListener listener) {
        this.listeners.add(listener);
    }

    public synchronized void removeAllListener() {
        this.listeners.clear();
    }

    @Nonnull
    public synchronized WrapperWeakCloneableList<ScoreUpdateListener> getListeners() {
        return new WrapperWeakCloneableList<>(this.listeners);
    }

    // スコアやゲーム情報の操作メソッド
    public synchronized void goalYellow(boolean isAI) {
        this.scoreYellow += 1;
        this.infoCode = isAI ? 2 : 21;
        this.isDirty = true;
    }

    public synchronized void goalBlue(boolean isAI) {
        this.scoreBlue += 1;
        this.infoCode = isAI ? 3 : 31;
        this.isDirty = true;
    }

    public synchronized void addScoreYellow(int score) {
        this.scoreYellow += score;
        this.infoCode = 0;
        this.isDirty = true;
    }

    public synchronized void addScoreBlue(int score) {
        this.scoreBlue += score;
        this.infoCode = 0;
        this.isDirty = true;
    }

    public synchronized void setAiLevel(TeamColor color, int level) {
        if (color.isYellow()) {
            this.yellowAiLevel = level;
        } else {
            this.blueAiLevel = level;
        }
    }

    public synchronized void setGameID(String gameID) {
        this.gameID = gameID;
        this.infoCode = 0;
        this.isDirty = true;
    }

    public synchronized void setGameDuration(int gameDuration) {
        this.gameDuration = gameDuration;
        this.infoCode = 0;
        this.isDirty = true;
    }

    public synchronized void setInfoCode(int infoCode) {
        this.infoCode = infoCode;
        this.isDirty = true;
    }

    public synchronized void reset() {
        this.scoreYellow = 0;
        this.scoreBlue = 0;
        this.blueAiLevel = 1;
        this.yellowAiLevel = 1;
        this.gameID = "";
        this.infoCode = 0;
        this.isDirty = true;
    }

    public synchronized void startGame() {
        int random = (int) (FastMath.random() * 100);
        this.gameID = random + "game";
        this.scoreYellow = 0;
        this.scoreBlue = 0;
        this.infoCode = 0;
        this.isDirty = true;
    }

    // Getter メソッド
    public synchronized int getScoreYellow() {
        return scoreYellow;
    }

    public synchronized int getScoreBlue() {
        return scoreBlue;
    }

    public synchronized int getAiLevel(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowAiLevel;
        } else {
            return this.blueAiLevel;
        }
    }

    public synchronized String getGameID() {
        return gameID;
    }

    public synchronized int getGameDuration() {
        return gameDuration;
    }

    public synchronized int getInfoCode() {
        return infoCode;
    }

    public synchronized boolean isDirty() {
        return isDirty;
    }

    public synchronized void resetDirty() {
        this.isDirty = false;
    }

    // リスナーインターフェース
    public interface ScoreUpdateListener {
        void onScoreUpdate();
        void addSession(Session session);
        void removeSession(Session session);
    }
}
