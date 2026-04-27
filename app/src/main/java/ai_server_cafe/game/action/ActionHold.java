package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.Map;
import java.util.Optional;

public class ActionHold extends AbstractAction {
    /**
     * 動作の状態の表現
     */
    public enum RunningState {
        MOVE,   ///< ボールへ近づく
        ROUND   ///< 回り込み
    }

    // 状態
    private RunningState state;

    // 目標
    private Vector2D target;
    // ボールを持っているか
    private boolean haveBall;
    // 前フレームの角速度
    private double prevOmega;
    // 更新された時刻
    private double lastUpdatedTime;

    private int holdCount;

    public ActionHold(int id, TeamColor color) {
        super(id, color);
        this.state = RunningState.MOVE;
        this.target = Vector2D.ZERO;
        this.haveBall = false;
        this.prevOmega = 0;
        this.lastUpdatedTime = TimeHelper.now();
        this.holdCount = 0;
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;
        // ロボット半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;
        // ドリブルバーの回転速度
        final int DRIBBLE_VALUE = 12;

        // ドリブルできる距離
        final double DRIBBLE_MARGIN = 700.0;

        // 加速度
        final double roundAcc = 2000;
        final double setAcc = 6000;
        final double toBallAcc = 2500;

        final Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        final Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        if (!friendlyRobots.containsKey(this.id)) return command;
        final FilteredRobot robot = friendlyRobots.get(this.id).getRobot();
        final Vector2D robotPos = MathHelper.position2D(robot);
        final Vector2D faceBallPos =
                robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, robot.getTheta()));
        Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();
        final Field wf = this.world.get().getField();
        final boolean isLost = this.world.get().getBall().isLost();
        if (isLost && MathHelper.distance2D(ballPos, faceBallPos) < ROBOT_RAD * 3) {
            ballPos = faceBallPos;
        }

        // ロボットとボールの距離
        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        final double distBtoF = MathHelper.distance2D(faceBallPos, ballPos);

        Vector2D target = this.target;

        double toTargetTheta = MathHelper.direction(target, ballPos);
        final double toBallTheta = MathHelper.direction(ballPos, robotPos);

        // ボールを持っているか
        this.haveBall = MathHelper.inferiorAngle(robot.getTheta(),
                MathHelper.direction(ballPos, robotPos)) < 0.05 * FastMath.PI
                && distBtoR * FastMath.cos(MathHelper.inferiorAngle(robot.getTheta(),
                MathHelper.direction(ballPos, robotPos))) < TO_FACE_RAD + BALL_RAD + 5;

        if (this.haveBall) {
            this.holdCount++;
        } else {
            this.holdCount = 0;
        }

        //制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();

        // 状態遷移
        switch (this.state) {
            case RunningState.MOVE:
                if (haveBall) {
                    this.state = RunningState.ROUND;
                }
                break;

            case RunningState.ROUND:
                if (distBtoR > ROBOT_RAD * 2 || MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) > 0.8 * FastMath.PI) {
                    this.state = RunningState.MOVE;
                }
                break;
        }
        double now = TimeHelper.now();
        if (now - this.lastUpdatedTime > 0.5) {
            // 前回呼ばれたときから時間が経っていたら
            this.state = RunningState.MOVE;
        }
        this.lastUpdatedTime = now;

        Vector2D roundVel;
        Vector2D toBallVel;
        Vector2D ballFlow = ballVel.scalarMultiply(haveBall ? 0.9 : 0.99);

        // 動作
        switch (this.state) {
            case RunningState.MOVE:
                if (distBtoR > WithPlanner.PENALTY_AREA_THRESHOLD) {
                    command.setTargetPosition(ballPos.add(ballVel.scalarMultiply(0.32)).add(MathHelper.normalized(robotPos.subtract(ballPos)).scalarMultiply(TO_FACE_RAD + BALL_RAD)));
                } else if (MathHelper.distance2D(robotPos, ballPos) < ROBOT_RAD * 2.0
                        && MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) > 0.15 * FastMath.PI) {
                    command.setTargetVel(ballVel);
                } else {
                    toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos)).scalarMultiply(FastMath.sqrt(2 * toBallAcc * distBtoF));
                    command.setTargetVel(toBallVel.add(ballVel));
                }
                command.setTargetTheta(MathHelper.direction(ballPos.subtract(robotPos)));
                break;

            case RunningState.ROUND:
                //  ロボットの目標角度を決める
                Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);
                if (oppRobot.isPresent()) {
                    Vector2D oppPos = oppRobot.get().getRobot().position();
                    if (this.haveBall && MathHelper.distance2D(oppPos, ballPos) < 1000) {
                        // 敵ロボットがボールの近くにいるとき
                        // ボールを持っているとき
                        // 敵から離れる方向
                        toTargetTheta = MathHelper.direction(ballPos, robotPos);
                    } else if (MathHelper.distance2D(oppPos, ballPos) < 500 && MathHelper.distance2D(oppPos, ballPos) < distBtoR) {
                        // 敵のすぐ近くにボールがあって、敵のほうがボールに近いとき
                        toTargetTheta = MathHelper.direction(oppPos, ballPos);
                    } else {
                        toTargetTheta = MathHelper.direction(ballPos, robotPos);
                    }
                } else {
                    toTargetTheta = MathHelper.direction(this.target, ballPos);
                }

                // 角速度の制御
                final double toThetaAlpha = 15.0;
                final double alpha = 15.0;
                double omega =
                        FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                        MathHelper.inferiorAngle(toTargetTheta, robot.getTheta()))),
                                MathHelper.wrapPI(toTargetTheta - robot.getTheta()));
                omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                command.setTargetOmega(omega);
                this.prevOmega = omega;
                double roundOmega = FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha * MathHelper.inferiorAngle(ballPos.subtract(target), robotPos.subtract(ballPos)))),
                        MathHelper.directionFrom(ballPos.subtract(target), robotPos.subtract(ballPos)));
                roundVel = MathHelper.applyRotation2D(MathHelper.normalized(robotPos.subtract(ballPos)), 0.5 * FastMath.PI).scalarMultiply(roundOmega * distBtoR);
                if (MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) > 0.15 * FastMath.PI) {
                    toBallVel = MathHelper.normalized(ballPos.subtract(robotPos)).scalarMultiply(FastMath.sqrt(2 * toBallAcc * FastMath.max(0, distBtoR - ROBOT_RAD - BALL_RAD)));
                } else {
                    toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos)).scalarMultiply(FastMath.sqrt(2 * toBallAcc * distBtoF));
                }
                command.setTargetVel(roundVel.add(toBallVel).add(ballFlow));

                break;
        }

        if (distBtoR < ROBOT_RAD * 2 && MathHelper.distance2D(ballPos, this.world.get().getDribbleStartPos(this.color)) > DRIBBLE_MARGIN) {
            // オーバードリブル対策
            command.setTargetPosEmpty();
            command.setTargetVel(Vector2D.ZERO);
            command.setTargetTheta(MathHelper.direction(ballPos, robotPos));
        } else if (this.haveBall && this.holdCount > 5) {
            command.setTargetPosition(new Vector2D(wf.getPenaltyFrontX() - ROBOT_RAD, robotPos.getY()));
            command.setTargetTheta(MathHelper.direction(this.target, ballPos));
        }

        command.setDribble(DRIBBLE_VALUE);

        return command;
    }

    @Override
    public String getName() {
        return "hold";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setTarget(Vector2D target) {
        this.target = target;
    }
}
