package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.Map;

public class ActionBallPlace extends AbstractAction {

    /**
     * 動作の状態の表現
     */
    public enum RunningState {
        MOVE,    ///< ボール前まで移動
        ROUND,   ///< 回り込み
        HOLD,    ///< ボールを持つ
        PLACE,   ///< ボールを指定位置まで運ぶ
        WAIT,    ///< ボールの回転を止めるためドリブルバーを止めて停止
        LEAVE,   ///< ボールから離れる
        FINISHED ///< 動作終了(停止)
    }

    /**
     * 動作のモードの表現
     */
    public enum PlaceMode{
        PULL, ///< ボールを引く
        PUSH, ///< ボールを押す
        PASS  ///< ボールを蹴る
    }

    // 状態
    private RunningState state;
    // モード
    private PlaceMode mode;
    // 一時的な目標
    private Vector2D target;
    // 目標
    private Vector2D abpTarget;
    // 保持直前のボール位置
    Vector2D firstBallPos;
    // キックの種類・パワー
    private Pair<EnumKickType, Integer> kickFlag;
    // ボールを持っているか
    private boolean haveBall;
    // 前フレームの角速度
    private double prevOmega;
    // 許容誤差
    private double margin;
    // 動作終了の状態か
    private boolean finished;
    // stateが変わった時刻
    double stateChangedTime;

    boolean forcePull;

