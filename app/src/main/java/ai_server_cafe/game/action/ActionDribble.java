package ai_server_cafe.game.action;

import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.Map;
import java.util.Optional;

public class ActionDribble extends AbstractAction {

    /**
     * 動作の状態の表現
     */
    public enum RunningState {
        DRIBBLE, MOVE, STOP, NONE, HOLD
    }

    private Optional<Double> optTheta;
    // 状態
    private RunningState state;
    // 目標
    private Vector2D target;
    // 目標位置での速度
    private double offsetVel;
    // バックドリブルをするか
    private boolean backDribble;
    // 前フレームの速度
    private double prevVel;
    // 前フレームの角速度
    private double prevOmega;

    private boolean isBack;
    private Vector2D backStartPos;
    private boolean avoidLeft;
    private boolean haveBall;

    private double stateChangedTime;

    public ActionDribble(int id, TeamColor color) {
        super(id, color);
        this.state = RunningState.NONE;
        this.optTheta = Optional.empty();
        this.offsetVel = 0.0;
        this.backDribble = false;

        this.isBack = false;
        this.backStartPos = Vector2D.ZERO;
        this.haveBall = false;
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;

        // 制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();
        // ロボット半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;

        final double omegaMax = ConfigManager.getInstance().getConfig().dribbleState.omega;
        final double forwardVelMax = ConfigManager.getInstance().getConfig().dribbleState.backVelocityMax;
        final double backVelMax = ConfigManager.getInstance().getConfig().dribbleState.forwardVelocityMax;
        final double forwardAcc = ConfigManager.getInstance().getConfig().dribbleState.forwardAcc;
        final double backAcc = ConfigManager.getInstance().getConfig().dribbleState.backAcc;

        final Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        final Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        final FilteredRobot robot = friendlyRobots.get(this.id).getRobot();
        Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();
        final Vector2D robotPos = this.world.get().getFriendlyRobotMap(color).get(id).getRobot().position();
        final Vector2D faceBallPos = robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, robot.getTheta()));

        // ボールを持っているか
        if (haveBall) {
            this.haveBall = MathHelper.distance2D(ballPos, faceBallPos) < 150
                    && MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(ballPos, robotPos)) < 1.0;
            if (this.world.get().getBall().isLost()) {
                this.haveBall = true;
                ballPos = faceBallPos;
            }
        } else {
            this.haveBall = MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(ballPos, robotPos)) < 0.3 &&
                    MathHelper.distance2D(ballPos, robotPos) < TO_FACE_RAD + BALL_RAD + 10;
        }
        if (!this.haveBall && ballVel.getNorm() > 30) {
            ballPos = ballPos.add(ballVel.scalarMultiply(cycle));
        }

        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        final double distBtoF = MathHelper.distance2D(faceBallPos, ballPos);
        final double toTargetTheta = MathHelper.direction(target, ballPos);
        final double toBallTheta = MathHelper.direction(ballPos, robotPos);

        // 状態遷移
        {
            switch (this.state) {
                case RunningState.NONE: {
                    if (haveBall)
                        this.state = RunningState.DRIBBLE;
                    else
                        this.state = RunningState.MOVE;
                }
                case RunningState.DRIBBLE: {
                    if (!haveBall)
                        this.state = RunningState.MOVE;
                    if (MathHelper.distance2D(ballPos, target) < ROBOT_RAD)
                        this.state = RunningState.STOP;
                    break;
                }
                case RunningState.MOVE: {
                    if (haveBall) {
                        this.state = RunningState.DRIBBLE;
                        prevVel = ballPos.getNorm();
                        if (backDribble) {
                            this.state = RunningState.HOLD;
                            this.stateChangedTime = TimeHelper.now();
                        }
                    }
                    break;
                }
                case RunningState.HOLD: {
                    if (this.stateChangedTime + 0.5 < TimeHelper.now())
                        this.state = RunningState.DRIBBLE;
                    break;
                }
                case RunningState.STOP: {
                    if (MathHelper.distance2D(ballPos, target) > ROBOT_RAD && ballVel.getNorm() < 50 ||
                            (optTheta.isPresent()
                                    && MathHelper.inferiorAngle(robot.getTheta(), this.optTheta.get()) > 0.1))
                        this.state = RunningState.MOVE;
                    break;
                }
            }
        }

        // 目標角度
        double theta = toTargetTheta;
        if (MathHelper.distance2D(ballPos, this.target) < ROBOT_RAD && optTheta.isPresent())
            theta = optTheta.get();

        switch (this.state) {
            case RunningState.DRIBBLE: {
                Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);
                if (oppRobot.isPresent()) {
                    final Vector2D oppPos = MathHelper.position2D(oppRobot.get().getRobot());
                    final double distBtoO = MathHelper.distance2D(oppPos, ballPos);
                    final double distRtoO = MathHelper.distance2D(oppPos, robotPos);
                    final double toOppTheta = MathHelper.direction(oppPos, robotPos);
                    // 進行方向に敵がいるとき
                    if (MathHelper.distancePositionToRobot(robotPos, oppRobot.get().getRobot())
                            < MathHelper.distance2D(robotPos, this.target)
                            && MathHelper.inferiorAngle(theta, MathHelper.direction(oppRobot.get().getRobot().position(), robotPos))
                            < Math.atan2(ROBOT_RAD * 2, MathHelper.distancePositionToRobot(robotPos, oppRobot.get().getRobot()))) {
                        // 避ける方向 角度と速度で決める
                        if (!this.isBack)
                            this.avoidLeft = Math.copySign(500, MathHelper.directionFrom(theta, MathHelper.direction(oppPos.subtract(robotPos))))
                                    + Math.copySign(500, MathHelper.directionFrom(toTargetTheta, toOppTheta))
                                    + robot.velocity().getNorm() * Math.sin(MathHelper.directionFrom(robot.velocity(), oppPos.subtract(robotPos))) >= 0;
                        theta = MathHelper.direction(oppPos.subtract(robotPos)) +
                                Math.asin(Math.min((2 * ROBOT_RAD) / distBtoO, 1)) * (this.avoidLeft ? 1 : -1);
                        if (!this.isBack) {
                            this.isBack = distBtoO < 200
                                    && Math.tan(MathHelper.inferiorAngle(ballPos.subtract(robotPos), oppPos.subtract(robotPos))) * distRtoO < ROBOT_RAD;
                            backStartPos = robotPos;
                        }
                    }

                    // 相手が近いとき、下がる
                    if (this.isBack) {
                        theta = MathHelper.direction(this.target, oppPos) + Math.PI / 4 * (this.avoidLeft ? -1 : 1);
                        this.isBack = distRtoO < 300 && MathHelper.distance2D(this.backStartPos, robotPos) < 500;
                    }
                }
                boolean isBack = this.backDribble || this.isBack || (MathHelper.inferiorAngle(theta, robot.getTheta()) > Math.PI / 2
                        && MathHelper.distance2D(robotPos, this.target) < 500);
                if (!isBack)
                    this.isBack = MathHelper.inferiorAngle(theta, robot.getTheta()) > Math.PI / 2
                            && MathHelper.distance2D(robotPos, this.target) < 500;

                // thetaをロボットの向きにする
                if (this.backDribble)
                    theta = MathHelper.wrapPI(theta + Math.PI);

                // ロボットの移動方向
                double moveTheta = MathHelper.wrapPI(robot.getTheta() + (isBack ? Math.PI : 0));
                // 角速度の制御
                final double alpha = 5.0;
                double omega = FastMath.copySign(FastMath.sqrt(2.0 * alpha *
                        MathHelper.inferiorAngle(theta, robot.getTheta())), MathHelper.wrapPI(theta - robot.getTheta()));
                omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                omega = Math.min(omega, omegaMax);
                command.setTargetOmega(omega);
                this.prevOmega = omega;

                // ドリブル時の加速度
                double toBallAcc;
                double brakeAcc;
                double velMax;
                if (!isBack) {
                    toBallAcc = forwardAcc;
                    brakeAcc = backAcc;
                    velMax = forwardVelMax;
                } else {
                    toBallAcc = backAcc;
                    brakeAcc = forwardAcc;
                    velMax = backVelMax;
                }
                // ボールを中心に回転する速度
                final Vector2D roundVel = MathHelper
                        .applyRotation2D(MathHelper.normalized(robotPos.subtract(ballPos)), 0.5 * FastMath.PI)
                        .scalarMultiply(distBtoR * omega);

                double targetVel = Math.min(ControlHelper.getVelocityTrapezoidal(brakeAcc, MathHelper.distance2D(ballPos, target),
                        this.offsetVel), velMax * Math.max(1 - (MathHelper.inferiorAngle(robot.getTheta(), theta) / 0.5), 0.0));
                Vector2D straightVel = MathHelper.getFromPolar(Math.clamp(
                        FastMath.min(ControlHelper.getVelocityTrapezoidal(brakeAcc, MathHelper.distance2D(ballPos, target),
                                this.offsetVel), targetVel),
                        this.prevVel - toBallAcc * cycle, this.prevVel + toBallAcc * cycle), moveTheta);
                prevVel = straightVel.getNorm();
                if (omega > 2 && MathHelper.inferiorAngle(toTargetTheta, moveTheta) > 0.2) {
                    straightVel = ballVel;
                }
                command.setTargetVel(straightVel.add(roundVel));
                int dribbleValue;
                if (omega < 0.5)
                    dribbleValue = 12;
                else
                    dribbleValue = 8;
                command.setDribble(dribbleValue);
                break;
            }
            case RunningState.MOVE: {
                Vector2D roundVel = MathHelper.getFromPolar(FastMath.sqrt(2 * 3000 * distBtoR), toBallTheta);
                if (MathHelper.distance2D(ballPos, target) < ROBOT_RAD)
                    roundVel = MathHelper.getFromPolar(FastMath.sqrt(2 * 3000 * distBtoR), theta);
                if (distBtoR < 500) {
                    roundVel = MathHelper.getFromPolar(FastMath.sqrt(2 * 1500 * distBtoF), robot.getTheta() + 1.5
                            * MathHelper.directionFrom(MathHelper.direction(ballPos, faceBallPos), robot.getTheta()));
                }
                Vector2D straightVel = Vector2D.ZERO;
                if (MathHelper.inferiorAngle(toBallTheta, toTargetTheta) > FastMath.PI * 0.5
                        && distBtoR < ROBOT_RAD + BALL_RAD + 20)
                    straightVel = MathHelper.normalized(ballPos.subtract(robotPos)).scalarMultiply(-100);
                if (MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(ballPos, robotPos)) < 0.3 &&
                        MathHelper.distance2D(ballPos, robotPos) < TO_FACE_RAD + BALL_RAD + 20)
                    straightVel = straightVel.add(MathHelper.getFromPolar(400, toBallTheta));
                command.setTargetTheta(theta);
                command.setTargetVel(ballVel.add(roundVel).add(straightVel));
                break;
            }
            case RunningState.STOP: {
                command.setTargetVel(Vector2D.ZERO);
                command.setDribble(2);
                break;
            }
            case RunningState.HOLD: {
                command.setTargetVel(MathHelper.getFromPolar(300, toBallTheta));
                command.setDribble(12);
                break;
            }
        }
        return command;
    }

    /**
     * 目標位置を設定する
     *
     * @param x 目標位置のx座標
     * @param y 目標位置のy座標
     */
    public void setTarget(double x, double y) {
        this.target = new Vector2D(x, y);
    }

    /**
     * 目標位置を設定する
     *
     * @param target 目標位置の座標
     */
    public void setTarget(Vector2D target) {
        this.target = target;
    }

    // 後ろにドリブルするか
    public void setBackDribble(boolean backDribble) {
        this.backDribble = backDribble;
    }

    // 目標位置でのロボットの向きを設定する
    public void setAngle(double theta) {
        this.optTheta = Optional.of(theta);
    }

    // 目標位置でのロボットの速度を設定する
    public void setOffsetVel(double offsetVel) {
        this.offsetVel = offsetVel;
    }

    @Override
    public String getName() {
        return "dribble";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
