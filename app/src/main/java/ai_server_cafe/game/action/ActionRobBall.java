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

import java.util.Optional;

public class ActionRobBall extends AbstractAction {

    /**
     * 動作の状態の表現
     */
    private enum RobState{
        MOVE,    ///< ボールに近づく
        ROB,     ///< ボールに向かって直進する
        LEAVE,   ///< ボールを持って去る
        ROTATE,  ///< 相手ゴール方向を向く
        FINISHED ///< 動作終了
    }
    private int hasBallCount;
    private RobState state;
    // 前フレームの角速度
    private double prevOmega;
    private double targetTheta;
    // robし出した場所
    private Vector2D startPos;
    private Vector2D targetPos;
    // 更新された時刻
    private double lastUpdatedTime;

    // 前フレームの回り込み角度
    private double lastRoundTheta;

    public ActionRobBall(int id, TeamColor color) {
        super(id, color);
        this.hasBallCount = 0;
        this.state = RobState.MOVE;
        this.prevOmega = 0.0;
        this.targetTheta = 0.0;
        this.startPos = Vector2D.ZERO;
        this.targetPos = Vector2D.ZERO;
        this.lastUpdatedTime = TimeHelper.now();
        this.lastRoundTheta = 0.0;
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;
        final Field wf = this.world.get().getField();
        final FilteredRobot robot = this.world.get().getFriendlyRobotMap(this.color).get(this.id).getRobot();
        final double FACE_DIST = ConfigManager.getInstance().getConfig().toFaceRadius;
        // ドリブラーの長さ
        final double DRIBBLER_LENGTH = ConfigManager.getInstance().getConfig().dribblerLength;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;

        final double FACE_THETA = 0.4 * FastMath.tan(0.5 * DRIBBLER_LENGTH / FACE_DIST);

        // ドリブルできる距離
        final double DRIBBLE_MARGIN = 700.0;
        // ronできる距離
        final double ROB_MARGIN = DRIBBLE_MARGIN*0.7;

        // 回り込み半径
        final double ROUND_RAD = ROBOT_RAD + BALL_RAD;
        // 回り込み最大速度
        final double ROUND_SPEED = 800.0;

        // 加速度
        final double toBallAcc = 4000;
        final double roundAcc = 3000;

        //制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();

        final Vector2D robotPos = robot.position();
        final Vector2D faceBallPos = robotPos.add(MathHelper.getFromPolar(FACE_DIST + BALL_RAD, robot.getTheta()));

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        double height = ConfigManager.getInstance().getConfig().maxFieldHeight;
        double width = ConfigManager.getInstance().getConfig().maxFieldWidth;
        if(!MathHelper.isCollidedWithBox(new Vector2D(-0.5 * width, -0.5 * height), new Vector2D(0.5 * width, 0.5 * height), ballPos, 0))
            return command;

        // ロボットとボールの距離
        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        final double distBtoF = MathHelper.distance2D(faceBallPos, ballPos);

        // ロボット正中線からのボールの距離
        final double distFromCenter =
                distBtoR * FastMath.sin(MathHelper.inferiorAngle(robot.getTheta(),
                MathHelper.direction(ballPos, robotPos)));

        final double RtoBtheta = MathHelper.direction(ballPos, robotPos);
        final boolean hasBall = (distBtoF < 35.0 ||
                (MathHelper.inferiorAngle(robot.getTheta(), RtoBtheta) < FastMath.PI/180*11 &&
                        distBtoR < BALL_RAD + FACE_DIST));

        Optional<IntegratedRobot> oppositeRobot = MathHelper.nearestRobotToPosition(this.world.get().getOppositeRobotMap(this.color), ballPos);
        if(oppositeRobot.isEmpty()) {
            return command;
        }
        final Vector2D oppPos = oppositeRobot.get().getRobot().position();

        // 状態遷移
        double dTheta = MathHelper.inferiorAngle(ballPos.subtract(robotPos), faceBallPos.subtract(robotPos));
        final double BtoOtheta = MathHelper.direction(oppPos, ballPos);
        if (MathHelper.HALF_PI < dTheta || 50.0 < distBtoR * FastMath.sin(dTheta) || distBtoF > 50.0) {
            this.state = RobState.MOVE;
        }
        switch (this.state) {
            case MOVE :
                if(distBtoF < 2*BALL_RAD && MathHelper.inferiorAngle(RtoBtheta, robot.getTheta()) < FastMath.PI/180 * 6) {
                    this.hasBallCount = 0;
                    this.state = RobState.ROB;
                    this.targetTheta = MathHelper.wrapPI(RtoBtheta +
                            FastMath.copySign(FastMath.PI/6, MathHelper.wrapPI(BtoOtheta - robot.getTheta())));
                    break;
                }
                break;
            case ROB:
                /*
                if(this.hasBallCount > 60) {
                    this.state = RobState.LEAVE;
                    this.hasBallCount = 0;
                    this.startPos = robotPos;
                    this.targetPos = robotPos.add(MathHelper.getFromPolar(ROB_MARGIN, this.targetTheta));
                    break;
                }
                 */
                break;
            case LEAVE:
                if(this.hasBallCount > 90 || MathHelper.distance2D(oppPos, robotPos) > ROB_MARGIN
                        || MathHelper.distance2D(this.startPos, robotPos) > ROB_MARGIN
                        || MathHelper.distance2D(this.targetPos, robot) < 50.0) {
                    this.state = RobState.ROTATE;
                    this.hasBallCount = 0;
                    break;
                } else if (distBtoF > 200.0) {
                    this.state = RobState.MOVE;
                    this.hasBallCount = 0;
                    break;
                }
                break;
            case ROTATE:
                if(FastMath.abs(robot.getTheta()-FastMath.PI) < FastMath.PI/180 * 6) {
                    this.state = RobState.FINISHED;
                    this.hasBallCount = 0;
                    break;
                } else if (distBtoF > 200.0) {
                    this.state = RobState.MOVE;
                    this.hasBallCount = 0;
                    break;
                }
                break;
            case FINISHED:
                if (distBtoF > 200.0) {
                    this.state = RobState.MOVE;
                    this.hasBallCount = 0;
                    break;
                }
                break;
            default:
                break;
        }

        double now = TimeHelper.now();
        if (now - this.lastUpdatedTime > 0.5) {
            // 前回呼ばれたときから時間が経っていたら
            // 状態をリセット
            this.state = RobState.MOVE;
            // 角速度の制限をリセット
            this.prevOmega = robot.getOmega();
        }
        this.lastUpdatedTime = now;

        this.hasBallCount++;

        // 制御

        double omega;

        // ロボットの向きの回転速度制限用
        final double toThetaAlpha = 15.0;
        final double alpha = 30.0;

        // 回り込み調整用
        final double k_p = 100.0;
        final double k_d = 5.0;

        // 回り込む速度
        Vector2D roundVel;
        // ボールに向かう速度
        Vector2D toBallVel;
        Vector2D pushVel;
        final double pushSpeed = 500.0;
        // ボールの速さ
        Vector2D ballFlow = ballVel.scalarMultiply(hasBall ? 0.3 : 0.9);

        // MOVEしか使わない
        this.state = RobState.MOVE;

        switch (this.state) {
            case MOVE:
                double roundTheta = MathHelper.wrapPI(BtoOtheta - RtoBtheta);
                double roundOmega = k_p * roundTheta + k_d * (roundTheta - this.lastRoundTheta) / cycle;
                this.lastRoundTheta = roundTheta;
                // ボール中心に回る速度
                roundVel = MathHelper.applyRotation2D(MathHelper.normalized(robotPos.subtract(ballPos)),
                        0.5 * FastMath.PI).scalarMultiply(Math.clamp(roundOmega * distBtoR, -ROUND_SPEED, ROUND_SPEED));

                // ボールから離れないようにする速度
                if (MathHelper.inferiorAngle(RtoBtheta, robot.getTheta()) > FACE_THETA) {
                    toBallVel = MathHelper.normalized(ballPos.subtract(robotPos))
                            .scalarMultiply(FastMath.sqrt(2 * roundAcc * Math.max(0, distBtoR - ROUND_RAD)));
                } else {
                    toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos))
                            .scalarMultiply(FastMath.sqrt(2 * roundAcc * FastMath.max(0, distBtoR - FACE_DIST - BALL_RAD)));
                }

