package ai_server_cafe.updater;

import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.MapLikeRobotList;
import ai_server_cafe.util.interfaces.WrapperCloneableList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class UpdaterCurrentObstacle {
    private static UpdaterCurrentObstacle instance = null;
    private final MapLikeRobotList<List<AbstractObstacle>> map;
    private final MapLikeRobotList<List<AbstractObstacle>> replayMap = new MapLikeRobotList<>();

    private UpdaterCurrentObstacle() {
        this.map = new MapLikeRobotList<>();
    }

    public synchronized static UpdaterCurrentObstacle getInstance() {
        if (instance == null) {
            instance = new UpdaterCurrentObstacle();
        }
        return instance;
    }

    public synchronized void put(TeamColor color, int id, List<AbstractObstacle> obstacles) {
        this.map.put(color, id, obstacles);
    }

    @Nonnull
    public synchronized WrapperCloneableList<AbstractObstacle> get(TeamColor color, int id) {
        List<AbstractObstacle> obstacles = (this.replayMap.isEmpty(color) ?
                this.map.get(color, id) : this.replayMap.get(color, id));
        return new WrapperCloneableList<>(obstacles == null ? new ArrayList<>() : new ArrayList<>(obstacles));
    }

    public synchronized void replayStop() {
        this.replayMap.clear();
    }

    public synchronized void putReplay(TeamColor color, int id, List<AbstractObstacle> obstacles) {
        this.replayMap.put(color, id, obstacles);
    }
}
