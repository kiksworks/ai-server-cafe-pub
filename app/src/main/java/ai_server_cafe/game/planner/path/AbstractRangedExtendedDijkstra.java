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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Threshold付き拡張Dijkstra
 * 拡張ダイクストラを実装してみたが間違っている可能性高
 */
public abstract class AbstractRangedExtendedDijkstra extends AbstractPathPlanner {
    protected AbstractRangedExtendedDijkstra(String name) {
        super(name);
    }

    @Nonnull
    public Optional<List<PathSide>> getPath(@Nonnull Vector2D start, Vector2D goal,
                                                         @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
                                                         @Nonnull List<IntegratedObstacle> integratedObstacles,
                                                         double step, double margin, int nodeDepth, double limit,
                                                         double maxAdditional, double timeLimit, double updatedTime,
                                                         @Nonnull List<Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>> confirmedPath,
                                                         @Nonnull List<Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>> bufferedPath,
                                                         @Nonnull List<AbstractObstacle> obstacles,
                                                         @Nonnull Map<Vector2D, Optional<List<Vector2D>>> olvList,
                                                         Map<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> minScores) {
        if (start.subtract(goal).getNorm() <= margin) {
            return Optional.of(List.of(new PathSide(goal.subtract(start), Vector2D.ZERO)));
        }
        if (confirmedPath.isEmpty()) {
            confirmedPath.add(new Pair<>(start, new Pair<>(List.of(new Pair<>(start, 0.0)), 0.0)));
        }
        // Main Dish
        final double ANGLE_LIMIT = this.angleLimit;
        final double COS_K = step / this.minimumMargin;

        Vector2D lastConfirmedPoint = confirmedPath.getLast().getFirst();
        double lastScore = confirmedPath.getLast().getSecond().getSecond();
        List<Pair<Vector2D, Double>> lastConfirmedPath = confirmedPath.getLast().getSecond().getFirst();
        if (!olvList.containsKey(lastConfirmedPoint)) {
            olvList.put(lastConfirmedPoint, this.getTangentPointsFromGoal(lastConfirmedPoint,
                    List.of(new Pair<>(goal, Optional.empty())), new ArrayList<>(), integratedObstacles,
                    step * this.deltaRatio, this.tangentSearchDepth, limit, timeLimit * this.timeLimitSplitRatio, updatedTime, new ArrayList<>()));
        }
        Optional<List<Vector2D>> olv = olvList.get(lastConfirmedPoint);
        if (olv.isEmpty()) {
            if (!this.isLargeAngle(lastConfirmedPath, lastConfirmedPoint, goal, ANGLE_LIMIT, margin)) {
                List<Pair<Vector2D, Double>> newPath = new ArrayList<>(lastConfirmedPath);
                double baseLength = 0.0;
                if (lastConfirmedPath.size() >= 2) {
                    baseLength = this.getMaxAdditionalLength(
                            lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                            lastConfirmedPath.getLast().getFirst(), goal, obstacles, maxAdditional, step);
                }
                newPath.addLast(new Pair<>(goal, baseLength));
                double newScore = this.getScore(
                        this.scoreSmoothing ? this.convertPath(newPath, COS_K, obstacles, step, maxAdditional) :
                                this.convertPath(newPath, COS_K), vel);
                bufferedPath.add(new Pair<>(goal, new Pair<>(newPath, newScore)));
            }
        } else {
            List<Vector2D> nextStarts = olv.get();
            if (nextStarts.isEmpty()) {
                // 引いてだめなら押してみろ (訳：時間切れかゴールが障害物に囲まれているとき)
                Optional<List<Vector2D>> olvStart = this.getTangentPointsFromStart(lastConfirmedPoint,
                        List.of(new Pair<>(goal, Optional.empty())), new ArrayList<>(), integratedObstacles,
                        step * this.deltaRatio, this.tangentSearchDepth, limit, timeLimit * 2.0 * this.timeLimitSplitRatio, updatedTime,
                        new ArrayList<>());
                olvList.put(lastConfirmedPoint, olvStart);
                if (olvStart.isEmpty()) {
                    if (!this.isLargeAngle(lastConfirmedPath, lastConfirmedPoint, goal, ANGLE_LIMIT, margin)) {
                        List<Pair<Vector2D, Double>> newPath = new ArrayList<>(lastConfirmedPath);
                        double baseLength = 0.0;
                        if (lastConfirmedPath.size() >= 2) {
                            baseLength = this.getMaxAdditionalLength(
                                    lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                                    lastConfirmedPath.getLast().getFirst(), goal, obstacles, maxAdditional, step);
                        }
                        newPath.addLast(new Pair<>(goal, baseLength));
                        double newScore = this.getScore(
                                this.scoreSmoothing ? this.convertPath(newPath, COS_K, obstacles, step, maxAdditional) :
                                        this.convertPath(newPath, COS_K), vel);
                        bufferedPath.add(new Pair<>(goal, new Pair<>(newPath, newScore)));
                    }
                } else if (!olvStart.get().isEmpty()) {
                    nextStarts.addAll(olvStart.get());
                }
            }

            for (Vector2D nextStart : nextStarts) {
                if (this.containsPos(lastConfirmedPath, margin, nextStart)) {
                    continue;
                }
                if (this.isLargeAngle(lastConfirmedPath, lastConfirmedPoint, nextStart, ANGLE_LIMIT, margin)) {
                    continue;
                }
                for (Vector2D key : olvList.keySet()) {
                    if (this.containsPos(nextStart, key, margin)) {
                        nextStart = key;
                        break;
                    }
                }
                List<Pair<Vector2D, Double>> newPath0 = new ArrayList<>(lastConfirmedPath);
                double baseLength = 0.0;
                if (lastConfirmedPath.size() >= 2) {
                    baseLength = this.getMaxAdditionalLength(
                            lastConfirmedPath.get(lastConfirmedPath.size() - 2).getFirst(),
                            lastConfirmedPath.getLast().getFirst(), nextStart, obstacles, maxAdditional, step);
                }
                newPath0.addLast(new Pair<>(nextStart, baseLength));
                double newScore = this.getScore(
                        this.scoreSmoothing ? this.convertPath(newPath0, COS_K, obstacles, step, maxAdditional) :
                                this.convertPath(newPath0, COS_K), vel);
                if (minScores.containsKey(nextStart)) {
                    if (newScore > minScores.get(nextStart).getSecond()) {
                        List<Pair<Vector2D, Double>> copied = new ArrayList<>(minScores.get(nextStart).getFirst());
                        copied.removeLast();
                        if (copied.getLast().getFirst() == lastConfirmedPoint) {
                            if (newScore - lastScore >= minScores.get(nextStart).getSecond() - this.getScore(
                                    this.scoreSmoothing ? this.convertPath(copied, COS_K, obstacles, step, maxAdditional) :
                                            this.convertPath(copied, COS_K), vel)) {
                                continue;
                            }
                        }
                    }
                    if (newScore - minScores.get(nextStart).getSecond() < this.getThreshold()) {
                        bufferedPath.add(new Pair<>(nextStart, new Pair<>(newPath0, newScore)));
                        if (newScore < minScores.get(nextStart).getSecond()) {
                            minScores.put(nextStart, new Pair<>(newPath0, newScore));
                        }
                    }
                } else {
                    minScores.put(nextStart, new Pair<>(newPath0, newScore));
                    bufferedPath.add(new Pair<>(nextStart, new Pair<>(newPath0, newScore)));
                }

            }
        }

        Optional<Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>> minScore = Optional.empty();
        while (minScore.isEmpty()) {
            if (bufferedPath.isEmpty()) {
                break;
            }
            minScore = bufferedPath.stream().min(InterfaceHelper.getComparator(
                    new IFuncParam1<Double, Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>>() {
                        @Override
                        public Double function(
                                Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> vector2DPairPair) {
                            return vector2DPairPair.getSecond().getSecond();
                        }
                    }));
            Vector2D key = minScore.get().getFirst();
            double score = minScore.get().getSecond().getSecond();
            if (minScores.containsKey(key)) {
                if (score - minScores.get(key).getSecond() <= this.getThreshold()) {
                    confirmedPath.add(minScore.get());
                    bufferedPath.remove(minScore.get());
                    if (score < minScores.get(key).getSecond()) {
                        minScores.put(key, minScore.get().getSecond());
                    }
                } else {
                    bufferedPath.remove(minScore.get());
                    minScore = Optional.empty();
                }
            } else {
                minScores.put(key, minScore.get().getSecond());
                confirmedPath.add(minScore.get());
                bufferedPath.remove(minScore.get());
            }
        }

        if (minScore.isPresent() && minScore.get().getFirst() == goal) {
            return Optional.of(this.outputSmoothing ?
                    this.convertPath(minScore.get().getSecond().getFirst(), obstacles, step, maxAdditional) :
                    this.convertPath(minScore.get().getSecond().getFirst()));
        }

        if (minScore.isEmpty() || nodeDepth <= 0) {
            Optional<Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>> minValue = confirmedPath.stream()
                    .min(InterfaceHelper.getComparator(
                            new IFuncParam1<Double, Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>>>() {
                                @Override
                                public Double function(
                                        Pair<Vector2D, Pair<List<Pair<Vector2D, Double>>, Double>> vector2DPairPair) {
                                    return vector2DPairPair.getFirst().subtract(goal).getNormSq();
                                }
                            }));
            // どうしようもない
            if (minValue.isEmpty() || minValue.get().getValue().getFirst().size() <= 1) {
                Vector2D maxVec =
                        this.getFirstCollidedPos(start, goal, integratedObstacles, step, limit, timeLimit,
                                updatedTime);
                if (maxVec.subtract(start).getNormSq() < MathHelper.EPSILON) {
                    return Optional.of(new ArrayList<>());
                } else {
                    // 限界まで近づく
                    return Optional.of(List.of(new PathSide(maxVec.subtract(start), Vector2D.ZERO)));
                }
            }
            return Optional.of(this.outputSmoothing ?
                    this.convertPath(minValue.get().getValue().getFirst(), obstacles, step, maxAdditional) :
                    this.convertPath(minValue.get().getValue().getFirst()));
        }

        return getPath(start, goal, vel, integratedObstacles, step, margin,
                nodeDepth - 1, limit, maxAdditional, timeLimit, updatedTime, confirmedPath, bufferedPath, obstacles,
                olvList, minScores);
    }

    @Override
    protected Optional<List<PathSide>> getPath(Vector2D start, Vector2D goal, Optional<Vector2D> vel,
                                               List<IntegratedObstacle> integratedObstacles, double step, double margin,
                                               int depth, int additionalDepth, double maxPathLength,
                                               double maxAdditional, double timeLimit, double now,
                                               List<AbstractObstacle> obstacles) {
        return this.getPath(start, goal, vel, integratedObstacles, step, margin, depth, maxPathLength,
                maxAdditional, timeLimit, now, new ArrayList<>(), new ArrayList<>(),
                obstacles,  new HashMap<>(), new HashMap<>());
    }

    protected abstract double getThreshold();

    protected abstract double getScore(List<PathSide> path,
                                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel);
}
