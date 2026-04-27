package ai_server_cafe.updater;

import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import ai_server_cafe.util.interfaces.WrapperWeakCloneableList;
import ai_server_cafe.util.interfaces.WrapperWeakCloneableMap;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UpdaterPassTarget {
    private static UpdaterPassTarget instance = null;

    // パスターゲット候補地点[候補点, スコア]
    private Map<Vector2D, Double> blueScoreMap;
    private Map<Vector2D, Double> yellowScoreMap;
    // 最終的なパスターゲット
    private List<Vector2D> bluePassTargets;
    private List<Vector2D> yellowPassTargets;
    // パスターゲットをロックするか？
    private boolean blueTargetLocked;
    private boolean yellowTargetLocked;
    // パス開始地点
    private Vector2D blueKickerPos;
    private Vector2D yellowKickerPos;
    // ペナルティアリア近くを除外するか?
    private boolean blueIsWidePenaltyArea;
    private boolean yellowIsWidePenaltyArea;

    private Map<Vector2D, Double> replayBlueScoreMap;
    private Map<Vector2D, Double> replayYellowScoreMap;
    private List<Vector2D> replayBluePassTargets;
    private List<Vector2D> replayYellowPassTargets;
    private Optional<Vector2D> replayBlueKickerPos;
    private Optional<Vector2D> replayYellowKickerPos;

    private UpdaterPassTarget() {
        this.blueScoreMap = new HashMap<>();
        this.yellowScoreMap = new HashMap<>();
        this.bluePassTargets = new ArrayList<>();
        this.yellowPassTargets = new ArrayList<>();
        this.blueTargetLocked = false;
        this.yellowTargetLocked = true;
        this.blueKickerPos = Vector2D.ZERO;
        this.yellowKickerPos = Vector2D.ZERO;
        this.blueIsWidePenaltyArea = false;
        this.yellowIsWidePenaltyArea = false;

        this.replayBlueScoreMap = new HashMap<>();
        this.replayYellowScoreMap = new HashMap<>();
        this.replayBluePassTargets = new ArrayList<>();
        this.replayYellowPassTargets = new ArrayList<>();
        this.replayBlueKickerPos = Optional.empty();
        this.replayYellowKickerPos = Optional.empty();
    }

    /**
     * インスタンスファクトリ
     * @return instance
     */
    public synchronized static UpdaterPassTarget getInstance() {
        if (instance == null) {
            instance = new UpdaterPassTarget();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    synchronized public void replayStop() {
        this.replayBlueScoreMap.clear();
        this.replayYellowScoreMap.clear();
        this.replayBluePassTargets.clear();
        this.replayYellowPassTargets.clear();
        this.replayBlueKickerPos = Optional.empty();
        this.replayYellowKickerPos = Optional.empty();
    }

    /* ========================= 以下 setter =========================*/

    synchronized public void setScoreMap(Map<Vector2D, Double> map, TeamColor color) {
        if (color.isYellow()) {
            this.yellowScoreMap = map;
        } else {
            this.blueScoreMap = map;
        }
    }

    synchronized public void setPassTargets(List<Vector2D> positions, TeamColor color) {
        if (color.isYellow()) {
            this.yellowPassTargets = positions;
        } else {
            this.bluePassTargets = positions;
        }
    }

    synchronized public void setTargetLocked(boolean locked, TeamColor color) {
        if (color.isYellow()) {
            this.yellowTargetLocked = locked;
        } else {
            this.blueTargetLocked = locked;
        }
    }

    synchronized public void setKickerPos(Vector2D pos, TeamColor color) {
        if (color.isYellow()) {
            this.yellowKickerPos = pos;
        } else {
            this.blueKickerPos = pos;
        }
    }

    synchronized public void setIsWidePenaltyArea(boolean wide, TeamColor color) {
        if (color.isYellow()) {
            this.yellowIsWidePenaltyArea = wide;
        } else {
            this.blueIsWidePenaltyArea = wide;
        }
    }

    synchronized public void setReplayScoreMap(Map<Vector2D, Double> map, TeamColor color) {
        if (color.isYellow()) {
            this.replayYellowScoreMap = map;
        } else {
            this.replayBlueScoreMap = map;
        }
    }

    synchronized public void setReplayPassTargets(List<Vector2D> positions, TeamColor color) {
        if (color.isYellow()) {
            this.replayYellowPassTargets = positions;
        } else {
            this.replayBluePassTargets = positions;
        }
    }

    synchronized public void setReplayKickerPos(Vector2D pos, TeamColor color) {
        if (color.isYellow()) {
            this.replayYellowKickerPos = Optional.of(pos);
        } else {
            this.replayBlueKickerPos = Optional.of(pos);
        }
    }

    /* ========================= 以下 getter =========================*/

    synchronized public WrapperWeakCloneableMap<Vector2D, Double> getScoreMap(TeamColor color) {
        final Map<Vector2D, Double> result;
        if (color.isYellow()) {
            if(this.replayYellowScoreMap.isEmpty()) result = this.yellowScoreMap;
            else result = this.replayYellowScoreMap;
        } else {
            if(this.replayBlueScoreMap.isEmpty()) result = this.blueScoreMap;
            else result = this.replayBlueScoreMap;
        }
        return new WrapperWeakCloneableMap<>(result);
    }

    synchronized public WrapperWeakCloneableList<Vector2D> getPassTargets(TeamColor color) {
        final List<Vector2D> result;
        if (color.isYellow()) {
            if(this.replayYellowPassTargets.isEmpty()) result = this.yellowPassTargets;
            else result = this.replayYellowPassTargets;
        } else {
            if(this.replayBluePassTargets.isEmpty()) result = this.bluePassTargets;
            else result = this.replayBluePassTargets;
        }
        return new WrapperWeakCloneableList<>(result);
    }

    synchronized public boolean getTargetLocked(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowTargetLocked;
        } else {
            return this.blueTargetLocked;
        }
    }

    synchronized public WrapperWeakCloneable<Vector2D> getKickerPos(TeamColor color) {
        final Vector2D result;
        if (color.isYellow()) {
            if(this.replayYellowKickerPos.isEmpty()) result = this.yellowKickerPos;
            else result = this.replayYellowKickerPos.get();
        } else {
            if(this.replayBlueKickerPos.isEmpty()) result = this.blueKickerPos;
            else result = this.replayBlueKickerPos.get();
        }
        return new WrapperWeakCloneable<>(result);
    }

    synchronized public boolean getIsWidePenaltyArea(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowIsWidePenaltyArea;
        } else {
            return this.blueIsWidePenaltyArea;
        }
    }

}
