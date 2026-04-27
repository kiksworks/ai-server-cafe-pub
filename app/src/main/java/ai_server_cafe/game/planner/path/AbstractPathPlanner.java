package ai_server_cafe.game.planner.path;

import ai_server_cafe.config.Config;
import ai_server_cafe.model.field.obstacle.IntegratedObstacle;
import ai_server_cafe.model.field.obstacle.ObstacleParallelogram;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.ICircle;
import ai_server_cafe.model.field.obstacle.interfaces.IPolygon;
import ai_server_cafe.model.field.obstacle.interfaces.ISegment;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.DynamicsManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plannerの基底クラス
 * 共通して使用するものはここに書く
 */
public abstract class AbstractPathPlanner {
    private final Logger logger;
    protected double marginRatio;
    protected double deltaRatio;
    protected double timeLimitSplitRatio;
    protected double defaultStep;
    protected int tangentSearchDepth;
    protected boolean scoreSmoothing;
    protected boolean outputSmoothing;
    protected double minimumMargin;
    protected double obstacleIntegrationStep;
    protected double angleLimit;
    protected double ratioA;
    protected double ratioB;

    protected AbstractPathPlanner(String name) {
        this.logger = LogManager.getLogger(name);
        this.loadConfig();
    }

    public AbstractPathPlanner loadConfig() {
        Config.PathPlannerConfig config = ConfigManager.getInstance().getConfig().pathPlannerConfig;
        this.marginRatio = config.commonConfig.pointIntegrationMarginRatio;
        this.timeLimitSplitRatio = config.commonConfig.timeLimitSplitRatio;
        this.defaultStep = config.withPlannerConfig.defaultStep;
        this.deltaRatio = config.commonConfig.tangentDeltaRatio;
        this.tangentSearchDepth = config.commonConfig.tangentSearchDepth;
        this.scoreSmoothing = config.commonConfig.enableSmoothingCalculation;
        this.outputSmoothing = config.commonConfig.enableSmoothingOutput;
        this.minimumMargin = config.commonConfig.minimumMargin;
        this.obstacleIntegrationStep = config.commonConfig.obstacleIntegrationStep;
        this.angleLimit = config.commonConfig.angleLimit;
        this.ratioA = config.withPlannerConfig.defaultAccelCentripetalRatioA;
        this.ratioB = config.withPlannerConfig.defaultAccelCentripetalRatioB;
        return this;
    }

    public AbstractPathPlanner setId(int id) {
        DynamicsManager manager = DynamicsManager.getInstance();
        this.ratioA = manager.getCentripetalRatio(id, false);
        this.ratioB = manager.getCentripetalRatio(id, true);
        return this;
    }

    /**
     * 障害物の統合を行う
     * @param obstacles 未統合の障害物群
     * @return 統合済みの障害物群
     * @param <T> extends AbstractObstacle
     */
    @Nonnull
    protected <T extends AbstractObstacle> List<IntegratedObstacle> getIntegratedObstacles(@Nonnull List<T> obstacles) {
        List<IntegratedObstacle> result = new ArrayList<>();
        List<AbstractObstacle> addedObstacles = new ArrayList<>();
        Map<AbstractObstacle, List<AbstractObstacle>> integratedMap = new HashMap<>();
        for (AbstractObstacle obstacle : obstacles) {
            boolean flag = true;
            for (AbstractObstacle ao : obstacles) {
                if (obstacle != ao && !addedObstacles.contains(ao) &&
                        obstacle.isCollided(ao.expandMargin(this.defaultStep), this.obstacleIntegrationStep)) {
                    if (!integratedMap.containsKey(obstacle)) {
                        integratedMap.put(obstacle, new ArrayList<>());
                    }
                    integratedMap.get(obstacle).add(ao);
                    flag = false;
                }
            }
            if (flag) {
                result.add(new IntegratedObstacle(obstacle));
                addedObstacles.add(obstacle);
            }
        }
        for (Map.Entry<AbstractObstacle, List<AbstractObstacle>> obstaclePair : integratedMap.entrySet()) {
            if (!addedObstacles.contains(obstaclePair.getKey())) {
                IntegratedObstacle io = new IntegratedObstacle(obstaclePair.getKey());
                addedObstacles.add(obstaclePair.getKey());
                boolean flag = true;
                while (flag) {
                    flag = false;
                    for (AbstractObstacle isIn : io.getObstacles()) {
                        if (integratedMap.containsKey(isIn)) {
                            for (AbstractObstacle relative : integratedMap.get(isIn)) {
                                if (!addedObstacles.contains(relative)) {
                                    io.add(relative);
                                    addedObstacles.add(relative);
                                    flag = true;
                                }
                            }
                        }
                    }
                }
                result.add(io);
            }
        }
        return result;
    }

    /**
     * 最終的に出力される経路
     * @param start 始点
     * @param goal 終点
     * @param obstacles 障害物
     * @param step ステップ長
     * @param depth 最大計算回数
     * @param allowObstacleEnd 障害物の内部に終点を取るか
     * @param additionalDepth ゴールを見つけたあと余分に計算する量
     * @param vel 初速度
     * @return 存在すれば，[[経路]，[障害物の外部に新しく取った終点]] の構造で探索結果
     * @param <T>
     */
    public <T extends AbstractObstacle> Optional<Pair<List<PathSide>, Vector2D>> getFinalPath(Vector2D start,
            Vector2D goal, List<T> obstacles, double step, int depth, boolean allowObstacleEnd, int additionalDepth,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel) {
        try {
            double updated = TimeHelper.now();
            double height = ConfigManager.getInstance().getConfig().maxFieldHeight;
            double width = ConfigManager.getInstance().getConfig().maxFieldWidth;
            double maxPathLength = FastMath.sqrt(height * height + width * width);
            double maxVel = ConfigManager.getInstance().getConfig().controllerConfig.velocityMax;
            double minAcc = ConfigManager.getInstance().getConfig().controllerConfig.brakeToTargetPosition;
            double maxExitLength = FastMath.max(5000.0, maxVel * maxVel / minAcc + 200.0);
            return this.exitAndSearch(start, goal, obstacles, step, depth, allowObstacleEnd, additionalDepth, vel,
                    maxExitLength, step / 500.0, true, maxPathLength, this.getTimeLimit(), updated);
        } catch (StackOverflowError e) {
            this.logger.info("stack-overflow occurred in 'optimalDijkstraPathFront'. The calculation was skipped.");
            return Optional.empty();
        }
    }

