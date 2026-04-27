package ai_server_cafe.records.ver1;

import ai_server_cafe.game.planner.path.PathSide;
import org.apache.commons.math3.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * ReplayとRecordDataのsetterおよびgetterとして使用するクラス
 */
public class PathPlannerWrapper {
    private final List<PathSide> pathSide;
    private final double step;
    private final boolean hasResult;

    public PathPlannerWrapper(List<PathSide> pathSide, double step, boolean hasResult) {
        this.pathSide = pathSide;
        this.step = step;
        this.hasResult = hasResult;
    }

    public List<PathSide> getPathSide() {
        return this.pathSide;
    }

    public double getStep() {
        return this.step;
    }

    public boolean getResult() {
        return this.hasResult;
    }

    public Map<Integer, Pair<List<PathSide>, Boolean>> getPath(int id) {
        final Map<Integer, Pair<List<PathSide>, Boolean>> result = new HashMap<>();
        result.put(id, new Pair<>(this.pathSide, this.hasResult));
        return result;
    }
}
