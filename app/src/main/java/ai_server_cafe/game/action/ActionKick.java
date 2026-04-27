package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.Map;
import java.util.Optional;

public class ActionKick extends AbstractAction {

    /**
     * 動作の状態の表現
     */
    public enum RunningState {
        ROUND,   ///< 回り込み
        HOLD,    ///< ボールを持って待つ
        DRIFT,   ///< ボールを持ったまま動く
        KICK     ///< キック
    }

    // 状態
    private RunningState state;
    // 目標
    private Vector2D target;
    // マニュアルモードにおけるキックの種類・パワー
    private Pair<EnumKickType, Integer> manualKickFlag;
    // 自動モードにおけるキックパワー (line時のキックパワー，chip時のキックパワー)
    private Pair<Integer, Integer> autoKickPow;
    // キックをマニュアルモードにするか
    private boolean kickManually;
    // チップで蹴ったか
    private boolean chipped;
    // ボールを持っているか
    private boolean haveBall;
    // ボールを持っているか(ドリフト用)
    private boolean haveBallDrift;
    // 前フレームの角速度
    private double prevOmega;
    // キックするまでのカウント
    private int count;
    // セットプレーか
    private boolean isSetPlay;
    // キーパーか
    private boolean isKeeper;
    // ターゲット固定
    private Vector2D fixedTarget;
    // 更新された時刻
    private double lastUpdatedTime;
    // キックする前に待つ時間
    private double holdTime;
    // キックするときにボールを押す速度
    private double pushSpeed;
    // ドリフト目標
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Vector2D> driftTarget;
    // クリアか
    private boolean isClear;
    // 邪魔しようとしている敵の位置
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Vector2D> oppPos;
    // キックするときにドリブラーを回すか？
    private boolean dribbleKickFlag;

    // 前フレームの回り込み角度
    private double lastRoundTheta;

    public ActionKick(int id, TeamColor color) {
        super(id, color);
        this.state = RunningState.ROUND;
        this.target = Vector2D.ZERO;
        this.manualKickFlag = new Pair<>(EnumKickType.STRAIGHT, 45);
        this.autoKickPow = new Pair<>(200, 200);
        this.kickManually = true;
        this.chipped = false;
        this.haveBall = false;
        this.haveBallDrift = false;
        this.prevOmega = 0;
        this.count = 0;
        this.isSetPlay = false;
        this.isKeeper = false;
        this.fixedTarget = Vector2D.ZERO;
        this.lastUpdatedTime = TimeHelper.now();
        this.holdTime = 20;
        this.driftTarget = Optional.empty();
        this.pushSpeed = 500.0;
        this.isClear = false;
        this.oppPos = Optional.empty();
        this.dribbleKickFlag = true;
        this.lastRoundTheta = 0.0;
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
        final double DRIBBLE_MARGIN = 700.0;

        final double FACE_THETA = 0.4 * FastMath.tan(0.5 * DRIBBLER_LENGTH / TO_FACE_RAD);

        // 回り込み半径
        final double ROUND_RAD = ROBOT_RAD + BALL_RAD * 3;
        // 回り込み最大速度
        final double ROUND_SPEED = 800.0;

        // 加速度
        final double roundAcc = this.isSetPlay ? 1200 : 4000;
        final double setAcc = 2000;

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

        // ロボットとボールの距離
        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        final double distBtoF = MathHelper.distance2D(faceBallPos, ballPos);

        final Vector2D target = this.target;

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

        // ドリフト用haveBall(直前にtrueだったらballLostでもtrueにする)
        this.haveBallDrift = (this.haveBallDrift && (this.world.get().getBall().isLost() || distBtoF < 180.0)) || haveBall;

        final double toTargetTheta = MathHelper.direction(target, robotPos);
        final double toBallTheta = MathHelper.direction(ballPos, robotPos);

        //制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();

        // 状態遷移
        switch (this.state) {

            case RunningState.ROUND:
                double thetaMargin = 0.01 * FastMath.PI;
                if (this.isClear) {
                    // isClear クリアなら適当に打つ
                    thetaMargin = 0.05 * FastMath.PI;
                } else if (
                    StrategyHelper.isLineInterrupted(ballPos, ROBOT_RAD + BALL_RAD, target, oppositeRobots.values().stream().toList())) {
                    // ドリフト？
                    thetaMargin = 0.2 * FastMath.PI;
                }
                if (haveBall && (robot.velocity().subtract(ballVel).getNorm() < 500 || holdTime < 1)
                        && MathHelper.inferiorAngle(toTargetTheta, robot.getTheta()) < thetaMargin) {
                    if (this.isSetPlay && !haveBall) break;
                    this.count = 0;
                    this.state = RunningState.HOLD;
                }
                break;

            case RunningState.HOLD:
                Command fakeCommand = new Command();
                kick(robotPos, oppositeRobots, friendlyRobots, fakeCommand);
                boolean isInterrupted = StrategyHelper.isLineInterrupted(ballPos, ROBOT_RAD + BALL_RAD, fixedTarget, oppositeRobots.values().stream().toList());
                if (!world.get().getBall().isLost() && MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) > 0.3 * FastMath.PI) {
                    this.state = RunningState.ROUND;
                } else if (this.count > this.holdTime && (fakeCommand.getKickFlag().getFirst() == EnumKickType.CHIP || !isInterrupted)) {
                    this.count = 0;
                    this.state = RunningState.KICK;
                } else if (this.driftTarget.isPresent() && this.count > 45
                        && isInterrupted) {
                    this.state = RunningState.DRIFT;
                }
                break;

            case RunningState.DRIFT:
                fakeCommand = new Command();
                kick(robotPos, oppositeRobots, friendlyRobots, fakeCommand);
                if ((driftTarget.isEmpty()
                        || !StrategyHelper.isLineInterrupted(ballPos, ROBOT_RAD + BALL_RAD, fixedTarget, oppositeRobots.values().stream().toList())
                        || MathHelper.distance2D(ballPos, this.world.get().getDribbleStartPos(this.color)) > DRIBBLE_MARGIN)
                    && MathHelper.inferiorAngle(MathHelper.direction(this.fixedTarget, robotPos), robot.getTheta()) < 0.01 * FastMath.PI) {
                    this.count = 0;
                    this.state = RunningState.KICK;
                } else if (!haveBallDrift && !world.get().getBall().isLost()) {
                    this.state = RunningState.ROUND;
                } else if (MathHelper.distance2D(fixedTarget, target) > 2 * ROBOT_RAD) {
                    // ターゲットが変わったとき
                    this.state = RunningState.ROUND;
                }
                break;

            case RunningState.KICK:
                if (this.count < 20) break;

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
            // 状態をリセット
            this.state = RunningState.ROUND;
            // 角速度の制限をリセット
            this.prevOmega = robot.getOmega();
        }
        this.lastUpdatedTime = now;

