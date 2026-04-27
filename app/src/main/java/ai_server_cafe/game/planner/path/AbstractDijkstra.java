package ai_server_cafe.game.planner.path;

import ai_server_cafe.model.field.obstacle.IntegratedObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ダイクストラ法の基本的な処理
 * Simple is the Best
 */
public abstract class AbstractDijkstra extends AbstractPathPlanner {
    protected AbstractDijkstra(String name) {
        super(name);
    }

    @Nonnull
    protected Optional<List<PathSide>> getPath(@Nonnull Vector2D start, Vector2D goal,
                                               @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
                                               @Nonnull List<IntegratedObstacle> integratedObstacles, double step,
                                               double margin, int nodeDepth, int additionalDepth, double limit,
                                               double maxAdditional, double timeLimit, double updatedTime,
                                               @Nonnull LinkedHashMap<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> confirmedPath,
                                               @Nonnull LinkedHashMap<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> bufferedPath,
                                               @Nonnull List<AbstractObstacle> obstacles,
                                               @Nonnull Map<Vector2D, Optional<List<Vector2D>>> olvList) {
        if (start.subtract(goal).getNorm() <= margin) {
            return Optional.of(List.of(new PathSide(goal.subtract(start), Vector2D.ZERO)));
        }
        if (confirmedPath.isEmpty()) {
            confirmedPath.put(start, new Pair<>(List.of(new Pair<>(start, 0.0)), 0.0));
        }
        // Main Dish
        final double ANGLE_LIMIT = this.angleLimit;

        Vector2D lastConfirmedPoint = confirmedPath.lastEntry().getKey();
        List<Pair<Vector2D, Double>> lastConfirmedPath = confirmedPath.lastEntry().getValue().getFirst();
        double prevScore = confirmedPath.lastEntry().getValue().getSecond();
        if (!olvList.containsKey(lastConfirmedPoint)) {
            olvList.put(lastConfirmedPoint,
                    this.getTangentPointsFromGoal(lastConfirmedPoint, List.of(new Pair<>(goal, Optional.empty())),
                            new ArrayList<>(), integratedObstacles, step * this.deltaRatio, this.tangentSearchDepth, limit,
                            timeLimit * this.timeLimitSplitRatio, updatedTime, new ArrayList<>()));
        }
        Optional<List<Vector2D>> olv = olvList.get(lastConfirmedPoint);
        if (olv.isEmpty()) {
            if (!this.isLargeAngle(lastConfirmedPath, lastConfirmedPoint, goal, ANGLE_LIMIT, margin)) {
                List<Pair<Vector2D, Double>> newPath = new ArrayList<>(lastConfirmedPath);
                double baseLength = 0.0;
                if (lastConfirmedPath.size() >= 2) {
                    baseLength =
                            this.getMaxAdditionalLength(lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                                    lastConfirmedPath.getLast().getFirst(), goal, obstacles, maxAdditional, step);
                } else if (lastConfirmedPath.size() == 1 && vel.isPresent()) {
                    baseLength = this.getMaxAdditionalLength(
                            lastConfirmedPath.getLast().getFirst().subtract(vel.get().scalarMultiply(0.016)),
                            lastConfirmedPath.getLast().getFirst(), goal, obstacles, maxAdditional, step);
                }
                newPath.addLast(new Pair<>(goal, baseLength));
                double newScore = this.getScore(this.convertPath(newPath, this.scoreSmoothing ? obstacles : new ArrayList<>(), step, maxAdditional, vel), vel, false);
                this.checkScore(prevScore, newScore);
                if (bufferedPath.containsKey(goal)) {
                    if (bufferedPath.get(goal).getSecond() > newScore) {
                        bufferedPath.put(goal, new Pair<>(newPath, newScore));
                    }
                } else {
                    bufferedPath.put(goal, new Pair<>(newPath, newScore));
                }
            }
        } else {
            List<Vector2D> nextStarts = olv.get();
            if (nextStarts.isEmpty()) {
                // 引いてだめなら押してみろ (訳：時間切れかゴールが障害物に囲まれているとき)
                Optional<List<Vector2D>> olvStart =
                        this.getTangentPointsFromStart(lastConfirmedPoint, List.of(new Pair<>(goal, Optional.empty())),
                                new ArrayList<>(), integratedObstacles, step * this.deltaRatio, this.tangentSearchDepth, limit,
                                timeLimit * 2.0 * this.timeLimitSplitRatio, updatedTime, new ArrayList<>());
                olvList.put(lastConfirmedPoint, olvStart);
                if (olvStart.isEmpty()) {
                    if (!this.isLargeAngle(lastConfirmedPath, lastConfirmedPoint, goal, ANGLE_LIMIT, margin)) {
                        List<Pair<Vector2D, Double>> newPath = new ArrayList<>(lastConfirmedPath);
                        double baseLength = 0.0;
                        if (lastConfirmedPath.size() >= 2) {
                            baseLength = this.getMaxAdditionalLength(
                                    lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                                    lastConfirmedPath.getLast().getFirst(), goal, obstacles, maxAdditional, step);
                        } else if (lastConfirmedPath.size() == 1 && vel.isPresent()) {
                            baseLength = this.getMaxAdditionalLength(
                                    lastConfirmedPath.getLast().getFirst().subtract(vel.get().scalarMultiply(0.016)),
                                    lastConfirmedPath.getLast().getFirst(), goal, obstacles, maxAdditional, step);
                        }
                        newPath.addLast(new Pair<>(goal, baseLength));
                        double newScore = this.getScore(this.convertPath(newPath, this.scoreSmoothing ? obstacles : new ArrayList<>(), step, maxAdditional, vel), vel, false);
                        this.checkScore(prevScore, newScore);
                        if (bufferedPath.containsKey(goal)) {
                            if (bufferedPath.get(goal).getSecond() > newScore) {
                                bufferedPath.put(goal, new Pair<>(newPath, newScore));
                            }
                        } else {
                            bufferedPath.put(goal, new Pair<>(newPath, newScore));
                        }
                    }
                } else if (!olvStart.get().isEmpty()) {
                    nextStarts.addAll(olvStart.get());
                }
            }
            label:
            for (Vector2D nextStart : nextStarts) {
                if (this.containsPos(lastConfirmedPath, margin, nextStart)) {
                    continue;
                }
                if (this.isLargeAngle(lastConfirmedPath, lastConfirmedPoint, nextStart, ANGLE_LIMIT, margin)) {
                    continue;
                }
                Map<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> map = new HashMap<>();
                for (Vector2D key : bufferedPath.keySet()) {
                    if (this.containsPos(nextStart, key, margin)) {
                        map.put(key, bufferedPath.get(key));
                    }
                }
                for (Vector2D key : confirmedPath.keySet()) {
                    if (this.containsPos(nextStart, key, margin)) {
                        continue label;
                    }
                }
                List<Pair<Vector2D, Double>> newPath0 = new ArrayList<>(lastConfirmedPath);
                if (!map.isEmpty()) {
                    for (Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> containedPos : map.entrySet()) {
                        List<Pair<Vector2D, Double>> newPath = new ArrayList<>(newPath0);
                        double baseLength = 0.0;
                        if (lastConfirmedPath.size() >= 2) {
                            baseLength = this.getMaxAdditionalLength(
                                    lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                                    lastConfirmedPath.getLast().getFirst(), containedPos.getKey(), obstacles,
                                    maxAdditional, step);
                        } else if (lastConfirmedPath.size() == 1 && vel.isPresent()) {
                            baseLength = this.getMaxAdditionalLength(
                                    lastConfirmedPath.getLast().getFirst().subtract(vel.get().scalarMultiply(0.016)),
                                    lastConfirmedPath.getLast().getFirst(), containedPos.getKey(), obstacles,
                                    maxAdditional, step);
                        }
                        newPath.addLast(new Pair<>(containedPos.getKey(), baseLength));
                        double newScore = this.getScore(this.convertPath(newPath, this.scoreSmoothing ? obstacles : new ArrayList<>(), step, maxAdditional, vel), vel, false);
                        this.checkScore(prevScore, newScore);
                        if (containedPos.getValue().getSecond() > newScore) {
                            bufferedPath.put(containedPos.getKey(), new Pair<>(newPath, newScore));
                        }
                    }
                } else {
                    double baseLength = 0.0;
                    if (lastConfirmedPath.size() >= 2) {
                        baseLength = this.getMaxAdditionalLength(
                                lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                                lastConfirmedPath.getLast().getFirst(), nextStart, obstacles, maxAdditional, step);
                    } else if (lastConfirmedPath.size() == 1 && vel.isPresent()) {
                        baseLength = this.getMaxAdditionalLength(
                                lastConfirmedPath.getLast().getFirst().subtract(vel.get().scalarMultiply(0.016)),
                                lastConfirmedPath.getLast().getFirst(), nextStart, obstacles, maxAdditional, step);
                    }
                    newPath0.addLast(new Pair<>(nextStart, baseLength));
                    double newScore = this.getScore(this.convertPath(newPath0, this.scoreSmoothing ? obstacles : new ArrayList<>(), step, maxAdditional, vel), vel, false);
                    this.checkScore(prevScore, newScore);
                    bufferedPath.put(nextStart, new Pair<>(newPath0, newScore));
                }
            }
        }

        Optional<Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>> minScore = Optional.empty();
        while (minScore.isEmpty()) {
            if (bufferedPath.isEmpty()) {
                break;
            }
            minScore = bufferedPath.entrySet().stream().min(InterfaceHelper.getComparator(
                    new IFuncParam1<Double, Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>>() {
                        @Override
                        public Double function(
                                Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> vector2DPairEntry) {
                            return vector2DPairEntry.getValue().getSecond();
                        }
                    }));
            if (!confirmedPath.containsKey(minScore.get().getKey()) || confirmedPath.get(minScore.get().getKey()).getSecond() > minScore.get().getValue().getSecond()) {
                confirmedPath.put(minScore.get().getKey(), minScore.get().getValue());
            }
            bufferedPath.remove(minScore.get().getKey());
            if (minScore.get().getKey() == goal) {
                minScore = Optional.empty();
            }
        }

        if (confirmedPath.containsKey(goal)) {
            additionalDepth--;
            if (nodeDepth <= 0 || additionalDepth <= 0 || minScore.isEmpty()) {
                return Optional.of(this.convertPath(confirmedPath.get(goal).getFirst(), this.outputSmoothing ? obstacles : new ArrayList<>(), step, maxAdditional, vel));
            }
        }

        if (minScore.isEmpty() || nodeDepth <= 0) {
            Optional<Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>> minValue =
                    confirmedPath.entrySet().stream().min(InterfaceHelper.getComparator(
                            new IFuncParam1<Double, Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>>() {
                                @Override
                                public Double function(
                                        Map.Entry<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> vector2DPairEntry) {
                                    return vector2DPairEntry.getKey().subtract(goal).getNormSq();
                                }
                            }));
            // どうしようもない
            if (minValue.isEmpty() || minValue.get().getValue().getFirst().size() <= 1) {
                Vector2D maxVec =
                        this.getFirstCollidedPos(start, goal, integratedObstacles, step, limit, timeLimit, updatedTime);
                if (maxVec.subtract(start).getNormSq() < MathHelper.EPSILON) {
                    return Optional.of(new ArrayList<>());
                } else {
                    // 限界まで近づく
                    return Optional.of(List.of(new PathSide(maxVec.subtract(start), Vector2D.ZERO)));
                }
            }
            return Optional.of(this.convertPath(minValue.get().getValue().getFirst(), this.outputSmoothing ? obstacles : new ArrayList<>(), step, maxAdditional, vel));
        }
        return this.getPath(start, goal, vel, integratedObstacles, step, margin, nodeDepth, additionalDepth, limit,
                maxAdditional, timeLimit, updatedTime, confirmedPath, bufferedPath, obstacles, olvList);
    }

    /**
     * ダイクストラ法のコスト
     * @param path 経路
     * @param vel 初速度
     * @param ignoreVelToObstacle 障害物への速度を無視するか(ペナルティ項をつけるかどうか)
     * @return スコア
     */
    public abstract double getScore(List<PathSide> path,
                                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel, boolean ignoreVelToObstacle);

    protected Optional<List<PathSide>> getPath(Vector2D start, Vector2D goal,
                                               Optional<Vector2D> vel,
                                               List<IntegratedObstacle> integratedObstacles, double step, double margin,
                                               int depth, int additionalDepth, double maxPathLength,
                                               double maxAdditional, double timeLimit, double now,
                                               List<AbstractObstacle> obstacles) {
        return this.getPath(start, goal, vel, integratedObstacles, step, margin, depth, additionalDepth, maxPathLength,
                maxAdditional, timeLimit, now, new LinkedHashMap<>(), new LinkedHashMap<>(), obstacles,
                new HashMap<>());
    }

    protected void checkScore(double prevScore, double newScore) {

    }
}