    /**
     * 計算をするときの時間制限
     * @return 計算用制限時間
     */
    protected abstract double getTimeLimit();

    /**
     * 終点と始点での障害物からの脱出をして，探索を行う
     * @param start 始点
     * @param goal 終点
     * @param obstacles 障害物
     * @param step ステップ長
     * @param depth 探索の深さ
     * @param allowObstacleEnd 終点が障害物内にある状態を許可するか
     * @param additionalDepth 追加の探索の深さ
     * @param vel 初速度
     * @param lengthRange 脱出する際の最大距離
     * @param ds 脱出用に障害物の探知を行うときの円弧方向の微小距離
     * @param dsIsTheta dsを微小角度[rad]として扱うか
     * @param maxPathLength 経路の1辺の最大長さ
     * @param timeLimit 制限時間
     * @param now 探索を始めた実時間
     * @return [[得られた経路],[障害物から脱出するために新たに置かれた終点]]の構造で探索結果
     * @param <T> extends AbstractObstacle
     */
    protected <T extends AbstractObstacle> Optional<Pair<List<PathSide>, Vector2D>> exitAndSearch(Vector2D start,
            Vector2D goal, List<T> obstacles, double step, int depth, boolean allowObstacleEnd, int additionalDepth,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel, double lengthRange,
            double ds, boolean dsIsTheta, double maxPathLength, double timeLimit, double now) {
        double velMax = ConfigManager.getInstance().getConfig().controllerConfig.velocityMax;
        double acc = ConfigManager.getInstance().getConfig().controllerConfig.brakeToTargetPosition;
        final double MAX_ADDITIONAL = 0.5 * velMax * velMax / acc;
        List<IntegratedObstacle> ios = this.getIntegratedObstacles(obstacles);
        List<IntegratedObstacle> collidedObstacleList = new ArrayList<>();
        for (IntegratedObstacle io : ios) {
            if (io.isCollided(start)) {
                collidedObstacleList.add(io);
            }
        }
        Optional<Vector2D> startExit = Optional.empty();
        Optional<Vector2D> newGoal = Optional.empty();
        if (!collidedObstacleList.isEmpty()) {
            startExit = this.exitObstacles(start, collidedObstacleList, step, lengthRange, ds, dsIsTheta,
                    vel.orElse(Vector2D.ZERO), acc, velMax);
        }
        Optional<IntegratedObstacle> isInGoal = Optional.empty();
        for (IntegratedObstacle io : ios) {
            if (io.isCollided(goal)) {
                // integrateしたので1つなはず
                isInGoal = Optional.of(io);
                break;
            }
        }

        // goalが埋まっている場合
        if (isInGoal.isPresent()) {
            Optional<Vector2D> vector2D =
                    this.exitObstacle(goal, isInGoal.get(), step, lengthRange, ds, dsIsTheta, start);
            if (vector2D.isEmpty()) {
                return Optional.empty();
            }
            newGoal = vector2D;
        }
        List<AbstractObstacle> abstractObstacles = new ArrayList<>(obstacles);
        Optional<List<PathSide>> result = this.getPath(startExit.isPresent() ? startExit.get() : start,
                newGoal.isPresent() ? newGoal.get() : goal, startExit.isPresent() ? Optional.of(Vector2D.ZERO) : vel,
                ios, step, this.marginRatio * step, depth, additionalDepth, maxPathLength, MAX_ADDITIONAL, timeLimit,
                now, abstractObstacles);
        if ((result.isEmpty())) {
            if (startExit.isPresent()) return Optional.of(
                    new Pair<>(List.of(new PathSide(startExit.get().subtract(start), Vector2D.ZERO)),
                            newGoal.orElse(goal)));
            return Optional.empty();
        }
        List<PathSide> exitResult = new ArrayList<>();
        List<PathSide> tempResult = new ArrayList<>(result.get());
        if (startExit.isPresent()) {
            exitResult.add(new PathSide(startExit.get().subtract(start), Vector2D.ZERO));
            if (!tempResult.isEmpty()) {
                List<AbstractObstacle> tempObstacles = new ArrayList<>();
                for (IntegratedObstacle io : ios) {
                    tempObstacles.addAll(io.getObstacles());
                }
                double l = this.getMaxAdditionalLength(start, startExit.get(),
                        tempResult.getFirst().side.add(startExit.get()), tempObstacles, MAX_ADDITIONAL, step);
                tempResult.set(0, new PathSide(tempResult.getFirst().side,
                        MathHelper.normalized(startExit.get().subtract(start)).scalarMultiply(l)));
            }
        }
        exitResult.addAll(tempResult);
        if (allowObstacleEnd && newGoal.isPresent()) {
            if (exitResult.size() < 2) {
                exitResult.add(new PathSide(goal.subtract(newGoal.get()), Vector2D.ZERO));
            } else {
                double l =
                        this.getMaxAdditionalLength(newGoal.get().subtract(exitResult.get(exitResult.size() - 2).side),
                                newGoal.get(), goal, abstractObstacles, MAX_ADDITIONAL, step);
                exitResult.add(new PathSide(goal.subtract(newGoal.get()),
                        MathHelper.normalized(exitResult.get(exitResult.size() - 2).side).scalarMultiply(l)));
            }
        }
        return Optional.of(new Pair<>(exitResult, newGoal.orElse(goal)));
    }

