package ai_server_cafe.game.planner.path;

import ai_server_cafe.model.field.obstacle.IntegratedObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ベルマンフォード法の基本的な処理
 * V^3
 */
public abstract class AbstractBellmanFord extends AbstractPathPlanner {
    private static final boolean ENABLE_TIME_LIMITED_PROCESS = true;

    protected AbstractBellmanFord(String name) {
        super(name);
    }

    @Override
    protected Optional<List<PathSide>> getPath(Vector2D start, Vector2D goal, Optional<Vector2D> vel,
                                               List<IntegratedObstacle> integratedObstacles, double step, double margin,
                                               int depth, int additionalDepth, double maxPathLength,
                                               double maxAdditional, double timeLimit, double now,
                                               List<AbstractObstacle> obstacles) {
        return this.getPath(start, goal, vel, integratedObstacles, maxAdditional, step, margin, depth, maxPathLength, timeLimit, now, obstacles);
    }

    @Nonnull
    public Map<Vector2D, List<Vector2D>> getTangentPoints(Vector2D start, Vector2D goal, List<IntegratedObstacle> integratedObstacles,
                                                                 int nodeDepth, double margin, double step, double limit, double timeLimit, double updatedTime) {
        Map<Vector2D, List<Vector2D>> map = new HashMap<>();
        List<Vector2D> nextStarts = new ArrayList<>(); // pathじゃなきゃだめ, pathにcontainsなら捨てる
        nextStarts.add(start);
        if (this.containsPos(start, goal, margin)) {
            map.put(start, List.of(goal));
            return map;
        }
        while (true) {
            List<Vector2D> starts = new ArrayList<>(nextStarts);
            nextStarts.clear();
            if (nodeDepth <= 0 || starts.isEmpty())
                break;
            for (Vector2D s0 : starts) {
                if (s0 == goal) {
                    continue;
                }
                List<Vector2D> nextStartsBuffer = new ArrayList<>();
                Optional<List<Vector2D>> olv =
                        this.getTangentPointsFromGoal(s0, List.of(new Pair<>(goal, Optional.empty())),
                                new ArrayList<>(), integratedObstacles, step * this.deltaRatio, this.tangentSearchDepth, limit,
                                FastMath.min(timeLimit * this.timeLimitSplitRatio, timeLimit * 0.9), updatedTime, new ArrayList<>());
                if (olv.isEmpty()) {
                    nextStartsBuffer.add(goal);
                } else if (olv.get().isEmpty()) {
                    Optional<List<Vector2D>> olvStart = this.getTangentPointsFromStart(s0,
                            List.of(new Pair<>(goal, Optional.empty())), new ArrayList<>(), integratedObstacles,
                            step * this.deltaRatio, this.tangentSearchDepth, limit, FastMath.min(timeLimit * 2.0 * this.timeLimitSplitRatio, timeLimit * 0.9), updatedTime,
                            new ArrayList<>());
                    if (olvStart.isEmpty()) {
                        nextStartsBuffer.add(goal);
                    } else {
                        nextStartsBuffer.addAll(olvStart.get());
                    }
                } else {
                    nextStartsBuffer.addAll(olv.get());
                }
                if (!nextStartsBuffer.isEmpty()) {
                    map.put(s0, new ArrayList<>());
                }
                for (Vector2D s0Next : nextStartsBuffer) {
                    boolean combined = false;
                    for (Map.Entry<Vector2D, List<Vector2D>> entry : map.entrySet()) {
                        if (this.containsPos(s0Next, entry.getKey(), margin)) {
                            map.get(s0).add(entry.getKey());
                            combined = true;
                        }
                    }
                    if (!combined) {
                        map.get(s0).add(s0Next);
                        nextStarts.add(s0Next);
                    }
                }
            }
            nodeDepth--;
        }
        return map;
    }

    public Optional<List<PathSide>> getPath(@Nonnull Vector2D start, Vector2D goal,
                                                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
                                                       @Nonnull List<IntegratedObstacle> integratedObstacles,
                                                       double maxAdditional, double step,
                                                       double margin, int nodeDepth, double limit,
                                                       double timeLimit, double updatedTime,
                                                       @Nonnull List<AbstractObstacle> obstacles) {
        final double COS_K = step / this.minimumMargin;
        Map<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> map = new HashMap<>();
        Map<Vector2D, List<Vector2D>> graph = this.getTangentPoints(start, goal, integratedObstacles, nodeDepth, margin, step, limit, timeLimit, updatedTime);
        map.put(start, new Pair<>(List.of(new Pair<>(start, 0.0)), 0.0));
        int vertexSize = graph.keySet().size();
        for (int i = 0; i < vertexSize; i++) {
            Map<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> copied = new HashMap<>(map);
            for (Vector2D vertex : copied.keySet()) {
                if (!graph.containsKey(vertex))
                    continue;
                for (Vector2D nextVertex : graph.get(vertex)) {
                    List<Pair<Vector2D, Double>> lastPath = new ArrayList<>(copied.get(vertex).getFirst());
                    if (lastPath.size() >= 2 && this.isLargeAngle(lastPath.get(lastPath.size() - 2).getFirst(), lastPath.getLast().getFirst(), nextVertex, this.angleLimit)) {
                        continue;
                    }
                    double baseLength = 0.0;
                    if (lastPath.size() >= 2) {
                        baseLength = this.getMaxAdditionalLength(
                                lastPath.get(lastPath.size() - 2).getFirst(),
                                lastPath.getLast().getFirst(), nextVertex, obstacles, maxAdditional, step);
                    }
                    lastPath.add(new Pair<>(nextVertex, baseLength));
                    double score = this.getScore(
                            this.scoreSmoothing ? this.convertPath(lastPath, COS_K, obstacles, step, maxAdditional) :
                                    this.convertPath(lastPath, COS_K), vel);
                    if (!map.containsKey(nextVertex)) {
                        map.put(nextVertex, new Pair<>(lastPath, score));
                    } else {
                        if (map.get(nextVertex).getSecond() > score) {
                            map.put(nextVertex, new Pair<>(lastPath, score));
                        }
                    }
                }
            }
            if (TimeHelper.now() - updatedTime > timeLimit && ENABLE_TIME_LIMITED_PROCESS) {
                break;
            }
        }
        if (map.containsKey(goal)) {
            return Optional.of(this.outputSmoothing ?
                    this.convertPath(map.get(goal).getFirst(), obstacles, step, maxAdditional) :
                    this.convertPath(map.get(goal).getFirst()));
        } else {
            Optional<Vector2D> key = map.keySet().stream().min(
                    InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
                        @Override
                        public Double function(Vector2D vector2D) {
                            return vector2D.subtract(goal).getNorm();
                        }
                    }));
            if (key.isEmpty() || map.size() <= 1) {
                return Optional.empty();
            } else {
                return Optional.of(this.outputSmoothing ?
                        this.convertPath(map.get(key.get()).getFirst(), obstacles, step, maxAdditional) :
                        this.convertPath(map.get(key.get()).getFirst()));
            }
        }
    }

    /**
     * 基準となるスコア
     * @param path 経路
     * @param vel 初速度
     * @return スコア
     */
    protected abstract double getScore(List<PathSide> path,
                                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel);
}
