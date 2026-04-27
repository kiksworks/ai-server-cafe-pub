package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.Map;

public class ActionMitoma extends AbstractAction {

    /**
     * 動作の状態の表現
     */
    public enum RunningState {
        ROUND,   ///< 回り込み
        HOLD,    ///< ボールを持って待つ
        DRIBBLE, ///< ドリブル
        KICK     ///< キック
    }

    // 状態
    private RunningState state;
    // 目標
    private Vector2D target;
    // マニュアルモードにおけるキックの種類・パワー
    private Pair<EnumKickType, Integer> manualKickFlag;
    // 自動モードにおけるキックスピード (line，chip)
    private Pair<Double, Double> autoKickSpeed;
    // キックをマニュアルモードにするか
    private boolean kickManually;
    // チップで蹴ったか
    private boolean chipped;
    // ボールを持っているか
    private boolean haveBall;
    // 前フレームの角速度
    private double prevOmega;
    // キックするまでのカウント
    private int count;
    // ターゲット固定
    private Vector2D fixedTarget;
    // 更新された時刻
    private double lastUpdatedTime;

    public ActionMitoma(int id, TeamColor color) {
        super(id, color);
        this.state = RunningState.ROUND;
        this.target = Vector2D.ZERO;
        this.manualKickFlag = new Pair<>(EnumKickType.STRAIGHT, 20);
        this.autoKickSpeed = new Pair<>(1000.0, 1000.0);
        this.kickManually = false;
        this.chipped = false;
        this.haveBall = false;
        this.prevOmega = 0;
        this.count = 0;
        this.fixedTarget = Vector2D.ZERO;
        this.lastUpdatedTime = TimeHelper.now();
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
        // ドリブラーの長さ
        final double DRIBBLER_LENGTH = ConfigManager.getInstance().getConfig().dribblerLength;
        // ドリブルバーの回転速度
        final int DRIBBLE_VALUE = 12;
        // ドリブルできる距離
        final double DRIBBLE_MARGIN = 600;
        final double DRIBBLE_LIMIT = 800;

        // 回り込み半径
        final double ROUND_RAD = ROBOT_RAD + BALL_RAD * 3;
        // 回り込み最大速度
        final double ROUND_SPEED = 600.0;

        // 加速度
        final double roundAcc = 2000;
        final double setAcc = 6000;
        final double toTargetAcc = 6000;
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

        // ロボットとボールの距離
        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        final double distBtoF = MathHelper.distance2D(faceBallPos, ballPos);

        final Vector2D target = this.target;

        final double toTargetTheta = MathHelper.direction(target, ballPos);
        final double toBallTheta = MathHelper.direction(ballPos, robotPos);

        // ロボット正中線からのボールの距離
        final double distFromCenter =
                distBtoR * FastMath.sin(MathHelper.inferiorAngle(robot.getTheta(),
                MathHelper.direction(ballPos, robotPos)));

        // ロボット正面からのボールの距離
        final double distFromFace =
                distBtoR * FastMath.cos(MathHelper.inferiorAngle(robot.getTheta(),
                MathHelper.direction(ballPos, robotPos))) - TO_FACE_RAD - BALL_RAD;

        // ボールを持っているか
        this.haveBall = distBtoR < TO_FACE_RAD + BALL_RAD ||
                (distFromCenter < 0.5 * DRIBBLER_LENGTH && -10.0 < distFromFace && distFromFace < 10.0);

        //制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();

        Vector2D dribbleStartPos = this.world.get().getDribbleStartPos(this.color);

        // 状態遷移
        switch (this.state) {

            case RunningState.ROUND:
                if (haveBall && MathHelper.inferiorAngle(toTargetTheta, robot.getTheta()) < 0.05 * FastMath.PI) {
                    this.count = 0;
                    this.fixedTarget = target;
                    this.state = RunningState.HOLD;
                }
                break;

            case RunningState.HOLD:
                if (distFromCenter > 0.5 * DRIBBLER_LENGTH) {
                    this.state = RunningState.ROUND;
                } else if (this.count > 10) {
                    this.state = RunningState.DRIBBLE;
                }
                break;

            case RunningState.DRIBBLE:
                if (MathHelper.distance2D(robotPos, ballPos) > ROBOT_RAD * 2
                        || distFromCenter > 0.5 * DRIBBLER_LENGTH) {
                    this.state = RunningState.ROUND;
                } else if (MathHelper.distance2D(dribbleStartPos, ballPos) > DRIBBLE_MARGIN) {
                    this.state = RunningState.KICK;
                }
                break;

            case RunningState.KICK:
                if (distFromCenter > 0.5 * DRIBBLER_LENGTH || distFromFace > 4 * BALL_RAD) {
                    this.state = RunningState.ROUND;
                } else if (MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(fixedTarget, robotPos)) > 0.2 * FastMath.PI) {
                    this.state = RunningState.ROUND;
                }
                break;
        }
        double now = TimeHelper.now();
        if (now - this.lastUpdatedTime > 0.5) {
            // 前回呼ばれたときから時間が経っていたら
            this.state = ActionMitoma.RunningState.ROUND;
        }
        this.lastUpdatedTime = now;