    protected abstract Optional<List<PathSide>> getPath(Vector2D start, Vector2D goal,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
            List<IntegratedObstacle> integratedObstacles, double step, double margin, int depth, int additionalDepth,
            double maxPathLength, double maxAdditional, double timeLimit, double now, List<AbstractObstacle> obstacles);

    /**
     * @param lastPos   1つ前のノード
     * @param nowPos    現在のノード
     * @param newPos    次のノードになるかもしれないノード
     * @param obstacles 障害物
     * @param maxLength 800.0 (=0.5 * vMax * vMax / brake)
     * @return 追加で移動できる長さ
     */
    protected double getMaxAdditionalLength(@Nonnull Vector2D lastPos, @Nonnull Vector2D nowPos,
            @Nonnull Vector2D newPos, @Nonnull List<AbstractObstacle> obstacles, double maxLength, double step) {
        double K = 0.5;
        Vector2D side1 = nowPos.subtract(lastPos);
        Vector2D side2 = newPos.subtract(nowPos);
        if (MathHelper.isEpsilon(side1.getNorm()) || MathHelper.isEpsilon(side2.getNorm())) return 0.0;
        if (MathHelper.isEpsilon(FastMath.sin(MathHelper.inferiorAngle(side1, side2))))
            return FastMath.min(nowPos.subtract(newPos).getNorm(), maxLength);
        List<AbstractObstacle> extracted = new ArrayList<>();
        double result = maxLength / K * FastMath.sin(MathHelper.inferiorAngle(side1, side2));
        ObstacleParallelogram op =
                new ObstacleParallelogram(nowPos, MathHelper.normalized(side1).scalarMultiply(result),
                        MathHelper.normalized(side2).scalarMultiply(maxLength), 0.0);
        for (AbstractObstacle obstacle : obstacles) {
            if (op.isCollided(obstacle, step)) {
                if (obstacle instanceof ICircle) {
                    Vector2D p = ((ICircle) obstacle).getCenter();
                    if (ObstacleSegment.hasPip(nowPos, newPos, p)) {
                        Vector2D ray = p.subtract(nowPos);
                        result = FastMath.min(result,
                                ray.getNorm() * FastMath.sin(MathHelper.inferiorAngle(ray, side2)) -
                                        ((ICircle) obstacle).getRadius() - obstacle.getMargin());
                    } else {
                        result = FastMath.min(result,
                                p.subtract(newPos).getNorm() - ((ICircle) obstacle).getRadius() - obstacle.getMargin());
                    }
                } else if (obstacle instanceof ISegment) {
                    for (Vector2D vertex : ((ISegment) obstacle).getVertexes()) {
                        if (op.isCollided(vertex)) {
                            if (ObstacleSegment.hasPip(nowPos, newPos, vertex)) {
                                Vector2D ray = vertex.subtract(nowPos);
                                result = FastMath.min(result,
                                        ray.getNorm() * FastMath.sin(MathHelper.inferiorAngle(ray, side2)) -
                                                obstacle.getMargin());
                            } else {
                                result = FastMath.min(result, vertex.subtract(newPos).getNorm() - obstacle.getMargin());
                            }
                        }
                    }
                } else if (obstacle instanceof IPolygon) {
                    for (Vector2D vertex : ((IPolygon) obstacle).getVertexes()) {
                        if (op.isCollided(vertex)) {
                            if (ObstacleSegment.hasPip(nowPos, newPos, vertex)) {
                                Vector2D ray = vertex.subtract(nowPos);
                                result = FastMath.min(result,
                                        ray.getNorm() * FastMath.sin(MathHelper.inferiorAngle(ray, side2)) -
                                                obstacle.getMargin());
                            } else {
                                result = FastMath.min(result, vertex.subtract(newPos).getNorm() - obstacle.getMargin());
                            }
                        }
                    }
                }
                extracted.add(obstacle);
            }
        }
        result = result / FastMath.sin(MathHelper.inferiorAngle(side1, side2));
        Vector2D v1 = MathHelper.normalized(side1);
        label:
        for (double l = step; l <= result; l += step) {
            Vector2D p1 = v1.scalarMultiply(l).add(nowPos);
            for (AbstractObstacle obstacle : extracted) {
                if (obstacle.isCollided(p1)) {
                    result = l - step;
                    break label;
                }
            }
            Vector2D p2 = v1.scalarMultiply(l).add(newPos);
            for (AbstractObstacle obstacle : extracted) {
                if (obstacle.isCollided(p2)) {
                    result = l - step;
                    break label;
                }
            }
        }
        return FastMath.max(K * result, 0.0); // ぎりぎりは困るので
    }

    protected List<PathSide> convertPath(@Nonnull List<Pair<Vector2D, Double>> path, List<AbstractObstacle> obstacles,
            double step, double maxAdditional) {
        return this.convertPathV3(path, obstacles, step, maxAdditional);
    }

    protected List<PathSide> convertPath(@Nonnull List<Pair<Vector2D, Double>> path, double K,
            List<AbstractObstacle> obstacles, double step, double maxAdditional) {
        return this.convertPathV3(path, obstacles, step, maxAdditional);
    }

    protected List<PathSide> convertPath(@Nonnull List<Pair<Vector2D, Double>> path, List<AbstractObstacle> obstacles,
            double step, double maxAdditional,
            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> initVel) {
        return this.convertPathV3(path, obstacles, step, maxAdditional);
    }

