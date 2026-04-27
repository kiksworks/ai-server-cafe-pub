package ai_server_cafe.scoreboard.host_board;

import ai_server_cafe.scoreboard.host_board.protocol.GameDataJson;
import ai_server_cafe.updater.UpdaterScoreBoard;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreSender implements UpdaterScoreBoard.ScoreUpdateListener {
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    public ScoreSender() {
    }

    @Override
    public void onScoreUpdate() {
        broadcastGameData();
    }

    private void broadcastGameData() {
        Gson gson = new Gson();
        UpdaterScoreBoard scoreBoard = UpdaterScoreBoard.getInstance();
        GameDataJson gameData = new GameDataJson(scoreBoard.getGameID(), scoreBoard.getGameDuration(), scoreBoard.getScoreYellow(), scoreBoard.getScoreBlue(), scoreBoard.getInfoCode());
        String data = gson.toJson(gameData);

        for (Session session : this.sessions) {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendString(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addSession(Session session) {
        this.sessions.add(session);
        broadcastGameData();
    }

    @Override
    public void removeSession(Session session) {
        this.sessions.remove(session);
    }
}