        this.count ++;

        Vector2D roundVel;
        Vector2D toBallVel;
        Vector2D pushVel;
        // 動作
        switch (this.state) {

            case RunningState.ROUND:
                if (MathHelper.inferiorAngle(toTargetTheta, robot.getTheta()) < 0.05 * FastMath.PI
                        && MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) < 0.05 * FastMath.PI) {
                    // 回り込む必要がないとき
                    // 直接ボールへ向かう
                    toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos))
                            .scalarMultiply(FastMath.sqrt(2 * roundAcc * distBtoF));
                    command.setTargetVel(toBallVel.add(ballVel));
                    command.setTargetTheta(toTargetTheta);
                    break;
                }

                final double toThetaAlpha = 15.0;
                final double alpha = 15.0;

                double roundTheta = MathHelper.wrapPI(toTargetTheta - toBallTheta);
                if (haveBall ||
                        (distFromCenter < 0.5 * DRIBBLER_LENGTH && -10.0 < distFromFace && distFromFace < 6 * BALL_RAD)) {
                    double omega =
                            FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                    FastMath.abs(roundTheta))), roundTheta);
                    omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                    this.prevOmega = omega;
                    command.setTargetOmega(omega);
                } else {
                    command.setTargetTheta(toTargetTheta);
                }

                double roundOmega = FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha * FastMath.abs(roundTheta))),
                        roundTheta);
                roundVel = MathHelper.applyRotation2D(MathHelper.normalized(robotPos.subtract(ballPos)),
                        0.5 * FastMath.PI).scalarMultiply(Math.clamp(roundOmega * distBtoR, -ROUND_SPEED, ROUND_SPEED));
                if (distFromCenter < 0.5 * DRIBBLER_LENGTH) {
                    toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos)).scalarMultiply(FastMath.sqrt(2 * roundAcc * FastMath.max(0, distBtoR - TO_FACE_RAD - BALL_RAD)));
                } else {
                    toBallVel = MathHelper.normalized(ballPos.subtract(robotPos)).scalarMultiply(FastMath.sqrt(2 * roundAcc * FastMath.max(0, distBtoR - ROUND_RAD)));
                }
                command.setTargetVel(roundVel.add(toBallVel).add(ballVel));
                break;

            case RunningState.HOLD:
                toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos)).scalarMultiply(FastMath.sqrt(2 * setAcc * distBtoF));
                // ボールを押す
                pushVel = MathHelper.normalized(ballPos.subtract(robotPos)).scalarMultiply(1000);
                command.setTargetVel(toBallVel.add(pushVel));
                command.setTargetTheta(MathHelper.direction(this.fixedTarget, robotPos));
                break;

            case RunningState.DRIBBLE:
                pushVel = MathHelper.normalized(fixedTarget.subtract(ballPos)).scalarMultiply(
                            FastMath.min(FastMath.sqrt(2 * toTargetAcc * MathHelper.distance2D(fixedTarget, ballPos)),
                                    FastMath.sqrt(2 * toTargetAcc * FastMath.max(0, (DRIBBLE_MARGIN - MathHelper.distance2D(dribbleStartPos, ballPos))))));
                command.setTargetVel(pushVel);
                command.setTargetTheta(MathHelper.direction(fixedTarget, robotPos));
                break;

            case RunningState.KICK:
                command.setTargetVel(Vector2D.ZERO);
                command.setTargetTheta(MathHelper.direction(this.fixedTarget, robotPos));
                kick(robotPos, robot.velocity().dotProduct(MathHelper.getFromPolar(1, robot.getTheta())), oppositeRobots, command);
                break;

        }

        if (distBtoR < ROBOT_RAD * 2 && MathHelper.distance2D(ballPos, this.world.get().getDribbleStartPos(this.color)) > DRIBBLE_LIMIT) {
            // オーバードリブル対策
            if (!this.isPenaltyKick) {
                command.setTargetPosition(this.world.get().getDribbleStartPos(this.color).add(robotPos).scalarMultiply(0.5));
                command.setTargetTheta(MathHelper.direction(ballPos, robotPos));
            } else {
                command.setTargetPosEmpty();
                command.setTargetVel(Vector2D.ZERO);
            }
        } else if (this.state != RunningState.KICK || this.isPenaltyKick) {
            command.setDribble(DRIBBLE_VALUE);
        }

        return command;
    }

    /**
     * 目標位置を設定する
     * @param x 目標位置のx座標
     * @param y 目標位置のy座標
     */
    public void setTarget(double x, double y) {
        this.target = new Vector2D(x, y);
    }

    /**
     * 目標位置を設定する
     * @param target 目標位置の座標
     */
    public void setTarget(Vector2D target) {
        this.target = target;
    }

    /**
     * キックを自動モードに設定する
     */
    public  void kickAutomatically() {
        this.kickManually = false;
    }

    /**
     * キックを自動モード設定し、同時にキックパワーを変更する
     * @param speed キックパワー。この値はキックの種類に関係なく適用される
     */
    public void kickAutomatically(double speed) {
        this.kickManually = false;
        this.autoKickSpeed = new Pair<Double, Double>(speed, speed);
    }

    /**
     * キックを自動モードに設定し、同時にキックパワーを変更する。
     * @param lineSpeed ラインキック時のキックスピード
     * @param chipSpeed チップキック時のキックスピード
     */
    public void kickAutomatically(double lineSpeed, double chipSpeed) {
        this.kickManually = false;
        this.autoKickSpeed = new Pair<Double, Double>(lineSpeed, chipSpeed);
    }

    /**
     * キックをマニュアルモードに設定する。
     */
    public void kickManually() {
        this.kickManually = true;
    }

    /**
     * キックをマニュアルモードに設定し、同時にキックパワーを変更する。
     * @param pow キックパワー
     */
    public void kickManually(int pow) {
        this.kickManually = true;
        this.manualKickFlag = new Pair<>(this.manualKickFlag.getFirst(), pow);
    }

    /**
     * キックをマニュアルモードに設定し、同時にキックタイプを変更する。
     * @param type キックタイプ
     */
    public void kickManually(EnumKickType type) {
        this.kickManually = true;
        this.manualKickFlag = new Pair<>(type, this.manualKickFlag.getSecond());
    }

    /**
     * キックをマニュアルモードに設定し、同時にキックコマンドを変更する。
     * @param kick キックフラグ
     */
    public void kickManually(Pair<EnumKickType, Integer> kick) {
        this.kickManually = true;
        this.manualKickFlag = kick;
    }

    /**
     * 現在の目標位置を返す。
     * @return 現在の目標位置
     */
    public Vector2D target() {
        return this.target;
    }

    /**
     * チップで蹴ったかを返す。
     * @return チップで蹴った場合は true
     */
    public boolean chipped() {
        return this.chipped;
    }

    @Override
    public String getName() {
        return "mitoma";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    /**
     *
     * @param robotPosition ロボットの位置
     * @param forwardVel ロボットの前方向の速度成分
     * @param opponentRobots 相手ロボット
     * @param command コマンド
     */
    private void kick(final Vector2D robotPosition, final Double forwardVel, final Map<Integer, IntegratedRobot> opponentRobots, Command command) {
        if (kickManually) {
            command.setKickFlag(manualKickFlag);
            this.chipped = manualKickFlag.getFirst() == EnumKickType.CHIP;
            return;
        }
        final double robotRad = ConfigManager.getInstance().getConfig().robotRadius;
        // チップが有効そうな距離
        final double radius = FastMath.min(MathHelper.distance2D(robotPosition, this.target), 1000.0);
        final boolean flag = opponentRobots.values().stream().anyMatch(tmpRobot -> {
            final Vector2D oppPos = MathHelper.position2D(tmpRobot.getRobot());
            final double theta = MathHelper.inferiorAngle(oppPos.subtract(robotPosition), this.target.subtract(robotPosition));
            final double distFromOpp = MathHelper.distance2D(oppPos, robotPosition);
            return distFromOpp * FastMath.sin(theta) < robotRad * 2 &&
                    0 < FastMath.cos(theta) &&
                    distFromOpp * FastMath.cos(theta) < MathHelper.distance2D(this.target, robotPosition) - robotRad * 2 &&
                    distFromOpp < radius;
        });
        if (flag) {
            command.setKickFlag(EnumKickType.CHIP, KickConverter.toPower(id, new Pair<>(EnumKickType.CHIP, autoKickSpeed.getSecond())));
        } else {
            command.setKickFlag(EnumKickType.STRAIGHT,
                    KickConverter.toPower(id, new Pair<>(EnumKickType.STRAIGHT, autoKickSpeed.getFirst())));
        }
    }
}