    @Nonnull
    protected List<PathSide> convertPath(@Nonnull List<Pair<Vector2D, Double>> path) {
        return this.convertPath(path, List.of(), 0.0, 0.0, Optional.empty());
    }

    @Nonnull
    protected List<PathSide> convertPath(@Nonnull List<Pair<Vector2D, Double>> path, double K) {
        return this.convertPath(path, List.of(), 0.0, 0.0, Optional.empty());
    }

    /**
     * vel, al0
     * |
     * p0 -- p1 -- al1 (additionalLength)
     * #######|
     * #######|
     * #######p2
     * 頂点と追加距離のリストからPathSideのリストに変換
     *
     * @param path          converting raw path. {(p0, 0), (p1, al0), (p2, al1)}
     * @param obstacles     Obstacles. If this is empty, smoothing process won't run.
     * @param step ステップ長
     * @param maxAdditional 最大制御距離
     * @return 変換されたPathSideのリスト
     */
    protected List<PathSide> convertPathV3(@Nonnull List<Pair<Vector2D, Double>> path, List<AbstractObstacle> obstacles,
            double step, double maxAdditional) {
        final Config.PIDController config = ConfigManager.getInstance().getConfig().controllerConfig;
        final double BRAKE = config.brakeToTargetPosition;
        final double MAX_VEL = config.velocityMax;
        List<PathSide> result = new ArrayList<>();
        if (path.isEmpty()) return result;
        if (path.size() == 1) return List.of(new PathSide(Vector2D.ZERO, Vector2D.ZERO));

        List<Pair<Vector2D, Double>> fixedPath = new ArrayList<>(path);
        if (!obstacles.isEmpty()) {
            fixedPath.clear();
            List<Vector2D> bufferPath = new ArrayList<>();
            for (Pair<Vector2D, Double> pair : path) {
                bufferPath.add(pair.getFirst());
            }
            while (true) {
                boolean flag = false;
                List<Vector2D> tempPath = new ArrayList<>();
                tempPath.add(bufferPath.getFirst());
                label:
                for (int i = 2; i < bufferPath.size(); i++) {
                    Vector2D start = bufferPath.get(i - 2);
                    Vector2D goal = bufferPath.get(i);
                    if (tempPath.getLast() != start) {
                        tempPath.add(bufferPath.get(i - 1));
                        continue;
                    }
                    // 0.5 * step
                    ObstacleSegment seg = new ObstacleSegment(start, goal, 0.5 * step);
                    for (AbstractObstacle ao : obstacles) {
                        if (ao.isCollided(seg, step)) {
                            tempPath.add(bufferPath.get(i - 1));
                            continue label;
                        }
                    }
                    flag = true;
                }
                tempPath.add(bufferPath.getLast());

                if (flag) {
                    bufferPath = new ArrayList<>(tempPath);
                } else {
                    break;
                }
            }

            fixedPath.add(new Pair<>(bufferPath.getFirst(), 0.0));
            fixedPath.add(new Pair<>(bufferPath.get(1), path.get(1).getSecond()));
            for (int i = 2; i < bufferPath.size(); i++) {
                double baseLength =
                        this.getMaxAdditionalLength(bufferPath.get(i - 2), bufferPath.get(i - 1), bufferPath.get(i),
                                obstacles, maxAdditional, step);
                fixedPath.add(new Pair<>(bufferPath.get(i), baseLength));
            }
        }
        Vector2D tempStart;
        Vector2D tempGoal = fixedPath.getLast().getFirst();
        double baseLength = fixedPath.getLast().getSecond();
        double additionalLength = 0.0;
        Vector2D lastStart;
        for (int i = fixedPath.size() - 2; i > 0; i--) {
            tempStart = fixedPath.get(i).getFirst();
            lastStart = fixedPath.get(i - 1).getFirst();
            Vector2D side2 = tempGoal.subtract(tempStart);
            Vector2D side1 = tempStart.subtract(lastStart);
            double theta = MathHelper.inferiorAngle(side1, side2);
            double vi = this.vp(theta, side1, side2, additionalLength, BRAKE, MAX_VEL);
            additionalLength = FastMath.min(baseLength, ControlHelper.getDistanceTrapezoidal(BRAKE, vi));
            result.add(new PathSide(side2, MathHelper.normalized(side1).scalarMultiply(additionalLength)));
            tempGoal = tempStart;
            baseLength = fixedPath.get(i).getSecond();
        }
        result.add(new PathSide(fixedPath.get(1).getFirst().subtract(fixedPath.getFirst().getFirst()),
                MathHelper.getFromPolar(baseLength, 0.0)));
        return result.reversed();
    }

    protected double vp(double theta, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double additional, double brake,
            double maxVel) {
        return this.vp0(theta, side1, side2, additional, brake, maxVel);
    }

    protected double vp0(double theta, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double additional,
            double brake, double maxVel) {
        final double K = 0.4;
        double cos = FastMath.max(0.0, FastMath.cos(theta));
        cos = FastMath.max(cos, FastMath.sqrt(K * FastMath.abs(cos))); // cosTheta <= K以下でsqrt
        return FastMath.min(0.9 * maxVel,
                ControlHelper.getVelocityTrapezoidal(brake, side1.getNorm() + additional) * cos);
    }

