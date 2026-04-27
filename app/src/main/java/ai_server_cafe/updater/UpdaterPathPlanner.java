package ai_server_cafe.updater;

import ai_server_cafe.game.planner.path.DataSet;
import ai_server_cafe.game.planner.path.EnumPathPlannerType;
import ai_server_cafe.game.planner.path.PathSide;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.MapLikeRobotList;
import ai_server_cafe.util.interfaces.WrapperCloneableList;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UpdaterPathPlanner {
    private static UpdaterPathPlanner instance = null;
    private final MapLikeRobotList<List<PathSide>> pathMap;
    private final MapLikeRobotList<Double> stepMap;
    private final MapLikeRobotList<Boolean> hasResult;
    private final MapLikeRobotList<DataSet> dataSet;
    private final MapLikeRobotList<EnumPathPlannerType> type;
    private final MapLikeRobotList<Vector2D> nextVel;

    private final MapLikeRobotList<List<PathSide>> replayPathMap;
    private final MapLikeRobotList<Boolean> replayHasResult;

    private UpdaterPathPlanner() {
        this.pathMap = new MapLikeRobotList<>();
        this.hasResult = new MapLikeRobotList<>();
        this.dataSet = new MapLikeRobotList<>();
        this.stepMap = new MapLikeRobotList<>();
        this.type = new MapLikeRobotList<>();

        this.replayPathMap = new MapLikeRobotList<>();
        this.replayHasResult = new MapLikeRobotList<>();
        this.nextVel = new MapLikeRobotList<>();
    }

    public synchronized static UpdaterPathPlanner getInstance() {
        if (instance == null) {
            instance = new UpdaterPathPlanner();
        }
        return instance;
    }

    /**
     * @implNote replayPathMapが存在したら、replayPathMapを返す
     */
    @Nonnull
    synchronized public WrapperCloneableList<PathSide> getResult(@Nonnull TeamColor color, int id) {
        if(this.replayPathMap.isEmpty(color)) {
            if (this.pathMap.containsKey(color, id)) {
                return new WrapperCloneableList<>(this.pathMap.get(color, id));
            }
        } else {
            if(this.replayPathMap.containsKey(color, id)) {
                return new WrapperCloneableList<>(this.replayPathMap.get(color, id));
            }
        }
        return new WrapperCloneableList<>(new ArrayList<>());
    }

    /**
     * @implNote replayHasResultMapが存在したら、replayHasResultMapを返す
     */
    synchronized public boolean hasResult(@Nonnull TeamColor color, int id) {
        if(this.replayHasResult.isEmpty(color)) return this.hasResult.containsKey(color, id) && this.hasResult.get(color, id);
        return this.replayHasResult.containsKey(color, id) && this.replayHasResult.get(color, id);
    }

    public synchronized void setResult(@Nonnull TeamColor color, @Nonnull Map<Integer, Pair<List<PathSide>, Boolean>> path) {
        for (int id : path.keySet()) {
            if (path.get(id).getSecond()) this.pathMap.put(color, id, path.get(id).getFirst());
            this.hasResult.put(color, id, path.get(id).getSecond());
        }
    }

    public synchronized void setReplayResult(TeamColor color, @Nonnull Map<Integer, Pair<List<PathSide>, Boolean>> path) {
        for (int id : path.keySet()) {
            if(path.get(id).getSecond()) this.replayPathMap.put(color, id, path.get(id).getFirst());
            this.replayHasResult.put(color, id, path.get(id).getSecond());
        }
    }

    public synchronized void setDataset(TeamColor color, int id, DataSet set, EnumPathPlannerType type) {
        this.dataSet.put(color, id, set);
        this.type.put(color, id, type);
    }

    @Nonnull
    public synchronized WrapperWeakCloneable<Pair<DataSet, EnumPathPlannerType>> getDataset(TeamColor color, int id) {
        return new WrapperWeakCloneable<>(new Pair<>(this.dataSet.get(color, id), this.type.get(color, id)));
    }

    public synchronized boolean hasDataset(TeamColor color, int id) {
        return this.dataSet.containsKey(color, id);
    }

    public synchronized boolean containsKeyStep(TeamColor color, int id) {
        return this.stepMap.containsKey(color, id);
    }

    public synchronized double getStep(TeamColor color, int id, double orDefault) {
        return this.stepMap.containsKey(color, id) ? this.stepMap.get(color, id) : orDefault;
    }

    @Nonnull
    public synchronized WrapperWeakCloneable<Vector2D> getNextVel(TeamColor color, int id) {
        return new WrapperWeakCloneable<>(this.nextVel.containsKey(color, id) ? this.nextVel.get(color, id) : Vector2D.ZERO);
    }

    public synchronized void setNextVel(TeamColor color, int id, Vector2D vector2D) {
        this.nextVel.put(color, id, vector2D);
    }

    public synchronized void setStep(TeamColor color, int id, double step) {
        this.stepMap.put(color, id, step);
    }

    public static void reset() {
        instance = null;
    }

    public synchronized void replayStop() {
        this.replayPathMap.clear();
        this.replayHasResult.clear();
    }
}