        this.count ++;

        Vector2D roundVel;
        Vector2D toBallVel;
        Vector2D pushVel;
        Vector2D ballFlow = this.isSetPlay ? Vector2D.ZERO : ballVel.scalarMultiply(haveBall ? 0.9 : 0.99);
        // 動作
        switch (this.state) {

            case RunningState.ROUND:

                this.fixedTarget = this.target;

                if (!this.isSetPlay
                        && MathHelper.inferiorAngle(toTargetTheta, toBallTheta) < FACE_THETA
                        && MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) < FACE_THETA) {
                    // 回り込む必要がないとき
                    // 直接ボールへ向かう
                    toBallVel = MathHelper.normalized(ballPos.subtract(robotPos))
                            .scalarMultiply(FastMath.sqrt(2 * roundAcc * FastMath.max(0, distBtoR - TO_FACE_RAD - BALL_RAD)));
                    command.setTargetVel(toBallVel.add(ballVel));
                    command.setTargetTheta(toTargetTheta);
                    break;
                }

                // ロボットの向きの回転速度制限用
                final double toThetaAlpha = 15.0;
                final double alpha = 30.0;

                // 回り込み調整用
                final double k_p = 100.0;
                final double k_d = 5.0;

                double roundTheta = MathHelper.wrapPI(toTargetTheta - toBallTheta);
                command.setTargetTheta(toTargetTheta);

                double roundOmega = k_p * roundTheta + k_d * (roundTheta - this.lastRoundTheta) / cycle;
                this.lastRoundTheta = roundTheta;
                // ボール中心に回る速度
                if (haveBall && MathHelper.inferiorAngle(toTargetTheta, robot.getTheta()) < 0.1 * FastMath.PI) {
                    roundVel = Vector2D.ZERO;
                } else {
                    roundVel = MathHelper.applyRotation2D(MathHelper.normalized(robotPos.subtract(ballPos)),
                            0.5 * FastMath.PI).scalarMultiply(Math.clamp(roundOmega * distBtoR, -ROUND_SPEED, ROUND_SPEED));
                }