    protected double vpA(double theta, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double additional,
            double brake, double maxVel) {
        double l2 = side2.getNorm();
        theta = FastMath.min(FastMath.abs(theta), MathHelper.HALF_PI);
        double r = 0.5 * l2 / FastMath.sin(theta);
        double perpendicularAcc = this.ratioA * brake;
        final double K = 0.4;
        double cos = FastMath.max(0.0, FastMath.cos(theta));
        cos = FastMath.max(cos, FastMath.sqrt(K * FastMath.abs(cos))); // cosTheta <= K以下でsqrt
        double c = l2 / MathHelper.sinc(theta) + additional * cos * cos;
        double ve = FastMath.min(ControlHelper.getVelocityTrapezoidal(perpendicularAcc, c),
                FastMath.sqrt(r * perpendicularAcc));
        return MathHelper.min(maxVel, ve);
    }

    protected double vpB(double theta, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double additional,
            double brake, double maxVel) {
        double l2 = side2.getNorm();
        double phi = MathHelper.getPhi(1.0, 1.0, theta);
        double r = 0.5 * l2 / FastMath.sin(phi);
        double perpendicularAcc = this.ratioB * brake;
        double cos = FastMath.cos(phi);
        double c = l2 / MathHelper.sinc(phi) + additional * cos * cos;
        double ve = FastMath.min(ControlHelper.getVelocityTrapezoidal(perpendicularAcc, c),
                FastMath.sqrt(r * perpendicularAcc));
        return MathHelper.min(maxVel, ve) * FastMath.cos(theta - phi);
    }

    /**
     * 終点から探索し，障害物に重ならない接線を抽出し，その頂点を見つける
     * @param start 始点
     * @param goals 終点
     * @param result 結果
     * @param ios 統合済み障害物
     * @param delta 頂点から少しずらす長さ
     * @param nodeLimit 探索の深さ
     * @param limit 探索の限界長さ
     * @param timeLimit 探索の制限時間
     * @param updatedTime 探索を始めた実時間
     * @param visitedObs 既に探索した障害物かどうか
     * @return 始点から見える障害物の頂点
     */
    @Nonnull
    public Optional<List<Vector2D>> getTangentPointsFromGoal(@Nonnull Vector2D start,
            @Nonnull List<Pair<Vector2D, Optional<AbstractObstacle>>> goals, List<Vector2D> result,
            List<IntegratedObstacle> ios, double delta, int nodeLimit, double limit, double timeLimit,
            double updatedTime, List<AbstractObstacle> visitedObs) {
        if (goals.isEmpty()) return Optional.of(result);
        List<Pair<Vector2D, Optional<AbstractObstacle>>> newGoals = new ArrayList<>();
        for (Pair<Vector2D, Optional<AbstractObstacle>> goalPair : goals) {
            Optional<AbstractObstacle> obstacle =
                    this.getLastCollided(start, goalPair.getFirst(), ios, delta, limit, timeLimit, updatedTime);
            if (TimeHelper.now() - updatedTime > timeLimit) {
                return Optional.of(new ArrayList<>());
            }
            if (obstacle.isEmpty()) {
                // 初期値ならemptyを返す
                if (goalPair.getSecond().isEmpty()) return Optional.empty();
                result.add(goalPair.getFirst());
            } else if ((goalPair.getSecond().isEmpty() || obstacle.get() != goalPair.getSecond().get()) &&
                    !visitedObs.contains(obstacle.get())) {
                visitedObs.add(obstacle.get());
                // 同じobstacleが呼ばれるとループするのでそれを回避 参照比較
                for (Vector2D newGoal : obstacle.get().getTangentPoints(start)) {
                    for (Vector2D perpendicular : MathHelper.getPerpendicularVectors(newGoal.subtract(start))) {
                        Vector2D nextGoal = newGoal.add(perpendicular.scalarMultiply(delta));
                        if (!obstacle.get().isCollided(nextGoal) && !this.isCollided(nextGoal, ios)) {
                            newGoals.add(new Pair<>(nextGoal, obstacle));
                        }
                    }
                }
            }
        }
        if (newGoals.isEmpty() || nodeLimit == 0) {
            return Optional.of(result);
        }
        return this.getTangentPointsFromGoal(start, newGoals, result, ios, delta, nodeLimit - 1, limit, timeLimit,
                updatedTime, visitedObs);
    }

    /**
     * 始点から探索し，障害物に重ならない接線を抽出し，その頂点を見つける
     * @param start 始点
     * @param goals 終点
     * @param result 結果
     * @param ios 統合済み障害物
     * @param delta 頂点から少しずらす長さ
     * @param nodeLimit 探索の深さ
     * @param limit 探索の限界長さ
     * @param timeLimit 探索の制限時間
     * @param updatedTime 探索を始めた実時間
     * @param visitedObs 既に探索した障害物かどうか
     * @return 始点から見える障害物の頂点
     */
    @Nonnull
    public Optional<List<Vector2D>> getTangentPointsFromStart(@Nonnull Vector2D start,
            @Nonnull List<Pair<Vector2D, Optional<AbstractObstacle>>> goals, List<Vector2D> result,
            List<IntegratedObstacle> ios, double delta, int nodeLimit, double limit, double timeLimit,
            double updatedTime, List<AbstractObstacle> visitedObs) {
        if (goals.isEmpty()) return Optional.of(result);
        List<Pair<Vector2D, Optional<AbstractObstacle>>> newGoals = new ArrayList<>();
        for (Pair<Vector2D, Optional<AbstractObstacle>> goalPair : goals) {
            Optional<AbstractObstacle> obstacle =
                    this.getFirstCollided(start, goalPair.getFirst(), ios, delta, limit, timeLimit, updatedTime);
            if (TimeHelper.now() - updatedTime > timeLimit) {
                return Optional.of(new ArrayList<>());
            }
            if (obstacle.isEmpty()) {
                // 初期値ならemptyを返す
                if (goalPair.getSecond().isEmpty()) return Optional.empty();
                result.add(goalPair.getFirst());
            } else if ((goalPair.getSecond().isEmpty() || obstacle.get() != goalPair.getSecond().get()) &&
                    !visitedObs.contains(obstacle.get())) {
                visitedObs.add(obstacle.get());
                // 同じobstacleが呼ばれるとループするのでそれを回避 参照比較
                for (Vector2D newGoal : obstacle.get().getTangentPoints(start)) {
                    for (Vector2D perpendicular : MathHelper.getPerpendicularVectors(newGoal.subtract(start))) {
                        Vector2D nextGoal = newGoal.add(perpendicular.scalarMultiply(delta));
                        if (!obstacle.get().isCollided(nextGoal) && !this.isCollided(nextGoal, ios)) {
                            newGoals.add(new Pair<>(nextGoal, obstacle));
                        }
                    }
                }
            }
        }
        if (newGoals.isEmpty() || nodeLimit == 0) {
            return Optional.of(result);
        }
        return this.getTangentPointsFromStart(start, newGoals, result, ios, delta, nodeLimit - 1, limit, timeLimit,
                updatedTime, visitedObs);
    }