                // ボールを押す速度
                if (MathHelper.inferiorAngle(BtoOtheta, RtoBtheta) < 0.2 * Math.PI && ballVel.dotProduct(MathHelper.getFromPolar(1.0, RtoBtheta)) < pushSpeed) {
                    pushVel = MathHelper.getFromPolar(pushSpeed, RtoBtheta);
                } else {
                    pushVel = Vector2D.ZERO;
                }

                if (distBtoF < 0.5 * DRIBBLER_LENGTH || hasBall) {
                    command.setTargetTheta(BtoOtheta);
                } else {
                    command.setTargetTheta(RtoBtheta);
                }

                if (distBtoR > 2000) {
                    // ボールが遠いときは回り込まない
                    command.setTargetVel(toBallVel.add(pushVel).add(ballVel));
                } else {
                    command.setTargetVel(roundVel.add(toBallVel).add(pushVel).add(ballVel));
                }

                break;
            case ROB :
                if (MathHelper.inferiorAngle(robot.getTheta(), RtoBtheta) < FastMath.PI/180*5
                        && distBtoF < 2*BALL_RAD) {
                    omega = FastMath.copySign(FastMath.min(5.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                    MathHelper.inferiorAngle(this.targetTheta, robot.getTheta()))),
                            MathHelper.wrapPI(this.targetTheta - robot.getTheta()));
                    omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                    command.setTargetOmega(omega);
                    this.prevOmega = omega;
                    command.setTargetVel(MathHelper.getFromPolar(500.0, robot.getTheta()));
                } else {
                    omega = FastMath.copySign(FastMath.min(5.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                    MathHelper.inferiorAngle(RtoBtheta, robot.getTheta()))),
                            MathHelper.wrapPI(RtoBtheta - robot.getTheta()));
                    omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                    command.setTargetOmega(omega);
                    this.prevOmega = omega;
                    command.setTargetVel(MathHelper.getFromPolar(500.0, RtoBtheta));
                }
                break;
            case LEAVE:
                final double toTargetTheta = MathHelper.direction(robotPos, oppPos);
                omega = FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                MathHelper.inferiorAngle(toTargetTheta, robot.getTheta()))),
                        MathHelper.wrapPI(toTargetTheta - robot.getTheta()));
                omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                command.setTargetOmega(omega);
                this.prevOmega = omega;
                command.setTargetVel(MathHelper.getFromPolar(500.0, RtoBtheta));
                break;
            case ROTATE:
                command.setTargetVel(MathHelper.getFromPolar(500.0, RtoBtheta));
                omega = FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                MathHelper.inferiorAngle(FastMath.PI, robot.getTheta()))),
                        MathHelper.wrapPI(FastMath.PI - robot.getTheta()));
                omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                command.setTargetOmega(omega);
                this.prevOmega = omega;
                break;
            case FINISHED:
                command.setTargetVel(Vector2D.ZERO);
                command.setTargetOmega(0.0);
                this.prevOmega = 0.0;
                break;
            default:
                break;
        }
        command.setDribble(distBtoR < 1000 ? 12 : 0);

        if (hasBall && MathHelper.distance2D(ballPos, this.world.get().getDribbleStartPos(this.color)) > DRIBBLE_MARGIN) {
            // オーバードリブル対策
            command.setTargetVel(Vector2D.ZERO);
        }
        return command;
    }

    @Override
    public String getName() {
        return "rob_ball";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
