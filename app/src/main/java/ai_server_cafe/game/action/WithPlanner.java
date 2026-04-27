package ai_server_cafe.game.action;

import ai_server_cafe.config.Config;
import ai_server_cafe.controller.PIDController;
import ai_server_cafe.game.planner.path.DataSet;
import ai_server_cafe.game.planner.path.EnumPathPlannerType;
import ai_server_cafe.game.planner.path.PathSide;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.field.obstacle.ObstacleParallelogram;
import ai_server_cafe.model.field.obstacle.ObstacleTriangle;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.dynamic.IDynamic;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.DynamicsManager;
import ai_server_cafe.updater.UpdaterCurrentObstacle;
import ai_server_cafe.updater.UpdaterPathPlanner;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.interfaces.WrapperCloneableList;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WithPlanner<T extends AbstractAction> extends AbstractAction {
    /**
     * ActionKick, ActionMitomaなどペナルティエリア付近でチャタるのを対策する, 移動するときに速度制御にならない範囲
     */
    public static final double PENALTY_AREA_THRESHOLD = 1000.0;
    public static final boolean USE_OLD_PLANNER = false;
    public static final EnumPathPlannerType PLANNER_TYPE =
            USE_OLD_PLANNER ? EnumPathPlannerType.TIME_DIJKSTRA_LQ : EnumPathPlannerType.TIME_DIJKSTRA_B;

    private static final Logger LOGGER = LogManager.getLogger("planner");
    private final T action;
    /**
     * 障害物の速度を考慮する時間
     */
    private final double dt;
    private final List<AbstractObstacle> obstacles;
    private final boolean avoiding;
    private final double defaultStep;
    private final double minimumStep;
    private final double fluctuationRange;
    private final double pathVelThreshold;
    private final double restrictionMargin;
    private boolean allowMove;

    public WithPlanner(@Nonnull T action, int id, @Nonnull TeamColor color, @Nonnull List<AbstractObstacle> obstacles,
            boolean avoidance) {
        super(id, color);
        this.action = action;
        this.allowMove = true;
        this.dt = 10.0 * ConfigManager.getInstance().getConfig().getCycleTime();
        this.obstacles = new ArrayList<>();
        this.avoiding = avoidance;
        for (AbstractObstacle ao : obstacles) {
            AbstractObstacle copied = ao.clone();
            if (copied instanceof IDynamic) {
                ((IDynamic) copied).setDt(this.dt);
            }
            this.obstacles.add(copied);
        }
        Config.PathPlannerConfig.WithPlannerConfig config =
                ConfigManager.getInstance().getConfig().pathPlannerConfig.withPlannerConfig;
        this.defaultStep = config.defaultStep;
        this.minimumStep = config.minimumStep;
        this.fluctuationRange = config.fluctuationStepRange;
        this.pathVelThreshold = config.velocityApplyToPlannerThreshold;
        this.restrictionMargin = config.restrictionObstacleMargin;
    }

    public WithPlanner(@Nonnull T action, int id, @Nonnull TeamColor color, @Nonnull List<AbstractObstacle> obstacles) {
        this(action, id, color, obstacles, true);
    }

    /**
     * 改めてぶつからない速度で運用するために追加距離を求める
     * @param pos 現在地
     * @param resultVel 経路から得られた速度
     * @param obstacles 障害物
     * @param max 最大追加距離
     * @param step ステップ長
     * @return この関数によって制限された速度
     */
    private static double additionalLength(@Nonnull Vector2D pos, @Nonnull Vector2D resultVel,
            @Nonnull List<AbstractObstacle> obstacles, double max, double step) {
        double K = 1.0;
        double result = max / K;
        Vector2D v1 = MathHelper.normalized(resultVel);
        List<AbstractObstacle> avoidanceList = new ArrayList<>();
        for (AbstractObstacle obstacle : obstacles) {
            // 埋まっていないものについて考える
            if (!obstacle.isCollided(pos)) {
                avoidanceList.add(obstacle);
            }
        }
        label:
        for (double l = step; l <= result; l += step) {
            Vector2D p1 = v1.scalarMultiply(l).add(pos);
            for (AbstractObstacle obstacle : avoidanceList) {
                if (obstacle.isCollided(p1)) {
                    result = l - step;
                    break label;
                }
            }
        }
        return FastMath.min(K * result, max); // ぎりぎりは困るので
    }

    /**
     * 経路全体の総距離
     * @param path 経路
     * @return 総距離
     */
    private static double totalLength(@Nonnull List<PathSide> path) {
        double result = 0.0;
        for (PathSide side : path) {
            result += side.side.getNorm();
        }
        return result;
    }

    public static double getMinAccelRatio() {
        double alpha = FastMath.toRadians(ConfigManager.getInstance().getConfig().wheelAngle);
        return FastMath.abs(FastMath.sin(2.0 * alpha)) / (2.0 * FastMath.sin(alpha));
    }

    /**
     * JapanOpen 2025で使った制御
     *
     * @param result 経路
     * @param start 現在地
     * @param brakeAcc 減速度
     * @param accMax 加速度
     * @return 中間目的地
     */
    @Nonnull
    private Vector2D makeNewPosA(@Nonnull List<PathSide> result, Vector2D start, double brakeAcc, double accMax,
            boolean isCollided) {
        Vector2D newPos = result.get(0).side.add(start);
        if (result.size() >= 2) {
            Vector2D side = result.get(0).side;
            Vector2D secondSide = result.get(1).side;
            Vector2D additionalVec = result.get(1).additionalVec;
            double tempVel = ControlHelper.getVelocityTrapezoidal(brakeAcc, additionalVec.getNorm());
            double ratio = DynamicsManager.getInstance().getCentripetalRatio(this.id, false);
            double perpendicularAcc = (result.size() >= 3 ? 1.2 : 1.0) * ratio * accMax;
            double sin = MathHelper.getSin(side, secondSide);
            double rVel = (MathHelper.isEpsilon(sin) || MathHelper.isNanOrInfinity(sin)) ? Double.MAX_VALUE :
                    FastMath.sqrt(0.5 * secondSide.getNorm() * perpendicularAcc / sin);
            tempVel = FastMath.min(rVel, tempVel);
            additionalVec = MathHelper.normalized(additionalVec)
                    .scalarMultiply(ControlHelper.getDistanceTrapezoidal(brakeAcc, tempVel));
            newPos = side.add(start).add(additionalVec);
        }
        UpdaterPathPlanner.getInstance().setNextVel(this.color, this.id, newPos.subtract(start));
        return newPos;
    }

    /**
     * 研究で最適と出た制御
     * @param result 経路
     * @param start 現在地
     * @param brakeAcc 減速度
     * @param accMax 加速度
     * @param isCollided 始点が埋まっているか
     * @return 中間目的地
     */
    @Nonnull
    private Vector2D makeNewPosB(@Nonnull List<PathSide> result, Vector2D start, double brakeAcc, double accMax,
            boolean isCollided) {
        Vector2D newPos = result.get(0).side.add(start);
        if (result.size() >= 2) {
            Vector2D side = result.get(0).side;
            Vector2D additionalVec = result.get(1).additionalVec;
            double tempVel = ControlHelper.getVelocityTrapezoidal(brakeAcc, additionalVec.getNorm());
            additionalVec = MathHelper.normalized(additionalVec)
                    .scalarMultiply(ControlHelper.getDistanceTrapezoidal(brakeAcc, tempVel));
            newPos = side.add(start).add(additionalVec);
        }
        UpdaterPathPlanner.getInstance().setNextVel(this.color, this.id, newPos.subtract(start));
        return newPos;
    }

    /**
     * 制御をタイプによって切り替える
     * @param type 経路探索手法
     * @param result 経路
     * @param start 現在地
     * @param brakeAcc 減速度
     * @param accMax 加速度
     * @param isCollided 現在地が障害物に埋まっているかどうか
     * @return 中間目的地
     */
    private Vector2D makeNewPos(EnumPathPlannerType type, @Nonnull List<PathSide> result, Vector2D start,
            double brakeAcc, double accMax, boolean isCollided) {
        if (type == EnumPathPlannerType.TIME_DIJKSTRA_B) {
            return this.makeNewPosB(result, start, brakeAcc, accMax, isCollided);
        } else {
            return this.makeNewPosA(result, start, brakeAcc, accMax, isCollided);
        }
    }

    public double getAccelRatio(double theta) {
        double alpha = FastMath.toRadians(ConfigManager.getInstance().getConfig().wheelAngle);
        return (FastMath.abs(FastMath.sin(alpha - theta)) + FastMath.abs(FastMath.sin(alpha + theta))) /
                (2.0 * FastMath.sin(alpha));
    }

    /**
     * Whether robot can move (default == true)
     *
     * @param flag allow to move
     * @return this
     */
    public WithPlanner<T> setAllowMove(boolean flag) {
        this.allowMove = flag;
        return this;
    }

    public Command update(long now) {
        // 下位にも流す
        this.action.now = now;
        return super.update(now);
    }

    @Override
    protected Command execute() {
        Command command = this.action.execute();
        UpdaterCurrentObstacle.getInstance().put(this.color, this.id, new WrapperCloneableList<>(this.obstacles).get());
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id)) {
            return command;
        }
        if (!UpdaterPathPlanner.getInstance().containsKeyStep(this.color, this.id)) {
            UpdaterPathPlanner.getInstance().setStep(this.color, this.id, this.defaultStep);
        }
        double step = UpdaterPathPlanner.getInstance().getStep(this.color, this.id, this.defaultStep);
        IntegratedRobot robot = this.world.get().getFriendlyRobotMap(this.color).get(this.id);
        Vector2D start = robot.getRobot().position();
        double theta = robot.getRobot().getTheta();
        Vector2D vel = robot.getRobot().velocity();
        Vector2D pathVel = vel.getNorm() < this.pathVelThreshold ? Vector2D.ZERO : vel;
        double velMax = ConfigManager.getInstance().getConfig().controllerConfig.velocityMax;
        double brakeAcc = ConfigManager.getInstance().getConfig().controllerConfig.brakeToTargetPosition;
        double accMax = ConfigManager.getInstance().getConfig().controllerConfig.accelToTargetVelocity;
        double brakeMax = ConfigManager.getInstance().getConfig().controllerConfig.brakeToTargetVelocity;
        double accAngMax = ConfigManager.getInstance().getConfig().controllerConfig.accelToTargetVelAngular;
        double velAngMax = ConfigManager.getInstance().getConfig().controllerConfig.velAngularMax;
        PIDController predictor = new PIDController(ConfigManager.getInstance().getConfig().getCycleTime(),
                ConfigManager.getInstance().getConfig().controllerConfig);
        double totalDist = 0.0;
        boolean isCollided = false;
        boolean isCollidedRestriction = false;
        for (AbstractObstacle abstractObstacle : this.obstacles) {
            if (abstractObstacle.expandMargin(this.restrictionMargin).isCollided(start)) {
                isCollidedRestriction = true;
            }
            if (abstractObstacle.isCollided(start)) {
                isCollided = true;
                isCollidedRestriction = true;
                break;
            }
        }
        // hasResult == false のとき何もしない
        if (command.getTargetPos().isPresent()) {
            Vector2D goal = command.getTargetPos().get();
            // 位置司令の場合
            UpdaterPathPlanner.getInstance().setDataset(this.color, this.id,
                    new DataSet(start, goal, this.obstacles, step, 80, 20, Optional.of(pathVel), id), PLANNER_TYPE);
            boolean hasResult = UpdaterPathPlanner.getInstance().hasResult(this.color, this.id);
            List<PathSide> result = UpdaterPathPlanner.getInstance().getResult(this.color, this.id).get();
            if (hasResult && !result.isEmpty()) {
                Vector2D newPos = this.makeNewPos(PLANNER_TYPE, result, start, brakeMax, velMax, isCollidedRestriction);
                command.setTargetPosition(newPos);
                totalDist = totalLength(result);
                if (newPos.subtract(start).getNorm() < MathHelper.EPSILON &&
                        start.subtract(goal).getNorm() > MathHelper.EPSILON) {
                    if (step - this.fluctuationRange >= this.minimumStep)
                        UpdaterPathPlanner.getInstance().setStep(this.color, this.id, step - this.fluctuationRange);
                } else {
                    if (step != this.defaultStep)
                        UpdaterPathPlanner.getInstance().setStep(this.color, this.id, this.defaultStep);
                }
            } else {
                return command.setTargetPosition(start);
            }
        } else {
            Vector2D targetVel = command.getTargetVel();
            Vector2D goal = MathHelper.normalized(targetVel)
                    .scalarMultiply(FastMath.min(velMax * velMax, targetVel.getNormSq()) / (0.8 * brakeAcc)).add(start);
            if (targetVel.getNormSq() < MathHelper.EPSILON) {
                goal = start.scalarMultiply(1.0);
            }
            UpdaterPathPlanner.getInstance().setDataset(this.color, this.id,
                    new DataSet(start, goal, this.obstacles, step, 80, 20, Optional.of(pathVel), id), PLANNER_TYPE);
            boolean hasResult = UpdaterPathPlanner.getInstance().hasResult(this.color, this.id);
            List<PathSide> result = UpdaterPathPlanner.getInstance().getResult(this.color, this.id).get();
            if (hasResult && !result.isEmpty()) {
                Vector2D newPos = this.makeNewPos(PLANNER_TYPE, result, start, brakeMax, velMax, isCollidedRestriction);
                Vector2D generatedVel = predictor.targetVelocity(newPos, start, vel);
                command.setTargetVel(newPos.subtract(start).getNormSq() < MathHelper.EPSILON ? Vector2D.ZERO :
                        newPos.subtract(start).normalize().scalarMultiply(isCollided ? generatedVel.getNorm() :
                                FastMath.min(generatedVel.getNorm(), targetVel.getNorm())));
                totalDist = totalLength(result);
                if (newPos.subtract(start).getNorm() < MathHelper.EPSILON &&
                        start.subtract(goal).getNorm() > MathHelper.EPSILON) {
                    if (step - this.fluctuationRange >= this.minimumStep)
                        UpdaterPathPlanner.getInstance().setStep(this.color, this.id, step - this.fluctuationRange);
                } else {
                    if (step != this.defaultStep)
                        UpdaterPathPlanner.getInstance().setStep(this.color, this.id, this.defaultStep);
                }
            } else {
                return command.setTargetVel(Vector2D.ZERO);
            }
        }
        if (!this.allowMove) {
            return new Command().setHalt(true);
        }
        if (isCollided) {
            return command; // 障害物から脱出する必要があるので衝突回避なんて言ってられない
        }
        this.applyRestriction(start, vel, command, 0.5 * velMax * velMax / brakeAcc, brakeAcc);
        if (!this.avoiding) return command;
        // avoidしない方がきれい
        // this.avoidingObstacles(command, start, vel, 0.5 * velMax * velMax / brakeAcc, brakeAcc, velMax);
        return command;
    }

    @Override
    public String getName() {
        return this.action.getName() + "_with_planner";
    }

    @Override
    public boolean isFinished() {
        return this.action.isFinished();
    }

    @Override
    public void setWorld(World world) {
        // 下位にも流す
        this.action.setWorld(world);
        super.setWorld(world);
    }

    @Nonnull
    private Command avoidingObstacles(@Nonnull Command command, Vector2D pos, Vector2D vel, double maxVelLen,
            double brake, double maxVel) {
        if (command.getTargetPos().isPresent()) {
            Vector2D edge = command.getTargetPos().get().subtract(pos);
            double maxLen = FastMath.min(edge.getNorm(), maxVelLen);
            double lt = additionalLength(pos, vel, this.obstacles, maxLen, 20.0);
            double ls = additionalLength(pos, edge, this.obstacles, maxLen, 20.0);
            double l = FastMath.min(lt, ls);
            if (l < maxLen) {
                command.setTargetPosition(pos.add(MathHelper.normalized(edge).scalarMultiply(l)));
            }
        } else {
            Vector2D edge = command.getTargetVel();
            double maxLen = FastMath.min(0.5 * edge.getNorm() * edge.getNorm() / brake, maxVelLen);
            double lt = additionalLength(pos, vel, this.obstacles, maxLen, 20.0);
            double ls = additionalLength(pos, edge, this.obstacles, maxLen, 20.0);
            double l = FastMath.min(lt, ls);
            if (l < maxLen) {
                double clampedVel = FastMath.min(maxVel, ControlHelper.getVelocityTrapezoidal(brake, l));
                command.setTargetVel(MathHelper.normalized(edge).scalarMultiply(clampedVel));
            }
        }
        return command;
    }

    /**
     * 新しいバージョンの速度制約を適用
     *
     * @param start                       現在地
     * @param currentTargetDestinationVec 速度制御のための中間目的地へのベクトル
     * @return 制約を適用した中間目的地へのベクトル
     */
    private Vector2D applyRestriction2(@Nonnull Vector2D start, @Nonnull Vector2D currentTargetDestinationVec) {
        double step = ConfigManager.getInstance().getConfig().pathPlannerConfig.withPlannerConfig.restrictionStep;
        double currentTargetDistance = currentTargetDestinationVec.getNorm();
        ObstacleSegment segment = new ObstacleSegment(start, currentTargetDestinationVec.add(start), 0.5 * step);
        for (AbstractObstacle obstacle : this.obstacles) {
            if (!obstacle.isCollided(start) && obstacle.isCollided(segment, step)) {
                while (currentTargetDistance > 0) {
                    currentTargetDistance -= FastMath.min(step, currentTargetDistance);
                    currentTargetDestinationVec =
                            MathHelper.normalized(currentTargetDestinationVec).scalarMultiply(currentTargetDistance);
                    segment = new ObstacleSegment(start, currentTargetDestinationVec.add(start), 0.5 * step);
                    if (!obstacle.isCollided(segment, step)) {
                        break;
                    }
                }
            }
        }
        return currentTargetDestinationVec;
    }

    /**
     * 古いバージョンの速度制約を適用
     *
     * @param start      現在地
     * @param currentVel 現在速度
     * @param input      入力command
     * @param maxLen     最大探索長
     * @param acc        最大減速度
     */
    private void applyRestriction(@Nonnull Vector2D start, @Nonnull Vector2D currentVel, @Nonnull Command input,
            double maxLen, double acc) {
        double step = ConfigManager.getInstance().getConfig().pathPlannerConfig.withPlannerConfig.restrictionStep;
        Vector2D currentDestination = MathHelper.normalized(input.getTargetVel())
                .scalarMultiply(FastMath.min(maxLen, input.getTargetVel().getNorm()));
        if (input.getTargetPos().isPresent()) {
            currentDestination = MathHelper.normalized(input.getTargetPos().get().subtract(start))
                    .scalarMultiply(FastMath.min(maxLen, input.getTargetPos().get().subtract(start).getNorm()));
        }
        double dirLen = 0.5 * 0.5 * currentVel.getNormSq() / acc;
        Vector2D currentDirection = MathHelper.normalized(currentVel).scalarMultiply(dirLen);
        // visionノイズが乗っているのでdirLenのしきい値は大きめに
        if (currentDestination.getNorm() < step || dirLen < 200.0) {
            return;
        }
        AbstractObstacle op = currentDirection.dotProduct(currentDestination) < 0 ?
                new ObstacleParallelogram(start, currentDirection, currentDestination, 0.0) :
                new ObstacleTriangle(start, start.add(currentDirection), start.add(currentDestination), 0.0);
        Vector2D nVec1 = MathHelper.applyRotation2D(currentDirection, MathHelper.HALF_PI);
        Vector2D des = MathHelper.normalized(currentDestination).scalarMultiply(currentDirection.getNorm());
        Vector2D nVec2 = MathHelper.applyRotation2D(des, MathHelper.HALF_PI);
        AbstractObstacle op1 =
                new ObstacleParallelogram(start.subtract(currentDirection), currentDirection.scalarMultiply(2.0),
                        nVec1.scalarMultiply(FastMath.copySign(1.0, nVec1.dotProduct(currentDestination))), 0.0);
        AbstractObstacle op2 = new ObstacleParallelogram(start.subtract(des), des.scalarMultiply(2.0),
                nVec2.scalarMultiply(FastMath.copySign(1.0, nVec2.dotProduct(currentDirection))), 0.0);
        AbstractObstacle oc = new ObstacleCircle(start, currentDirection.getNorm(), 0.0);
        for (AbstractObstacle abstractObstacle : this.obstacles) {
            if (op.isCollided(abstractObstacle, step) ||
                    (oc.isCollided(abstractObstacle, step) && op1.isCollided(abstractObstacle, step) &&
                            op2.isCollided(abstractObstacle, step))) {
                input.setTargetPosEmpty();
                input.setTargetVel(Vector2D.ZERO);
            }
        }
    }
}
