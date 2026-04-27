package ai_server_cafe.game.planner.thread;

import ai_server_cafe.game.planner.path.DataSet;
import ai_server_cafe.game.planner.path.EnumPathPlannerType;
import ai_server_cafe.game.planner.path.PathSide;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AxisAlignedBoundingBox;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterPathPlanner;
import ai_server_cafe.updater.UpdaterThreadStatus;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ThreadPathPlanner extends AbstractLoopThreadCafe {
    private double lastTime;
    private final TeamColor color;
    private static ThreadPathPlanner instanceYellow;
    private static ThreadPathPlanner instanceBlue;

    private ThreadPathPlanner(@Nonnull TeamColor color) {
        super("thread-path-planner:" + color.name());
        this.color = color;
        this.lastTime = 0.0;
    }

    @Nonnull
    public static ThreadPathPlanner getInstance(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            if (instanceYellow == null) {
                instanceYellow = new ThreadPathPlanner(color);
            }
            return instanceYellow;
        }
        if (instanceBlue == null) {
            instanceBlue = new ThreadPathPlanner(color);
        }
        return instanceBlue;
    }

    @Override
    protected void loop() {
        double nowDate = TimeHelper.now();
        if (nowDate - this.lastTime > ConfigManager.getInstance().getCycleTime()) {
            this.lastTime = nowDate;
            // TPS計算用データを追加
            UpdaterThreadStatus.getInstance().addTPSData(this.name, nowDate);
            Map<Integer, Pair<DataSet, EnumPathPlannerType>> dataMap = new HashMap<>();
            label:
            for (int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
                if (!UpdaterPathPlanner.getInstance().hasDataset(this.color, id)) continue;
                // DataSetを持ってくる
                Pair<DataSet, EnumPathPlannerType> data = UpdaterPathPlanner.getInstance().getDataset(this.color, id).get();
                // 安全対策始め
                double height = ConfigManager.getInstance().getConfig().maxFieldHeight;
                double width = ConfigManager.getInstance().getConfig().maxFieldWidth;
                AxisAlignedBoundingBox aabb = new AxisAlignedBoundingBox(new Vector2D(-width, -height), new Vector2D(width, height));
                for (AbstractObstacle obstacle : data.getFirst().obstacles) {
                    // 中に収まっていない
                    if (!aabb.isPerfectlyIncluded(obstacle.getAABB())) {
                        continue label;
                    }
                }
                if (aabb.isExcluded(data.getFirst().start)) {
                    continue;
                }
                if (aabb.isExcluded(data.getFirst().goal)) {
                    continue;
                }
                // 安全対策終わり
                dataMap.put(id, new Pair<>(data.getFirst().clone(), data.getSecond()));
            }
            Map<Integer, Pair<List<PathSide>, Boolean>> resultMap = new HashMap<>();
            for (int id : dataMap.keySet()) {
                // DataSetをplannerに通す
                Optional<Pair<List<PathSide>, Vector2D>> path = dataMap.get(id).getSecond().getCalculator().function(dataMap.get(id).getFirst());
                resultMap.put(id, new Pair<>(path.orElse(new Pair<>(new ArrayList<>(), Vector2D.ZERO)).getFirst(), path.isPresent()));
            }
            // 結果全体を更新
            UpdaterPathPlanner.getInstance().setResult(this.color, resultMap);
        }
    }

    @Override
    protected void init() {
    }

    public static void reset() {
        instanceBlue = null;
        instanceYellow = null;
    }
}
