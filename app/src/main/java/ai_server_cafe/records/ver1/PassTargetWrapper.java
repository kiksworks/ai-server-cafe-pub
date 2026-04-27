package ai_server_cafe.records.ver1;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.List;
import java.util.Map;

/**
 * ReplayとRecordDataのsetterおよびgetterとして使用するクラス
 */
public class PassTargetWrapper {
    private final Map<Vector2D, Double> scoreMap;
    private final List<Vector2D> passTargets;
    private final Vector2D kickerPos;

    public PassTargetWrapper(Map<Vector2D, Double> scoreMap, List<Vector2D> passTargets, Vector2D kickerPos) {
        this.scoreMap = scoreMap;
        this.passTargets = passTargets;
        this.kickerPos = kickerPos;
    }

    public Map<Vector2D, Double> getScoreMap() {
        return this.scoreMap;
    }

    public List<Vector2D> getPassTargets() {
        return this.passTargets;
    }

    public Vector2D getKickerPos() {
        return this.kickerPos;
    }
}