                // ボールから離れないようにする速度
                if (MathHelper.inferiorAngle(toBallTheta, robot.getTheta()) > FACE_THETA || (this.isSetPlay && distFromCenter > 10.0)) {
                    toBallVel = MathHelper.normalized(ballPos.subtract(robotPos))
                            .scalarMultiply(FastMath.sqrt(2 * roundAcc * Math.max(0, distBtoR - ROUND_RAD)));
                } else {
                    toBallVel = MathHelper.normalized(ballPos.subtract(robotPos))
                            .scalarMultiply(FastMath.sqrt(2 * roundAcc * FastMath.max(0, distBtoR - TO_FACE_RAD - BALL_RAD)));
                }

                // ボールを押す速度
                if (haveBall && ballVel.dotProduct(MathHelper.getFromPolar(1.0, robot.getTheta())) < pushSpeed) {
                    pushVel = MathHelper.getFromPolar(pushSpeed, robot.getTheta());
                } else {
                    pushVel = Vector2D.ZERO;
                }


                if (distBtoR > 2000) {
                    // ボールが遠いときは回り込まない
                    command.setTargetVel(toBallVel.add(pushVel).add(ballVel));
                } else {
                    command.setTargetVel(roundVel.add(toBallVel).add(pushVel).add(ballVel));
                }
                break;

            case RunningState.HOLD:
                toBallVel = MathHelper.normalized(ballPos.subtract(faceBallPos)).scalarMultiply(FastMath.sqrt(2 * setAcc * distBtoF));
                // ドリフト前はボールを押す
                pushVel = this.driftTarget.isPresent()
                        ? MathHelper.getFromPolar(pushSpeed, robot.getTheta()) : Vector2D.ZERO;
                command.setTargetVel(toBallVel.add(pushVel));
                command.setTargetTheta(MathHelper.direction(this.fixedTarget, robotPos));
                break;

            case RunningState.DRIFT:
                if (StrategyHelper.isLineInterrupted(faceBallPos, ROBOT_RAD + BALL_RAD, fixedTarget, oppositeRobots.values().stream().toList())) {
                    command.setTargetPosition(driftTarget.orElse(robotPos));
                } else {
                    command.setTargetPosition(robotPos);
                }
                command.setTargetTheta(MathHelper.direction(this.fixedTarget, robotPos));
                break;

