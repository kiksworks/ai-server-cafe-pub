package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActionReceive extends AbstractAction {

    private boolean finished;
    private boolean passcutFlag;
    private Optional<Vector2D> kickTarget;
    private Optional<Vector2D> fixedTarget;
    // 更新された時刻
    private double lastUpdatedTime;
    private Pair<EnumKickType, Integer> manualKickFlag;
    // ワンツーしようとしているか
    private long directWaiting;

    public ActionReceive(int id, TeamColor color) {
        super(id, color);
        this.finished = false;
        this.passcutFlag = true;
        this.kickTarget = Optional.empty();
        this.fixedTarget = Optional.empty();
        this.lastUpdatedTime = TimeHelper.now();
        this.manualKickFlag = new Pair<>(EnumKickType.STRAIGHT, 200);
        this.directWaiting = 0L;
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;

        final Field wf = this.world.get().getField();

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final IntegratedRobot robot = this.world.get().getFriendlyRobotMap(this.color).get(id);
        final Vector2D robotPos = robot.getRobot().position();
        final Vector2D faceBallPos =
                robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, robot.getRobot().getTheta()));
        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        if (ballVel.getNorm() < 10) {
            return command;
        }

        boolean haveBall = (faceBallPos.subtract(ballPos)).getNorm() < 50.0 ||
                (MathHelper.inferiorAngle(robot.getRobot().getTheta(),
                        MathHelper.direction(ballPos, robotPos)) < 0.4 &&
                        MathHelper.distance2D(ballPos, robotPos) < TO_FACE_RAD + BALL_RAD);

        // ボール減速度
        final double ballBrake = ConfigManager.getInstance().getConfig().ballBrake;
        // ボールが止まる位置
        Vector2D ballStop = ballPos.add(MathHelper.normalized(ballVel).scalarMultiply(ballVel.getNorm() * ballVel.getNorm() / (2 * ballBrake)));

        // ボール直線上へ行く
        Vector2D movePos = ballPos.add(MathHelper.normalized(ballVel).scalarMultiply(MathHelper.distance2D(robotPos, ballPos)));

        if (MathHelper.distance2D(ballPos, ballStop) < MathHelper.distance2D(ballPos, movePos)) {
            // ボールが止まる位置まで行く
            movePos = ballStop.add(MathHelper.normalized(ballVel).scalarMultiply(ROBOT_RAD));
        }

        if (MathHelper.inferiorAngle(ballVel, robotPos.subtract(ballPos)) > 0.3 * FastMath.PI) {
            // 回り込み
            movePos = ballStop.add(MathHelper.applyRotation2D(MathHelper.normalized(ballVel).scalarMultiply(ROBOT_RAD * 3),
                    FastMath.copySign(0.5 * FastMath.PI, MathHelper.directionFrom(robotPos.subtract(ballPos), ballVel))));
        }

        if (this.passcutFlag) {
            // 敵chaserを求める
            final Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
            Map<IntegratedRobot, Double> chaserScoreMap = new HashMap<>();
            for (IntegratedRobot opp : oppositeRobots.values()) {
                double score = 3000 * FastMath.exp(-MathHelper.distance2D(opp.getRobot().position(), ballPos) / 1500.0);
                if (ballVel.getNorm() > 1500) {
                    score = FastMath.exp(-MathHelper.inferiorAngle(opp.getRobot().position().subtract(ballPos), ballVel)) +
                            FastMath.exp(-MathHelper.distance2D(opp.getRobot().position(), ballPos) / 1500.0);
                }
                chaserScoreMap.put(opp, score);
            }
            Optional<IntegratedRobot> oppRobot = MathHelper.robotWithMaxScore(chaserScoreMap);

            if (this.passcutFlag && oppRobot.isPresent() &&
                    MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < MathHelper.distance2D(robotPos, ballPos) + ROBOT_RAD * 3 &&
                    MathHelper.inferiorAngle(MathHelper.direction(ballVel), MathHelper.direction(oppRobot.get().getRobot().position(), ballPos)) < 0.1 * FastMath.PI) {
                // パスカット
                movePos = MathHelper.normalized(ballVel).scalarMultiply(MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) - ROBOT_RAD * 3).add(ballPos);
            }
        }

        command.setTargetTheta(MathHelper.direction(ballPos, movePos));
        command.setDribble(12);

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

        if ( MathHelper.isCollidedWithBox(
                new Vector2D(wf.getMinX(), wf.getPenaltyMinY()), new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMaxY()), movePos, ROBOT_RAD) || MathHelper.isCollidedWithBox(
                new Vector2D(wf.getPenaltyFrontX(), wf.getPenaltyMinY()), new Vector2D(wf.getMaxX(), wf.getPenaltyMaxY()), movePos, ROBOT_RAD)) {
            // ペナルティーエリア内のとき
            List<Vector2D> linePosisions = new ArrayList<>();
            // ボール速度上に複数の点を取る
            for (int i = 0; i <= 5000; i+=100) {
                Vector2D tmpPos =
                        ballPos.add(MathHelper.normalized(movePos.subtract(ballPos)).scalarMultiply(i));
                if (!MathHelper.isCollidedWithBox(
                        new Vector2D(wf.getMinX(), wf.getPenaltyMinY()), new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMaxY()), tmpPos, ROBOT_RAD) && !MathHelper.isCollidedWithBox(
                        new Vector2D(wf.getPenaltyFrontX(), wf.getPenaltyMinY()), new Vector2D(wf.getMaxX(), wf.getPenaltyMaxY()), tmpPos, ROBOT_RAD) && MathHelper.isCollidedWithBox(
                        new Vector2D(wf.getMinX(), wf.getMinY()), new Vector2D(wf.getMaxX(), wf.getMaxY()), tmpPos, 0)) {
                    linePosisions.add(tmpPos);
                }
            }
            if (!linePosisions.isEmpty()) {
                movePos = MathHelper.nearestPositionToRobot(linePosisions, robot).get();
            }
        }

        double now = TimeHelper.now();
        if (now - this.lastUpdatedTime > 0.5 || MathHelper.distance2D(robotPos, ballPos) > 1000 || this.fixedTarget.isEmpty()) {
            this.fixedTarget = this.kickTarget;
        }
        this.lastUpdatedTime = now;

        if (this.fixedTarget.isPresent()
                && !MathHelper.toList(ConfigManager.getInstance().getConfig().newDribblerIds).contains(robot.getId())) {
            // 新型ドリブラーではワンツーしない
            final double kickSpeed = 12000;
            Vector2D target = this.fixedTarget.get().subtract(ballVel.scalarMultiply(MathHelper.distance2D(robotPos, fixedTarget.get()) / kickSpeed));
            if (MathHelper.inferiorAngle(this.fixedTarget.get().subtract(robotPos), ballPos.subtract(robotPos)) < 0.4 * FastMath.PI) {
                // ワンツー
                this.directWaiting = this.now;
                Vector2D ballVelRotated = MathHelper.normalized(MathHelper.applyRotation2D(ballVel, 0.5 * FastMath.PI));
                movePos = movePos.add(ballVelRotated.scalarMultiply(
                        ballVelRotated.dotProduct(MathHelper.normalized(movePos.subtract(target)).scalarMultiply(TO_FACE_RAD + BALL_RAD))));
                command.setTargetTheta(MathHelper.direction(target, movePos));
                command.setDribble(12);
                if (MathHelper.inferiorAngle(MathHelper.direction(target, movePos), robot.getRobot().getTheta()) < 0.1 * FastMath.PI) {
                    command.setKickFlag(this.manualKickFlag);
                }
            }
        }

        command.setTargetPosition(movePos);

        this.finished = MathHelper.distance2D(ballPos, robotPos) < 100;

        return command;
    }

    @Override
    public String getName() {
        return "receive";
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    /**
     * ダイレクトでキックするときの目標位置を設定
     * @param kickTarget キック目標
     */
    public void setKickTarget(Optional<Vector2D> kickTarget) {
        this.kickTarget = kickTarget;
    }

    /**
     * キックフラグを設定
     */
    public void setKickFlag(Pair<EnumKickType, Integer> kickFlag) {
        this.manualKickFlag = kickFlag;
    }

    /**
     * パスカットするかを設定
     * @param flag
     */
    public void setPasscutFlag(boolean flag) {
        this.passcutFlag = flag;
    }

    /**
     * ワンツーしようとしているか
     * @param now
     * @return
     */
    public boolean isDirectWaiting(long now) {
        return now - this.directWaiting <= 1L;
    }
}