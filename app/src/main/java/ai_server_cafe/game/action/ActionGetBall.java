package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.Map;
import java.util.Optional;

public class ActionGetBall extends AbstractAction {

    /**
     * 動作の状態の表現
     */
    public enum RunningState {
        MOVE, ROUND, DRIBBLE, FINISHED
    }

    // 状態
    private RunningState state;
    // 目標
    private Vector2D target;
    // 目標位置との許容誤差
    private double kickMargin;
    // マニュアルモードにおけるキックの種類・パワー
    private Pair<EnumKickType, Integer> manualKickFlag;
    // 自動モードにおけるキックパワー (line時のキックパワー，chip時のキックパワー)
    private Pair<Integer, Integer> autoKickPow;
    // キックをマニュアルモードにするか
    private boolean kickManually;
    // チップで蹴ったか
    private boolean chipped;
    // 許容誤差
    private double allow;
    // シュートできそうならキックする特殊条件を使うか
    private boolean isShoot;
    // クリアできそうならキックする特殊条件を使うか
    private boolean isClear;
    // ボールを敵に奪われないように立ち回るか
    private boolean protectBall;
    // ボールを持っているか
    private boolean haveBall;
    private Vector2D haveBallPos;
    // キーパー用の設定にするか
    private boolean isKeeper;
    // 前フレームの角速度
    private double prevOmega;
    // kickするときに溜める用のカウンタ
    private int kickCharge, haveBallCount;

    // シュート時のキックパワー
    private int shootPower;

    private boolean roundToVel;