    /**
     * 複数の障害物からの脱出(始点用)
     * @param start 始点
     * @param obstacles 障害物
     * @param step ステップ長
     * @param lengthRange 探索範囲
     * @param ds 探索する円弧方向の微小距離
     * @param dsIsTheta dsを微小角度[rad]として扱うか
     * @param vel 初速度
     * @param brake 減速度
     * @param maxVel 最大速度
     * @return 脱出位置
     * @param <T> extends AbstractObstacle
     */
    @Nonnull
    public <T extends AbstractObstacle> Optional<Vector2D> exitObstacles(@Nonnull Vector2D start,
            @Nonnull List<T> obstacles, double step, double lengthRange, double ds, boolean dsIsTheta, Vector2D vel,
            double brake, double maxVel) {
        final double K = 1.0;
        if (obstacles.isEmpty()) return Optional.of(start);
        List<Vector2D> candidates = new ArrayList<>();
        for (double l = step; l < lengthRange; l += step) {
            for (double theta = 0.0; theta <= MathHelper.PI; theta += dsIsTheta ? ds : ds / l) {
                Vector2D newPos = MathHelper.getFromPolar(l, theta).add(start);
                if (!isCollided(newPos, obstacles)) {
                    boolean flag = false;
                    for (double theta0 = 0.0; theta0 <= MathHelper.PI; theta0 += dsIsTheta ? ds : ds / step) {
                        if (isCollided(newPos.add(MathHelper.getFromPolar(K * step, theta0)), obstacles)) {
                            flag = true;
                            break;
                        }
                        if (isCollided(newPos.add(MathHelper.getFromPolar(K * step, -theta0)), obstacles)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        candidates.add(newPos);
                    }
                }
                Vector2D newPos2 = MathHelper.getFromPolar(l, -theta).add(start);
                if (!isCollided(newPos2, obstacles)) {
                    boolean flag = false;
                    for (double theta0 = 0.0; theta0 <= MathHelper.PI; theta0 += dsIsTheta ? ds : ds / step) {
                        if (isCollided(newPos2.add(MathHelper.getFromPolar(K * step, theta0)), obstacles)) {
                            flag = true;
                            break;
                        }
                        if (isCollided(newPos2.add(MathHelper.getFromPolar(K * step, -theta0)), obstacles)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        candidates.add(newPos2);
                    }
                }
            }
            if (candidates.size() > 100) {
                break;
            }
        }
        return candidates.stream().min(InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
            @Override
            public Double function(Vector2D vector2D) {
                return getExitScore(start, vector2D, vel, brake, maxVel);
            }
        }));
    }

    /**
     * 脱出点のスコア(始点用)
     * @param start 始点
     * @param pos 脱出位置
     * @param vel 初速度
     * @param brake 減速度
     * @param vm 最大速度
     * @return スコア
     */
    private double getExitScore(@Nonnull Vector2D start, @Nonnull Vector2D pos, @Nonnull Vector2D vel, double brake,
            double vm) {
        Vector2D dir = pos.subtract(start);
        double inferiorAngle = MathHelper.inferiorAngle(vel, dir);
        double sin = FastMath.sin(inferiorAngle);
        double l = dir.getNorm();
        double perpendicularBrake = this.ratioA * brake;
        double r = 0.5 * l / sin;
        double vr = vm;
        if (sin > 1.0E-9) {
            vr = FastMath.sqrt(perpendicularBrake * r);
        }
        double time = 0.0;
        double maxVel = FastMath.min(vr, vm);
        if (vel.getNorm() > maxVel) {
            time += 1000.0;
        }
        double c = l / MathHelper.sinc(inferiorAngle);
        Pair<Double, Double> pair = ControlHelper.timeForTrapezoidalControl(brake, brake, maxVel, vel.getNorm(), 0.0, c);
        time += pair.getFirst();
        return time;
    }

