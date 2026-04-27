package ai_server_cafe.game.planner.path;

import ai_server_cafe.model.field.obstacle.IntegratedObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ベルマンフォード法の改良版
 * 気合の全探索
 */
public abstract class AbstractRoundRobin extends AbstractPathPlanner {

    protected AbstractRoundRobin(String name) {
        super(name);
    }

    /**
     * 基準となるスコア
     * @param path 経路
     * @param vel 初速度
     * @return スコア
     */
    protected abstract double getScore(List<PathSide> path,
                                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel);

    @Override
    protected Optional<List<PathSide>> getPath(Vector2D start, Vector2D goal, Optional<Vector2D> vel,
                                               List<IntegratedObstacle> integratedObstacles, double step, double margin,
                                               int depth, int additionalDepth, double maxPathLength,
                                               double maxAdditional, double timeLimit, double now,
                                               List<AbstractObstacle> obstacles) {
        Map<Vector2D, List<Vector2D>> map =
                this.getTangentPoints(start, goal, integratedObstacles, depth, margin, step, maxPathLength, timeLimit,
                        now);
        Map<Vector2D, List<LinkedHashMap<Vector2D, Double>>> pathMap = new HashMap<>();
        List<LinkedHashMap<Vector2D, Double>> lastPaths = new ArrayList<>();
        lastPaths.add(new LinkedHashMap<>());
        lastPaths.getFirst().put(start, 0.0);
        while (true) {
            List<LinkedHashMap<Vector2D, Double>> copied = new ArrayList<>(lastPaths);
            if (copied.isEmpty() || TimeHelper.now() - now > timeLimit) break;
            lastPaths.clear();
            for (LinkedHashMap<Vector2D, Double> path : copied) {
                Vector2D s0 = path.lastEntry().getKey();
                if (!map.containsKey(s0)) continue;
                for (Vector2D next : map.get(s0)) {
                    LinkedHashMap<Vector2D, Double> newPath = new LinkedHashMap<>(path);
                    if (path.size() < 2) {
                        newPath.put(next, 0.0);
                    } else {
                        if (path.containsKey(next)) continue;
                        Vector2D lastStart = MathHelper.get(path, path.size() - 2).getKey();
                        if (this.isLargeAngle(lastStart, s0, next, this.angleLimit)) {
                            continue;
                        }
                        double baseLength =
                                this.getMaxAdditionalLength(lastStart, s0, next, obstacles, maxAdditional, step);
                        newPath.put(s0, baseLength);
                        newPath.put(next, 0.0);
                    }
                    lastPaths.add(newPath);
                    if (!pathMap.containsKey(next)) {
                        pathMap.put(next, new ArrayList<>());
                    }
                    pathMap.get(next).add(newPath);
                }
            }
        }
        Vector2D key;
        if (pathMap.containsKey(goal)) {
            key = goal;
        } else {
            Optional<Vector2D> vector2D = pathMap.keySet().stream()
                    .min(InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
                        @Override
                        public Double function(Vector2D vector2D) {
                            return vector2D.subtract(goal).getNorm();
                        }
                    }));
            if (vector2D.isEmpty()) return Optional.of(new ArrayList<>());
            else {
                key = vector2D.get();
            }
        }
        List<List<PathSide>> optimalPaths = new ArrayList<>();
        for (LinkedHashMap<Vector2D, Double> path : pathMap.get(key)) {
            optimalPaths.add(this.convertPath(this.makeOptimal(path)));
        }
        Optional<List<PathSide>> optimal = optimalPaths.stream()
                .min(InterfaceHelper.getComparator(new IFuncParam1<Double, List<PathSide>>() {
                    @Override
                    public Double function(List<PathSide> pathSides) {
                        return getScore(pathSides, vel);
                    }
                }));
        if (optimal.isEmpty()) {
            Vector2D maxVec =
                    this.getFirstCollidedPos(start, goal, integratedObstacles, step, maxPathLength, timeLimit, now);
            if (maxVec.subtract(start).getNormSq() < MathHelper.EPSILON) {
                return Optional.of(new ArrayList<>());
            } else {
                // 限界まで近づく
                return Optional.of(List.of(new PathSide(maxVec.subtract(start), Vector2D.ZERO)));
            }
        } else {
            return optimal;
        }
    }

    @Nonnull
    public Map<Vector2D, List<Vector2D>> getTangentPoints(Vector2D start, Vector2D goal,
                                                          List<IntegratedObstacle> integratedObstacles, int nodeDepth,
                                                          double margin, double step, double limit, double timeLimit,
                                                          double updatedTime) {
        Map<Vector2D, Map.Entry<List<Vector2D>, List<Vector2D>>> map = new HashMap<>();
        List<Vector2D> nextStarts = new ArrayList<>(); // pathじゃなきゃだめ, pathにcontainsなら捨てる
        nextStarts.add(start);
        if (this.containsPos(start, goal, margin)) {
            map.put(start, new AbstractMap.SimpleEntry<>(List.of(goal), List.of()));
            Map<Vector2D, List<Vector2D>> resultMap = new HashMap<>();
            for (Vector2D key : map.keySet()) {
                resultMap.put(key, map.get(key).getKey());
            }
            return resultMap;
        }
        while (true) {
            List<Vector2D> starts = new ArrayList<>(nextStarts);
            nextStarts.clear();
            if (nodeDepth <= 0 || starts.isEmpty()) break;
            for (Vector2D s0 : starts) {
                if (s0 == goal) {
                    continue;
                }
                List<Vector2D> nextStartsBuffer = new ArrayList<>();
                Optional<List<Vector2D>> olv =
                        this.getTangentPointsFromGoal(s0, List.of(new Pair<>(goal, Optional.empty())),
                                new ArrayList<>(), integratedObstacles, step * this.deltaRatio, this.tangentSearchDepth, limit,
                                timeLimit * this.timeLimitSplitRatio, updatedTime, new ArrayList<>());
                if (olv.isEmpty()) {
                    nextStartsBuffer.add(goal);
                } else if (olv.get().isEmpty()) {
                    Optional<List<Vector2D>> olvStart =
                            this.getTangentPointsFromStart(s0, List.of(new Pair<>(goal, Optional.empty())),
                                    new ArrayList<>(), integratedObstacles, step * this.deltaRatio, this.tangentSearchDepth,
                                    limit, timeLimit * 2.0 * this.timeLimitSplitRatio, updatedTime, new ArrayList<>());
                    if (olvStart.isEmpty()) {
                        nextStartsBuffer.add(goal);
                    } else {
                        nextStartsBuffer.addAll(olvStart.get());
                    }
                } else {
                    nextStartsBuffer.addAll(olv.get());
                }
                if (!nextStartsBuffer.isEmpty()) {
                    map.put(s0, new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>()));
                }
                for (Vector2D s0Next : nextStartsBuffer) {
                    // if containsOther map.get(other).addNode, map.get(s0).add(other)
                    // else nextStarts.add(s0Next), map.get(s0).add(s0Next)
                    boolean combined = false;
                    for (Map.Entry<Vector2D, Map.Entry<List<Vector2D>, List<Vector2D>>> entry : map.entrySet()) {
                        if (this.containsPos(s0Next, entry.getKey(), margin)) {
                            if (!entry.getValue().getValue().contains(s0)) {
                                entry.getValue().getValue().add(s0);
                            }
                            if (!map.get(s0).getKey().contains(entry.getKey())) {
                                map.get(s0).getKey().add(entry.getKey());
                            }
                            combined = true;
                        }
                    }
                    if (!combined) {
                        if (!map.get(s0).getKey().contains(s0Next)) {
                            map.get(s0).getKey().add(s0Next);
                        }
                        if (!nextStarts.contains(s0Next)) {
                            nextStarts.add(s0Next);
                        }
                    }
                }
            }
            nodeDepth--;
        }
        Map<Vector2D, List<Vector2D>> resultMap = new HashMap<>();
        for (Vector2D key : map.keySet()) {
            resultMap.put(key, map.get(key).getKey());
        }
        return resultMap;
    }
}