    public ActionGetBall(int id, TeamColor color, Vector2D target) {
        super(id, color);
        this.state = RunningState.ROUND;
        this.target = target;
        this.kickMargin = 50;
        this.manualKickFlag = new Pair<>(EnumKickType.STRAIGHT, 45);
        this.autoKickPow = new Pair<>(200, 200);
        this.kickManually = true;
        this.chipped = false;
        this.allow = 10;
        this.isShoot = false;
        this.isClear = false;
        this.protectBall = false;
        this.haveBall = false;
        this.haveBallPos = Vector2D.ZERO;
        this.isKeeper = false;
        this.prevOmega = 0.0;
        this.kickCharge = 0;
        this.haveBallCount = 0;
        this.shootPower = 0;
        this.roundToVel = false;

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

        final Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        final Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        if (!friendlyRobots.containsKey(this.id)) return command;
        final FilteredRobot robot = friendlyRobots.get(this.id).getRobot();
        final Vector2D robotPos = MathHelper.position2D(robot);
        final Vector2D robotVel = MathHelper.velocity2D(robot);
        final Vector2D faceBallPos =
                robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, robot.getTheta()));
        Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();
        final Field wf = this.world.get().getField();
        final boolean isLost = this.world.get().getBall().isLost();

        // ボールを持っているか
        haveBall = (faceBallPos.subtract(ballPos)).getNorm() < 35.0 ||
                (MathHelper.inferiorAngle(robot.getTheta(),
                        MathHelper.direction(ballPos, robotPos)) < 0.2 &&
                        MathHelper.distance2D(ballPos, robotPos) < TO_FACE_RAD + BALL_RAD);
        if (!haveBall) {
            this.haveBallCount = 0;
        }

        // ボールを持ったら haveBallPos の更新を止める
        if (!haveBall) this.haveBallPos = ballPos;

        // ロボットとボールの距離
        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        final double distBtoF = MathHelper.distance2D(faceBallPos, ballPos);

        // キック・ドリブル
        boolean kickable = false;
        {
            final double dTheta =
                    MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(this.target, faceBallPos));
            if ((distBtoR < TO_FACE_RAD + BALL_RAD + 200.0 || isLost) &&
                    dTheta < 0.5 * FastMath.PI &&
                    FastMath.abs(MathHelper.distance2D(this.target, faceBallPos) * FastMath.sin(dTheta)) < this.kickMargin) {
                this.kickCharge++;
                final boolean oppRobotFlag = oppositeRobots.values().stream().anyMatch(oppRobot -> MathHelper.distancePositionToRobot(robotPos, oppRobot.getRobot()) < ROBOT_RAD * 3.0);
                // ある程度ホールドしてから打つ (haveBallは遠いときに打たなくなるのを防ぐため)
                // 近くに敵がいるときはすぐに打つ
                if ( this.kickCharge > (oppRobotFlag ? 5 : 10) || this.haveBallCount > 20 ){
                    kick(robotPos, oppositeRobots, command);
                    kickable = true;
                }
            } else {
                this.kickCharge = 0;
            }

            if (distBtoR < TO_FACE_RAD + BALL_RAD + 50.0) command.setDribble(DRIBBLE_VALUE);
        }

        final Vector2D target = this.target;

        final double toTargetTheta = MathHelper.direction(target, ballPos);
        final double toBallTheta = MathHelper.direction(ballPos, robotPos);

        // 状態遷移
        {
            final double dTheta = MathHelper.inferiorAngle(ballPos.subtract(robotPos), faceBallPos.subtract(robotPos));
            switch (this.state) {
                case RunningState.DRIBBLE: {
                    if (!haveBall && (0.5 * FastMath.PI < dTheta || 50.0 < distBtoR * FastMath.sin(dTheta)))
                        this.state = RunningState.ROUND;
                    break;
                }

                case RunningState.ROUND: {
                    if (haveBall) {
                        this.state = RunningState.DRIBBLE;
                    }
                }

            }
        }

        if (this.state == RunningState.DRIBBLE) {
            this.haveBallCount++;
        } else {
            this.haveBallCount = 0;
        }

        // 目標角度
        double theta = toTargetTheta;
        // ドリブルで敵を避けたい
        Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);
        if (this.protectBall && oppRobot.isPresent() && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < ROBOT_RAD * 3) {
            // 敵から避ける動作が有効かつ迫ってきている敵がいるとき
            final Vector2D oppPos = MathHelper.position2D(oppRobot.get().getRobot());
            final double distBtoO = MathHelper.distance2D(oppPos, ballPos);
            if (!haveBall && distBtoO < ROBOT_RAD + BALL_RAD + 500.0 &&
                    this.state != RunningState.DRIBBLE) {
                theta = MathHelper.direction(oppPos, ballPos);
                this.state = RunningState.ROUND;
            }
        }

        boolean flagRoundToVel = false;
        //制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();
        // 速度指令
        switch (this.state) {
            // ボールを蹴る
            case RunningState.DRIBBLE: {
                // 角速度の制御
                final double toThetaAlpha = 10.0;
                final double alpha = 10.0;
                final Optional<IntegratedRobot> opp = oppositeRobots.values().stream().filter(
                        tmpRobot -> MathHelper.distancePositionToRobot(ballPos, tmpRobot.getRobot()) < ROBOT_RAD * 3.0 + BALL_RAD).findFirst();
                double omega =
                        FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha *
                                        MathHelper.inferiorAngle(theta, robot.getTheta()))),
                                MathHelper.wrapPI(theta - robot.getTheta()));
                omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                command.setTargetOmega(omega);
                this.prevOmega = omega;

                // ドリブル時の加速度
                final double toBallAcc = 6000.0;
                final double roundDistFromBall = TO_FACE_RAD + BALL_RAD;
                final Vector2D roundPos =
                        ballPos.add(MathHelper.normalized(robotPos.subtract(ballPos)).scalarMultiply(roundDistFromBall));

                // ボールを中心に回転する速度
                final Vector2D roundVel=
                        MathHelper.applyRotation2D(MathHelper.normalized(robotPos.subtract(ballPos)), 0.5 * FastMath.PI).scalarMultiply(distBtoR * omega);
                // 敵が目前にいるときは少し押す
                final double minStraightVel = (opp.isPresent() ? 700.0 : 200.0) + ((this.isKeeper) ? 0.0 : 500.0);
                Vector2D straightVel =
                        MathHelper.normalized(ballPos.subtract(robotPos)).scalarMultiply(
                                FastMath.max(minStraightVel,
                                        kickable && !(this.kickManually && this.manualKickFlag.getFirst() ==
                                                EnumKickType.NONE)
                                                ? 2000.0
                                                : FastMath.sqrt(2.0 * toBallAcc * MathHelper.distance2D(roundPos, robotPos))
                                ));
                if (haveBall && MathHelper.distance2D(ballPos, this.world.get().getDribbleStartPos(this.color)) > DRIBBLE_MARGIN) {
                    // オーバードリブル対策
                    straightVel = Vector2D.ZERO;
                }

                command.setTargetVel(straightVel.add(roundVel));
                command.setDribble(DRIBBLE_VALUE);
                break;
            }

            case RunningState.ROUND: {

                if (oppRobot.isPresent() && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < 1000.0 &&
                        MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < distBtoR) {
                    // 相手ロボットが近くにいて、相手ロボットのほうがボールに近いとき
                    command.setTargetTheta(MathHelper.direction(oppRobot.get().getRobot().position(), ballPos));
                } else {
                    command.setTargetTheta(toTargetTheta);
                }

                Vector2D roundVel =
                        MathHelper.getFromPolar(FastMath.sqrt(2 * 3000 * distBtoR), toBallTheta);
                if (distBtoR < 500) {
                    roundVel = MathHelper.getFromPolar(FastMath.sqrt(2 * 1500 * distBtoF), robot.getTheta() + 1.5 * MathHelper.directionFrom(MathHelper.direction(ballPos, faceBallPos), robot.getTheta()));
                }
                command.setTargetVel(ballVel.add(roundVel));

                command.setDribble(DRIBBLE_VALUE);
                break;
            }

        }

        if (!flagRoundToVel) {
            this.roundToVel = false;
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
     * 許容する目標位置とボールとの距離を設定する（キック時）
     * @param margin 距離
     */
    public void setKickMargin(double margin) {
        this.kickMargin = margin;
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

    /**
     * シュート時のキックパワーを設定する。
     * @param pow キックパワー
     */
    public void setShootPow(int pow) {
        this.shootPower = pow;
    }

    /**
     * 許容する目標位置とボールとの距離を設定する(終了判定時)。
     * @param allow 距離
     */
    public void setAllow(double allow) {
        this.allow = allow;
    }

    /**
     * シュートできそうならキックする特殊条件を使用するか。
     * @param isShoot true なら特殊条件を使用する
     */
    public void setIsShoot(boolean isShoot) {
        this.isShoot = isShoot;
    }

    /**
     * クリアできそうならキックする特殊条件を使用するか。
     * @param isClear true なら特殊条件を使用する
     */
    public void setIsClear(boolean isClear) {
        this.isClear = isClear;
    }

    /**
     * ボールを敵に奪われないように立ち回るか。
     * @param protectBall true ならボールを敵から守る
     */
    public void setProtectBall(boolean protectBall) {
        this.protectBall = protectBall;
    }

    /**
     * キーパー用の設定にするか。
     * @param isKeeper true なら特殊条件を使用する
     */
    public void setIsKeeper(boolean isKeeper) {
        this.isKeeper = isKeeper;
    }

    /**
     * 現在の目標位置を返す。
     * @return 現在の目標位置
     */
    public Vector2D target() {
        return this.target;
    }

    /**
     * 現在の状態を返す。
     * @return 現在の状態
     */
    public RunningState state() {
        return this.state;
    }

    /**
     * チップで蹴ったかを返す。
     * @return チップで蹴った場合は true
     */
    public boolean chipped() {
        return this.chipped;
    }

    /**
     * ボールを持っている位置を返す。
     * @return ボールを持っている位置
     */
    public Vector2D haveBallPos() {
        return this.haveBallPos;
    }

    /**
     * ロボットがボールを持っているかを返す。
     * @return ボールを持っている場合は true
     */
    public boolean hasBall() {
        return this.haveBall;
    }

    @Override
    public String getName() {
        return "getBall";
    }

    @Override
    public boolean isFinished() {
        return this.state == RunningState.FINISHED;
    }

    private void kick(final Vector2D robotPosition, final Map<Integer, IntegratedRobot> opponentRobots, Command command) {
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
            command.setKickFlag(EnumKickType.CHIP, autoKickPow.getSecond());
        } else {
            command.setKickFlag(EnumKickType.STRAIGHT, autoKickPow.getFirst());
        }
    }
}