    /**
     * 点が障害物に埋まっているかどうか
     * @param target 点
     * @param obstacles 障害物
     * @return 点が障害物に埋まっているか
     * @param <T> extends AbstractObstacle
     */
    public <T extends AbstractObstacle> boolean isCollided(@Nonnull Vector2D target, @Nonnull List<T> obstacles) {
        // フィールド外ならtrue
        if (isOutSide(target)) return true;
        for (AbstractObstacle obstacle : obstacles) {
            if (obstacle.isCollided(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * フィールド外を探索しないようにするための関数
     * @param target 点
     * @return targetがフィールド外にあるか
     */
    public boolean isOutSide(@Nonnull Vector2D target) {
        return -0.5 * UpdaterWorld.getInstance().getField().getFieldWidth() > target.getX() ||
                -0.5 * UpdaterWorld.getInstance().getField().getFieldHeight() > target.getY() ||
                0.5 * UpdaterWorld.getInstance().getField().getFieldWidth() < target.getX() ||
                0.5 * UpdaterWorld.getInstance().getField().getFieldHeight() < target.getY();
    }

    /**
     * 単一の障害物からの脱出(終点用)
     * @param start 終点
     * @param obstacle 障害物
     * @param step ステップ長
     * @param lengthRange 探索範囲
     * @param ds 探索するときの円弧方向の微小距離
     * @param dsIsTheta dsを微小角度として扱うか
     * @param basePos 基準点(始点)
     * @return 脱出位置
     */
    @Nonnull
    public Optional<Vector2D> exitObstacle(@Nonnull Vector2D start, @Nonnull AbstractObstacle obstacle, double step,
            double lengthRange, double ds, boolean dsIsTheta, Vector2D basePos) {
        if (!isCollided(start, List.of(obstacle))) return Optional.of(start);
        List<Vector2D> candidates = new ArrayList<>();
        for (double l = step; l < lengthRange; l += step) {
            for (double theta = 0.0; theta <= MathHelper.PI; theta += dsIsTheta ? ds : ds / l) {
                Vector2D newPos = MathHelper.getFromPolar(l, theta).add(start);
                if (!isCollided(newPos, List.of(obstacle))) {
                    candidates.add(newPos);
                }
                Vector2D newPos2 = MathHelper.getFromPolar(l, -theta).add(start);
                if (!isCollided(newPos2, List.of(obstacle))) {
                    candidates.add(newPos2);
                }
                if (candidates.size() > 50) {
                    break;
                }
            }
        }
        return candidates.stream().min(InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
            @Override
            public Double function(Vector2D vector2D) {
                return getGoalExitScore(vector2D, basePos, start);
            }
        }));
    }

    /**
     * 脱出用スコア(終点用)
     * @param targetPos 脱出候補位置
     * @param currentPos 現在位置
     * @param goalPos 終点
     * @return スコア
     */
    private double getGoalExitScore(@Nonnull Vector2D targetPos, @Nonnull Vector2D currentPos,
            @Nonnull Vector2D goalPos) {
        double k = goalPos.subtract(targetPos).getNorm() / goalPos.subtract(currentPos).getNorm();
        if (MathHelper.isNanOrInfinity(k)) {
            k = 0.0;
        }
        return goalPos.subtract(targetPos).getNorm() + k * currentPos.subtract(targetPos).getNorm();
    }

    public boolean isLargeAngle(@Nonnull List<Pair<Vector2D, Double>> path, Vector2D newStart, Vector2D newGoal,
            double angle, double margin) {
        if (path.size() < 2) {
            return false;
        }
        Vector2D lastVector = path.getLast().getFirst().subtract(path.get(path.size() - 2).getFirst());
        if (lastVector.getNorm() <= margin) {
            if (path.size() < 3) {
                return false;
            }
            lastVector = path.get(path.size() - 2).getFirst().subtract(path.get(path.size() - 3).getFirst());
        }
        Vector2D nextVector = newGoal.subtract(newStart);
        return lastVector.dotProduct(nextVector) < lastVector.getNorm() * nextVector.getNorm() * FastMath.cos(angle);
    }

    public boolean isLargeAngle(@Nonnull Vector2D lastStart, @Nonnull Vector2D newStart, @Nonnull Vector2D newGoal,
            double angle) {
        Vector2D lastVector = newStart.subtract(lastStart);
        Vector2D nextVector = newGoal.subtract(newStart);
        return lastVector.dotProduct(nextVector) < lastVector.getNorm() * nextVector.getNorm() * FastMath.cos(angle);
    }

    /**
     * 終点から直線的に探索したとき，初めに重なった障害物
     * @param start 始点
     * @param goal 終点
     * @param obstacles 障害物
     * @param step ステップ長
     * @param limit 探索距離限界
     * @param timeLimit 制限時間
     * @param updatedTime 実行実時刻
     * @return 重なった障害物
     * @param <T> extends AbstractObstacle
     */
    public <T extends AbstractObstacle> Optional<AbstractObstacle> getLastCollided(@Nonnull Vector2D start,
            @Nonnull Vector2D goal, @Nonnull List<T> obstacles, double step, double limit, double timeLimit,
            double updatedTime) {
        Vector2D sg = start.subtract(goal);
        if (sg.getNormSq() < MathHelper.EPSILON * MathHelper.EPSILON) return Optional.empty();
        List<AbstractObstacle> canCollide = new ArrayList<>();
        for (AbstractObstacle obstacle : obstacles) {
            if (obstacle.getAABB().isIncluded(start, goal)) {
                canCollide.add(obstacle);
            }
        }
        double gsNorm = FastMath.min(sg.getNorm(), limit);
        double mod = MathHelper.mod(gsNorm, step);
        for (double l = mod; l <= gsNorm; l += step) {
            if (TimeHelper.now() - updatedTime > timeLimit) {
                return Optional.of(AbstractObstacle.EMPTY);
            }
            Vector2D newPos = MathHelper.normalized(sg).scalarMultiply(l).add(goal);
            for (AbstractObstacle obstacle : canCollide) {
                if (obstacle.isCollided(newPos)) {
                    return Optional.of(obstacle);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 始点から直線的に探索したとき，初めに重なった障害物
     * @param start 始点
     * @param goal 終点
     * @param obstacles 障害物
     * @param step ステップ長
     * @param limit 探索距離限界
     * @param timeLimit 制限時間
     * @param updatedTime 実行実時刻
     * @return 重なった障害物
     * @param <T> extends AbstractObstacle
     */
    public <T extends AbstractObstacle> Optional<AbstractObstacle> getFirstCollided(@Nonnull Vector2D start,
            @Nonnull Vector2D goal, @Nonnull List<T> obstacles, double step, double limit, double timeLimit,
            double updatedTime) {
        Vector2D gs = goal.subtract(start);
        if (gs.getNormSq() < MathHelper.EPSILON * MathHelper.EPSILON) return Optional.empty();
        List<AbstractObstacle> canCollide = new ArrayList<>();
        for (AbstractObstacle obstacle : obstacles) {
            if (obstacle.getAABB().isIncluded(start, goal)) {
                canCollide.add(obstacle);
            }
        }
        double gsNorm = FastMath.min(gs.getNorm(), limit);
        double mod = MathHelper.mod(gsNorm, step);
        for (double l = mod; l <= gsNorm; l += step) {
            if (TimeHelper.now() - updatedTime > timeLimit) {
                return Optional.of(AbstractObstacle.EMPTY);
            }
            Vector2D newPos = MathHelper.normalized(gs).scalarMultiply(l).add(start);
            for (AbstractObstacle obstacle : canCollide) {
                if (obstacle.isCollided(newPos)) {
                    return Optional.of(obstacle);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * ある点がある点に含まれているか
     * @param target ある点
     * @param compared 比較対象
     * @param margin マージン
     * @return 含まれているか
     */
    protected boolean containsPos(@Nonnull Vector2D target, @Nonnull Vector2D compared, double margin) {
        return compared.subtract(target).getNormSq() <= margin * margin;
    }

    /**
     * ある点がある経路に含まれているか
     * @param list ある経路
     * @param margin マージン
     * @param target ある点
     * @return 含まれているか
     */
    protected boolean containsPos(@Nonnull List<Pair<Vector2D, Double>> list, double margin, @Nonnull Vector2D target) {
        for (Pair<Vector2D, Double> compared : list) {
            if (compared.getFirst().subtract(target).getNormSq() <= margin * margin) {
                return true;
            }
        }
        return false;
    }

    /**
     * 始点から直線的に探索したとき，初めに重なった地点の少し前(ギリギリ重ならない点)
     * @param start 始点
     * @param goal 終点
     * @param obstacles 障害物
     * @param step ステップ長
     * @param limit 探索距離限界
     * @param timeLimit 制限時間
     * @param updatedTime 実行実時刻
     * @return 始めて重なった地点の少し前(ギリギリ重ならない点)
     * @param <T> extends AbstractObstacle
     */
    public <T extends AbstractObstacle> Vector2D getFirstCollidedPos(@Nonnull Vector2D start, @Nonnull Vector2D goal,
            @Nonnull List<T> obstacles, double step, double limit, double timeLimit, double updatedTime) {
        Vector2D gs = goal.subtract(start);
        if (gs.getNormSq() < MathHelper.EPSILON * MathHelper.EPSILON) return goal;
        List<AbstractObstacle> canCollide = new ArrayList<>();
        for (AbstractObstacle obstacle : obstacles) {
            if (obstacle.getAABB().isIncluded(start, goal)) {
                canCollide.add(obstacle);
            }
        }
        Vector2D lastPos = start;
        double mod = MathHelper.mod(gs.getNorm(), step);
        for (double l = mod; l <= FastMath.min(gs.getNorm(), limit); l += step) {
            if (TimeHelper.now() - updatedTime > timeLimit) {
                return lastPos;
            }
            Vector2D newPos = MathHelper.normalized(gs).scalarMultiply(l).add(start);
            for (AbstractObstacle obstacle : canCollide) {
                if (obstacle.isCollided(newPos)) {
                    return lastPos;
                }
            }
            lastPos = newPos;
        }
        return goal;
    }

    /**
     *
     * @param path = [{pos0, 0.0}, {pos1, 0.0}, {pos2, base2}, ...., {posN, baseN}]
     * @return = [{pos0, 0.0}, {pos1, 0.0}, {pos2, max2}, ...., {posN, maxN}, {goal, maxN+1}]
     */
    @Nonnull
    public List<Pair<Vector2D, Double>> makeOptimal(@Nonnull LinkedHashMap<Vector2D, Double> path) {
        final Config.PIDController config = ConfigManager.getInstance().getConfig().controllerConfig;
        final double BRAKE = config.brakeToTargetPosition;
        final double MAX_VEL = config.velocityMax;
        List<Pair<Vector2D, Double>> result = new ArrayList<>();
        if (path.isEmpty()) return result;
        LinkedHashMap<Vector2D, Double> copied = new LinkedHashMap<>(path);
        Vector2D tempStart;
        Vector2D tempGoal = copied.pollLastEntry().getKey();
        Vector2D lastStart;
        double additionalLength = 0.0;
        for (int i = path.size() - 2; i > 0; i--) {
            Map.Entry<Vector2D, Double> last = copied.pollLastEntry();
            double baseLength = last.getValue();
            tempStart = last.getKey();
            lastStart = copied.lastEntry().getKey();
            Vector2D side2 = tempGoal.subtract(tempStart);
            Vector2D side1 = tempStart.subtract(lastStart);
            double theta = MathHelper.inferiorAngle(side1, side2);
            double cos = Math.max(0, FastMath.cos(theta));
            double vi = FastMath.min(MAX_VEL,
                    ControlHelper.getVelocityTrapezoidal(BRAKE, side2.getNorm() + additionalLength) * cos);
            additionalLength = FastMath.min(baseLength, 0.5 * vi * vi / BRAKE);
            result.add(new Pair<>(tempGoal, additionalLength));
            tempGoal = tempStart;
        }
        result.add(new Pair<>(tempGoal, 0.0));
        while (!copied.isEmpty()) {
            result.add(new Pair<>(copied.pollLastEntry().getKey(), 0.0));
        }
        return result.reversed();
    }
}
