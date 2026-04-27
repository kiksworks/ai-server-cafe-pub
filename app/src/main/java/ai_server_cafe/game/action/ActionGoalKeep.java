package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class ActionGoalKeep extends AbstractAction {

    private boolean finished;
    private Vector2D prevRobotPos;
    private long lastDirect;
    private int chipPow;
    private Deque<Vector2D> ballVelBuffer;

    public ActionGoalKeep(int id, TeamColor color) {
        super(id, color);
        this.finished = false;
        this.prevRobotPos = Vector2D.ZERO;
        this.lastDirect = 0L;
        this.chipPow = 255;
        this.ballVelBuffer = new ArrayDeque<>();
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;
        final Field wf = this.world.get().getField();

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final IntegratedRobot robot = this.world.get().getFriendlyRobotMap(this.color).get(id);
        final Vector2D robotPos = robot.getRobot().position();
        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();
        Vector2D ballVel2 = ballVel;

        final Vector2D goalLeft = new Vector2D(wf.getMinX(), wf.getGoalMaxY());
        final Vector2D goalRight = new Vector2D(wf.getMinX(), wf.getGoalMinY());
        final double toLeft = MathHelper.direction(goalLeft.add(new Vector2D(0.0, ROBOT_RAD)).subtract(ballPos));
        final double toRight = MathHelper.direction(goalRight.add(new Vector2D(0.0, -ROBOT_RAD)).subtract(ballPos));
        this.ballVelBuffer.addLast(ballVel);
        if (this.ballVelBuffer.size() > 5) {
            this.ballVelBuffer.pollFirst();
        }
        Vector2D average = Vector2D.ZERO;
        for (Vector2D vector2D : this.ballVelBuffer) {
            average = average.add(vector2D);
        }
        final Vector2D copied = average.scalarMultiply(1.0 / this.ballVelBuffer.size());
        List<Vector2D> median = this.ballVelBuffer.stream().sorted(Comparator.comparingDouble(new ToDoubleFunction<Vector2D>() {
            @Override
            public double applyAsDouble(Vector2D value) {
                return value.dotProduct(copied);
            }
        })).toList();
        if (!median.isEmpty()) {
            ballVel2 = median.get(median.size() / 2);
        }
        boolean flag = ballVel.getNorm() > 1000 && MathHelper.isRightOf(MathHelper.direction(ballVel), toRight) && MathHelper.isLeftOf(MathHelper.direction(ballVel), toLeft);
        boolean flag2 = ballVel2.getNorm() > 1000 && MathHelper.isRightOf(MathHelper.direction(ballVel2), toRight) && MathHelper.isLeftOf(MathHelper.direction(ballVel2), toLeft);
        if (flag && MathHelper.inferiorAngle(ballVel2, ballVel) > 0.5) {
            this.ballVelBuffer.clear();
            this.ballVelBuffer.add(ballVel);
            ballVel2 = ballVel;
        }

        if (flag || flag2) {
            // ボールがゴールに向かってきているとき
            Vector2D movePos = ballPos.add(MathHelper.normalized(ballVel2)
                    .scalarMultiply(FastMath.max(0, MathHelper.normalized(ballVel2).dotProduct(robotPos.subtract(ballPos)))));

            if (MathHelper.isCollidedWithBox(
                    new Vector2D(wf.getMinX(), wf.getMinY()), new Vector2D(wf.getMaxX(), wf.getMaxY()), ballPos, 0)) {
                for (int i = 0; i < 30; i++) {
                    if (MathHelper.isCollidedWithBox(
                            new Vector2D(wf.getMinX(), wf.getMinY()), new Vector2D(wf.getMaxX(), wf.getMaxY()), movePos, ROBOT_RAD)) {
                        break;
                    }
                    // フィールド内に収める
                    movePos = movePos.add(MathHelper.normalized(ballPos.subtract(movePos)).scalarMultiply(2 * ROBOT_RAD));
                }
            }

            // 無理やり間に合わせる
            final double GAIN =
                    MathHelper.distance2D(ballPos, movePos) / ballVel2.getNorm()
                            < MathHelper.distance2D(robotPos, movePos) / robot.getRobot().velocity().getNorm() ? 0.005 : 0;
            Vector2D robotVel = robot.getRobot().velocity();
            Vector2D r = movePos.subtract(robotPos);

            Vector2D estimatedVel = robotPos.subtract(this.prevRobotPos).scalarMultiply(60.0);
            Vector2D normalizedRobotVel = MathHelper.normalized(robotVel);
            Vector2D filteredEstimatedVel = normalizedRobotVel.scalarMultiply(estimatedVel.dotProduct(normalizedRobotVel));
            double v0 = filteredEstimatedVel.dotProduct(MathHelper.normalized(r));
            // どうせ3000.0もでない
            double brake = 2000.0;
            double accel = 2000.0;
            double maxVel = ConfigManager.getInstance().getConfig().controllerConfig.velocityMax;
            double moveTime = getTrapezoidalTime(v0, r.getNorm(), accel, brake, maxVel);
            double ballTime = movePos.subtract(ballPos).getNorm() / ballVel2.getNorm();
            if (MathHelper.isNanOrInfinity(ballTime)) {
                ballTime = 0;
            }
            // ボール速度は途中で上がる 係数 0.7
            boolean isPosition = moveTime + 0.1 < 0.7 * ballTime || r.getNorm() <= 30.0;
            if (!isPosition) {
                this.lastDirect = this.now;
            }

            if (this.now - this.lastDirect > 8L) {
                // 普通に取る
                command.setTargetPosition(movePos);
                command.setTargetTheta(MathHelper.direction(ballPos, movePos));
            } else {
                // ダイレクトアタック
                double targetVel = r.getNorm() / ballTime; // TODO ボールの減衰率が必要かも？
                if (MathHelper.isNanOrInfinity(targetVel)) {
                    targetVel = 6000.0;
                }
                if (targetVel > filteredEstimatedVel.getNorm() + 100.0) {
                    command.setTargetVel(MathHelper.normalized(r).scalarMultiply(maxVel));
                } else {
                    double gainP = 0.5;
                    command.setTargetVel(MathHelper.normalized(r).scalarMultiply(targetVel * (1.0 + gainP)));
                }
                // 向きを指定しない方が安定するかも？
                // command.setTargetTheta(MathHelper.direction(ballPos, movePos));
            }

            if (command.getTargetPos().isEmpty()) {
                Vector2D targetVel = command.getTargetVel();
                Vector2D error = targetVel.subtract(filteredEstimatedVel);
                command.setTargetVel(targetVel.add(error.scalarMultiply(2.0)));
            }

            command.setDribble(12);
            command.setKickFlag(EnumKickType.CHIP, this.chipPow);
        } else {
            Vector2D goalCenter = wf.getBackGoalCenter();
            Vector2D movePos = goalCenter.add(MathHelper.normalized(ballPos.subtract(goalCenter))
                    .scalarMultiply(1.8 * wf.getGoalWidth() * MathHelper.inferiorAngle(MathHelper.direction(ballPos, goalCenter) / FastMath.PI, 0)));
            movePos = new Vector2D(FastMath.max(movePos.getX(), wf.getMinX() + ROBOT_RAD), movePos.getY());
            command.setTargetPosition(movePos);
            command.setTargetTheta(MathHelper.direction(ballPos, movePos));
        }

        this.prevRobotPos = robotPos.scalarMultiply(1.0D);

        return command;
    }

    private static double getTrapezoidalTime(double r, double accel, double brake, double maxVel) {
        double maxTriangle = 0.5 * maxVel * maxVel / accel + 0.5 * maxVel * maxVel / brake;
        if (r <= maxTriangle) {
            return FastMath.sqrt(2.0 * r / (accel + brake));
        }
        return maxVel / accel + maxVel / brake + (r - maxTriangle) / maxVel;
    }

    private static double getTrapezoidalTime(double v0, double r, double accel, double brake, double maxVel) {
        double baseR = v0 > 0.0 ? r + 0.5 * v0 * v0 / accel : r + 0.5 * v0 * v0 / brake;
        double baseT = getTrapezoidalTime(baseR, accel, brake, maxVel);
        double finalT = baseT + ((v0 > 0.0) ? -v0 / accel : v0 / brake);
        if (finalT < 0.0) {
            if (r < 0.0) {
                return 0.0;
            }
            return getTrapezoidalTime(-v0, -r, accel, brake, maxVel);
        }
        return finalT;
    }

    @Override
    public String getName() {
        return "goalKeep";
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    /**
     * チップキックのパワーを設定する
     * @param chipPow チップパワー
     */
    public void setChipPow(int chipPow) {
        this.chipPow = chipPow;
    }

}