            case RunningState.KICK:
                pushVel = MathHelper.getFromPolar(pushSpeed, robot.getTheta());
                command.setTargetVel(pushVel);
                command.setTargetTheta(MathHelper.direction(this.fixedTarget, robotPos));
                kick(robotPos, oppositeRobots, friendlyRobots, command);
                break;

        }

        if (this.isSetPlay) {
            if (MathHelper.inferiorAngle(MathHelper.direction(ballPos.subtract(robotPos)), robot.getTheta()) < 0.5 * FastMath.PI) {
                kick(robotPos, oppositeRobots, friendlyRobots, command);
            }
        }

        if ((distBtoR < ROBOT_RAD * 2 || !world.get().getBall().isLost())
                && MathHelper.inferiorAngle(MathHelper.direction(this.fixedTarget.subtract(robotPos)), robot.getTheta()) < 0.01
                && FastMath.abs(robot.getOmega()) < 0.2
                && !StrategyHelper.isLineInterrupted(ballPos, ROBOT_RAD + BALL_RAD, target, oppositeRobots.values().stream().toList())) {
            // ロボットの向きがあっていて、ロボットが回転していないとき
            // とりあえずキックフラグON (本当にいいのか？)
            // TODO check
            kick(robotPos, oppositeRobots, friendlyRobots, command);
        }

        if (distBtoR < ROBOT_RAD * 6 && (this.dribbleKickFlag || this.state != RunningState.KICK)) {
            command.setDribble(DRIBBLE_VALUE);
        }

        if (!this.isSetPlay && distBtoR < ROBOT_RAD * 2 && MathHelper.distance2D(ballPos, this.world.get().getDribbleStartPos(this.color)) > DRIBBLE_MARGIN && !this.isPenaltyKick) {
            // オーバードリブル対策
            command.setTargetPosition(this.world.get().getDribbleStartPos(this.color).add(robotPos).scalarMultiply(0.5));
            command.setTargetTheta(MathHelper.direction(ballPos, robotPos));
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
     * セットプレー用の設定にするか
     */
    public void setIsSetPlay(boolean flag) {
        this.isSetPlay = flag;
    }

    /**
     * キーパー用の設定にするか
     */
    public void setIsKeeper(boolean flag) {
        this.isKeeper = flag;
    }

    /**
     * キックを自動モードに設定する
     */
    public  void kickAutomatically() {
        this.kickManually = false;
    }

    /**
     * キックを自動モード設定し、同時にキックパワーを変更する
     * @param pow キックパワー。この値はキックの種類に関係なく適用される
     */
    public void kickAutomatically(int pow) {
        this.kickManually = false;
        this.autoKickPow = new Pair<Integer, Integer>(pow, pow);
    }

    /**
     * キックを自動モードに設定し、同時にキックパワーを変更する。
     * @param linePow ラインキック時のキックパワー
     * @param chipPow チップキック時のキックパワー
     */
    public void kickAutomatically(int linePow, int chipPow) {
        this.kickManually = false;
        this.autoKickPow = new Pair<Integer, Integer>(linePow, chipPow);
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

    public void setPushSpeed(double speed) {
        this.pushSpeed = speed;
    }

    public RunningState getState() {
        return this.state;
    }

    /**
     * ボールを持ったまま動く目標を設定する
     * @param target 目標位置
     */
    public void setDriftTarget(Optional<Vector2D> target) {
        this.driftTarget = target;
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

    /**
     * キックする前に待つ時間を設定する
     * @param time
     */
    public void setHoldTime(double time) {
        this.holdTime = time;
    }

    /**
     * クリア用の設定にするかを設定する
     * @param flag クリアならtrue
     */
    public void setIsClear(boolean flag) {
        this.isClear = flag;
    }

    /**
     * 邪魔になりそうな敵の位置を設定
     * @param pos 敵ロボットの位置
     */
    public void setOppPos(Optional<Vector2D> pos) {
        this.oppPos = pos;
    }

    /**
     * キックするときにドリブラーを回すかどうかを設定
     * @param flag trueなら回す
     */
    public void setDribbleKickFlag(boolean flag) {
        this.dribbleKickFlag = flag;
    }

    /**
     * ボールを持ったまま動いているか
     * @return ボールを持ったまま動いている場合は true
     */
    public boolean drifting() {
        return this.state == RunningState.DRIFT;
    }

    /**
     * ターゲットが変わったときに呼ぶ関数
     * fixedTargetを解除
     */
    public void resetTarget() {
        this.state = RunningState.ROUND;
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    private void kick(final Vector2D robotPosition, final Map<Integer, IntegratedRobot> opponentRobots, final Map<Integer, IntegratedRobot> friendlyRobots,  Command command) {
        if (kickManually) {
            command.setKickFlag(manualKickFlag);
            this.chipped = manualKickFlag.getFirst() == EnumKickType.CHIP;
            return;
        }
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final double MARGIN = ROBOT_RAD * 2;
        // チップ打つか？
        final boolean flag =
                (this.isKeeper || this.isSetPlay || this.isClear || MathHelper.distance2D(robotPosition, target) > 2500)
                && (opponentRobots.values().stream().anyMatch(
                InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                    @Override
                    public Boolean function(IntegratedRobot robot) {
                        return MathHelper.distancePositionToRobot(robotPosition, robot.getRobot()) > ROBOT_RAD * 4
                                && MathHelper.isCollidedWithSegment(robotPosition, target, robot.getRobot().position(), MARGIN);
                    }
                }))
                || friendlyRobots.values().stream().anyMatch(
                InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                    @Override
                    public Boolean function(IntegratedRobot robot) {
                        return MathHelper.isCollidedWithSegment(
                                robotPosition.add(MathHelper.normalized(target.subtract(robotPosition)).scalarMultiply(MARGIN * 2)),
                                target.add(MathHelper.normalized(robotPosition.subtract(target)).scalarMultiply(MARGIN * 2)),
                                robot.getRobot().position(), MARGIN);
                    }
                })));
        if (flag) {
            command.setKickFlag(EnumKickType.CHIP, autoKickPow.getSecond());
        } else {
            command.setKickFlag(EnumKickType.STRAIGHT, autoKickPow.getFirst());
        }
    }
}