    public ActionBallPlace(int id, TeamColor color, Vector2D target) {
        super(id, color);
        this.state = RunningState.MOVE;
        this.finished = false;
        this.stateChangedTime = TimeHelper.now();
        this.target = target;
        this.abpTarget = target;
        this.firstBallPos = target;
        this.kickFlag = new Pair<>(EnumKickType.NONE, 0);
        this.haveBall = false;
        this.prevOmega = 0.0;
        this.margin = 90.0;
        this.forcePull = false;
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
        // 台形制御の減速度
        final double acc = 1200.0;

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
        if (this.haveBall && this.world.get().getBall().isLost()) ballPos = faceBallPos;
        this.haveBall = (MathHelper.distance2D(faceBallPos, ballPos) < 20.0 ||
                (MathHelper.inferiorAngle(robot.getTheta(),
                        MathHelper.direction(ballPos, robotPos)) < 0.2 &&
                        MathHelper.distance2D(ballPos, robotPos) < TO_FACE_RAD + BALL_RAD));
        if (this.haveBall) ballPos = faceBallPos;
        // 許容誤差(ルール上は100[mm]以内)
        final double allowFromTarget = this.margin;
        // 配置後にロボットがボールから離れなければならない距離
        final double finishMargin = 500.0;
        // dribble
        final int dribbleValue = 8;
        // ロボットとボールの距離
        final double distBtoR = MathHelper.distance2D(ballPos, robotPos);
        final double distBtoF = MathHelper.distance2D(ballPos, faceBallPos);
        // ボールと最終目標の距離
        final double distBtoAbpTarget = MathHelper.distance2D(ballPos, abpTarget);

        // ボールがフィールド外にあれば目標・モード変更
        {
            // ボールが外に出ているか
            final boolean outX = FastMath.abs(ballPos.getX()) > wf.getMaxX() - 100.0;
            final boolean outY = FastMath.abs(ballPos.getY()) > wf.getMaxY() - 100.0;
            final boolean nearGoal = FastMath.abs(ballPos.getY()) < wf.getGoalWidth() / 2 + 100.0
                    && FastMath.abs(ballPos.getY()) > wf.getGoalWidth() / 2 - 100.0;
            // ボールが外に出ている
            if (outX || outY) {
                if (nearGoal) {
                    this.target = new Vector2D(ballPos.getX(), FastMath.copySign(wf.getGoalWidth() / 2 +
                            (FastMath.abs(ballPos.getY()) > wf.getGoalWidth() / 2 ? 500 : -500), ballPos.getY()));
                } else {
                    this.target = new Vector2D(
                            outX ? FastMath.copySign(wf.getMaxX() - 500.0, ballPos.getX()) : ballPos.getX(),
                            outY ? FastMath.copySign(wf.getMaxY() - 500.0, ballPos.getY()) : ballPos.getY());
                }
                this.mode = PlaceMode.PULL;
            } else if (this.forcePull) {
                this.target = this.abpTarget;
                this.mode = PlaceMode.PULL;
            } else {
                this.target = this.abpTarget;
                this.mode = PlaceMode.PUSH;
            }
            if (this.mode == PlaceMode.PUSH && this.kickFlag.getFirst() != EnumKickType.NONE)
                this.mode = PlaceMode.PASS;
        }

        // 目標角度
        final Vector2D basePos = robotPos;
        final double theta = this.mode == PlaceMode.PUSH || this.mode == PlaceMode.PASS
                ? MathHelper.direction(this.target, basePos)
                : MathHelper.direction(ballPos, this.target);

        this.finished = distBtoAbpTarget < allowFromTarget && distBtoR > finishMargin + ROBOT_RAD;

        if (this.finished) this.state = RunningState.FINISHED;

        final RunningState prevState = this.state;

        switch (this.state) {
            // 終了
            case RunningState.FINISHED: {
                if (distBtoAbpTarget > allowFromTarget) this.state = RunningState.MOVE;
                if (distBtoR < finishMargin + ROBOT_RAD) this.state = RunningState.LEAVE;
                this.finished = true;
                command.setTargetVel(Vector2D.ZERO);
            } break;

            // ボールから離れる
            case RunningState.LEAVE: {
                if (MathHelper.distance2D(robotPos, this.abpTarget) > finishMargin + ROBOT_RAD + 100.0)
                    this.state = RunningState.FINISHED;
                if (!isLost && MathHelper.distance2D(this.abpTarget, ballPos) > allowFromTarget)
                    this.state = RunningState.MOVE;
                command.setTargetPosition(
                        MathHelper.normalized(robotPos.subtract(faceBallPos)).scalarMultiply(finishMargin + ROBOT_RAD + 150.0).add(this.abpTarget));
            } break;

            // 待機
            case RunningState.WAIT: {
                final double now = TimeHelper.now();
                if (now - this.stateChangedTime <= (dribbleValue + 3) * 0.1) {
                    command.setDribble(
                            FastMath.max(0, dribbleValue - (int)((now - this.stateChangedTime) / 0.1)));
                } else {
                    this.state = RunningState.LEAVE;
                }
                if (!isLost && MathHelper.distance2D(this.abpTarget, ballPos) > allowFromTarget)
                    this.state = RunningState.MOVE;
                command.setTargetVel(Vector2D.ZERO);
            } break;

            // 配置
            case RunningState.PLACE: {
                // ロボットの位置で配置終了を判定
                if (MathHelper.distance2D(faceBallPos, this.target) < FastMath.max(allowFromTarget - 50.0, 0.0)) {
                    this.state = RunningState.WAIT;
                }
                if (MathHelper.distance2D(faceBallPos, ballPos) > 150.0) {
                    // ボールが離れていたらボール前まで移動する処理に移行
                    this.state = RunningState.MOVE;
                }
                {
                    final Vector2D tmp = robotPos.add(
                            MathHelper.getFromPolar(robot.getTheta(), MathHelper.distance2D(this.abpTarget, robotPos))
                    );
                    if (MathHelper.distance2D(tmp, this.target) < FastMath.max(allowFromTarget - 50.0, 0.0))
                        command.setKickFlag(this.kickFlag);
                }

                final double toThetaAlpha = 3.0;
                final double alpha = 3.0;
                final double cycle = 1.0 / 60.0;
                final double dtheta = MathHelper.wrapPI(theta - robot.getTheta());
                double omega =
                        FastMath.copySign(FastMath.min(6.0, FastMath.sqrt(2.0 * toThetaAlpha * FastMath.abs(dtheta))), dtheta);
                omega = MathHelper.clamp(omega, this.prevOmega - alpha * cycle, this.prevOmega + alpha * cycle);
                command.setTargetOmega(omega);
                this.prevOmega = omega;

                Vector2D straightVel = MathHelper.inferiorAngle(theta, robot.getTheta()) > 0.2 * FastMath.PI ? Vector2D.ZERO :
                        MathHelper.normalized(this.target.subtract(faceBallPos)).scalarMultiply(
                                MathHelper.clamp(3.0 * MathHelper.distance2D(this.target, faceBallPos), 300.0, mode == PlaceMode.PULL ? 400 : 800.0));
                command.setTargetVel(straightVel);
                command.setDribble(dribbleValue);
                this.firstBallPos = ballPos;
            } break;

            // ボールを持つ
            case RunningState.HOLD: {
                final double now = TimeHelper.now();
                if ((now - this.stateChangedTime > 0.5 && this.haveBall) || now - this.stateChangedTime > 2) {
                    this.state = RunningState.PLACE;
                }
                if (100.0 < MathHelper.distance2D(ballPos, faceBallPos)) this.state = RunningState.ROUND;

                final Vector2D vel = MathHelper.normalized(ballPos.subtract(robotPos)).scalarMultiply(500.0);
                command.setDribble(dribbleValue);
                command.setTargetVel(vel);
                command.setTargetTheta(theta);
            } break;

            case RunningState.ROUND: {
                this.firstBallPos = ballPos;
                if (MathHelper.distance2D(ballPos, faceBallPos) < 10.0) this.state = RunningState.HOLD;
                if (600.0 < distBtoR) this.state = RunningState.MOVE;

                Vector2D roundVel = MathHelper.getFromPolar(FastMath.sqrt(2 * 2000.0 * distBtoR), MathHelper.direction(ballPos, robotPos));
                if (distBtoR < 1500) {
                    roundVel = MathHelper.getFromPolar(FastMath.sqrt(2 * 1500 * distBtoF), robot.getTheta() + 1.5 * MathHelper.directionFrom(MathHelper.direction(ballPos, faceBallPos), robot.getTheta()));
                }

                command.setTargetVel(roundVel);
                command.setTargetTheta(theta);
                command.setDribble(dribbleValue);
            } break;

            // 移動
            default: {
                if (distBtoR < 500.0) this.state = RunningState.ROUND;
                Vector2D pos = ballPos.subtract(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, theta));

                final Vector2D vel =
                        MathHelper.normalized(pos.subtract(robotPos)).scalarMultiply(FastMath.sqrt(2.0 * acc * MathHelper.distance2D(pos, robotPos))).add(ballVel);
                command.setTargetVel(vel);
                command.setTargetTheta(theta);
                command.setDribble(dribbleValue);
            }
        }

        if (prevState != this.state) this.stateChangedTime = TimeHelper.now();

        return command;
    }

    /**
     * 目標位置を設定する
     * @param x 目標位置のx座標
     * @param y 目標位置のy座標
     */
    public void setAbpTarget(double x, double y) {
        this.abpTarget = new Vector2D(x, y);
    }

    /**
     * 目標位置を設定する
     * @param target 目標位置の座標
     */
    public void setAbpTarget(Vector2D target) {
        this.abpTarget = target;
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
     * ロボットがボールを持っているかを返す。
     * @return ボールを持っている場合は true
     */
    public boolean hasBall() {
        return this.haveBall;
    }

    @Override
    public String getName() {
        return "ballPlace";
    }

    @Override
    public boolean isFinished() {
        return this.state == RunningState.FINISHED;
    }

}