package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMitoma;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.ActionReceive;
import ai_server_cafe.game.action.ActionRobBall;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterPassTarget;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleChaser extends AbstractRole {
    private static final Logger LOGGER = LogManager.getLogger("obstacle-test");

    public RoleChaser(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent()) {
            return list;
        }
        // ロボット半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;

        // 相手ペナルティーエリアからこの距離以内の位置からシュートを打つ
        final double SHOOT_AREA_MARGIN = 2500;
        // シュートを打てるときのゴールからの角度
        final double SHOOT_ANGLE = 0.45 * Math.PI;

        // シュート速度
        final double SHOOT_SPEED = 6000;

        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        Field wf = this.world.get().getField();
        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;
        int chaser = MathHelper.nearestRobotToPosition(MathHelper.robotListFromIds(friendlyRobots, this.roleIds), ballPos).get().getId();
        IntegratedRobot chaserRobot = friendlyRobots.get(chaser);
        Vector2D robotPos = chaserRobot.getRobot().position();

        // 敵chaser
        Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);
        boolean oppHaveBall = oppRobot.isPresent() && MathHelper.distance2D(
                oppRobot.get().getRobot().position().add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, oppRobot.get().getRobot().getTheta())), ballPos) < 50.0;
        boolean oppNearBall = oppRobot.isPresent() && MathHelper.distance2D(
                oppRobot.get().getRobot().position(), ballPos) < 160.0;

        final Vector2D faceBallPos =
                robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, chaserRobot.getRobot().getTheta()));
        // ボールを持っているか
        boolean haveBall = (faceBallPos.subtract(ballPos)).getNorm() < 35.0 ||
                (MathHelper.inferiorAngle(chaserRobot.getRobot().getTheta(),
                        MathHelper.direction(ballPos, robotPos)) < 0.2 &&
                        MathHelper.distance2D(ballPos, robotPos) < TO_FACE_RAD + BALL_RAD);

        List<Integer> otherIds = new ArrayList<>(visibleIds);
        otherIds.remove((Integer) chaser);
        List<IntegratedRobot> waiterRobots = MathHelper.robotListFromIds(friendlyRobots, otherIds);

        // ボールを運ぶ目標を決定
        Optional<Vector2D> shootTarget= Optional.empty();
        Optional<Vector2D> dribbleTarget = Optional.empty();
        Optional<Vector2D> driftTarget = Optional.empty();
        List<Vector2D> shootTargets = StrategyHelper.findShootTarget(this.world.get(), this.color, robotPos);
        List<Vector2D> dribbleTargets = StrategyHelper.findDribbleTarget(this.world.get(), this.color, robotPos);
        List<Vector2D> driftTargets = StrategyHelper.findDriftTarget(this.world.get(), this.color, world.get().getDribbleStartPos(this.color), robotPos);

        // パスターゲットのロックを解除
        UpdaterPassTarget.getInstance().setTargetLocked(false, this.color);

        List<Vector2D> passTargets = UpdaterPassTarget.getInstance().getPassTargets(this.color).get();
        Optional<Vector2D> passTarget = Optional.empty();
        Optional<IntegratedRobot> targetRobot = Optional.empty();
        for (Vector2D target : passTargets) {
            targetRobot = MathHelper.nearestRobotToPosition(waiterRobots, target);
            boolean isClear = ballPos.getX() < 0 && target.getX() > 0;
            if (targetRobot.isPresent()
                    && MathHelper.distancePositionToRobot(target, targetRobot.get().getRobot())
                    < MathHelper.distance2D(target, ballPos) * (isClear ? 0.5 : 0.3)) {
                // ターゲットにレシーバーが近づいてから打つ
                // クリア時は判定をゆるく
                passTarget = Optional.of(target);
                break;
            }
        }

        if (!shootTargets.isEmpty()
                && ((robotPos.getX() > wf.getPenaltyFrontX() - ROBOT_RAD * 8 && Math.abs(robotPos.getY()) < wf.getPenaltyMaxY() + ROBOT_RAD * 8)
                || (MathHelper.inferiorAngle(robotPos.subtract(wf.getFrontGoalCenter()), new Vector2D(-1, 0)) < SHOOT_ANGLE
                && robotPos.getX() > wf.getPenaltyFrontX() - SHOOT_AREA_MARGIN && Math.abs(robotPos.getY()) < wf.getPenaltyMaxY() + SHOOT_AREA_MARGIN))) {
            List<Vector2D> sortedTargets = StrategyHelper.sortShootTarget(this.world.get(), this.color, shootTargets, robotPos, chaserRobot.getRobot().getTheta());
            shootTarget = Optional.of(sortedTargets.getFirst());
            this.visualizerTargets = List.of(sortedTargets.getFirst());
        } else if (!driftTargets.isEmpty()
                && MathHelper.inferiorAngle(robotPos.subtract(wf.getFrontGoalCenter()), new Vector2D(-1, 0)) < SHOOT_ANGLE
                && robotPos.getX() > wf.getPenaltyFrontX() - SHOOT_AREA_MARGIN && Math.abs(robotPos.getY()) < wf.getPenaltyMaxY() + SHOOT_AREA_MARGIN) {
            List<Vector2D> sortedTargets = StrategyHelper.sortDriftTarget(this.world.get(), this.color, driftTargets, robotPos, chaserRobot.getRobot().velocity());
            driftTarget = Optional.of(sortedTargets.getFirst());
            this.visualizerTargets = new ArrayList<>(List.of(driftTarget.get()));
        } else if (passTarget.isPresent()) {
            this.visualizerTargets = List.of(passTarget.get());
        } else if (!dribbleTargets.isEmpty()) {
            List<Vector2D> sortedTargets = StrategyHelper.sortDribbleTarget(this.world.get(), this.color, dribbleTargets, robotPos, chaserRobot.getRobot().getTheta());
            dribbleTarget = Optional.of(sortedTargets.getFirst());
            this.visualizerTargets = new ArrayList<>(List.of(dribbleTarget.get()));
        } else {
            this.visualizerTargets = new ArrayList<>();
        }

        // レシーブするか？
        boolean receiveFlag =
                (this.receiveMap.get(chaser).isDirectWaiting(now) || MathHelper.distance2D(ballPos, robotPos) > ROBOT_RAD * 2)
                        && ballVel.getNorm() > 700
                && !(MathHelper.inferiorAngle(ballVel, ballPos.subtract(robotPos)) < 0.3 * Math.PI
                && ((shootTarget.isPresent() && MathHelper.inferiorAngle(ballVel, shootTarget.get().subtract(ballPos)) < 0.3 * Math.PI)
                || (passTarget.isPresent() && MathHelper.inferiorAngle(ballVel, passTarget.get().subtract(ballPos)) < 0.3 * Math.PI)
                || (dribbleTarget.isPresent() && MathHelper.inferiorAngle(ballVel, dribbleTarget.get().subtract(ballPos)) < 0.3 * Math.PI)));

        final Vector2D frontGoalLeft = new Vector2D(wf.getMaxX(), wf.getGoalMaxY());
        final Vector2D frontGoalRight = new Vector2D(wf.getMaxX(), wf.getGoalMinY());
        final double toLeft = MathHelper.direction(frontGoalLeft.subtract(ballPos));
        final double toRight = MathHelper.direction(frontGoalRight.subtract(ballPos));
        // ボールを避けるか？（味方がシュートを打ったとき）
        boolean isShootGoing = ballVel.getNorm() > 2000
                && MathHelper.distance2D(ballPos, wf.getFrontGoalCenter()) < 4000
                && MathHelper.isRightOf(MathHelper.direction(ballVel), toLeft)
                && MathHelper.isLeftOf(MathHelper.direction(ballVel), toRight);

        // アクションの設定
        ActionKick chaserKick = this.kickMap.get(chaser);
        chaserKick.setDriftTarget(Optional.empty());
        double holdTime = 30;
        Optional<Vector2D> optPoint = ObstacleSegment.getPerpendicularIntersectionPoint(ballPos, ballPos.add(MathHelper.normalized(ballVel).scalarMultiply(6000)), robotPos);
        if (isShootGoing && optPoint.isPresent()) {
            // 味方のシュートを避ける
            ActionMove chaserMove = this.moveMap.get(chaser);
            Vector2D intersectionPoint = optPoint.get();
            chaserMove.setPos(intersectionPoint.add(MathHelper.normalized(robotPos.subtract(intersectionPoint))
                    .scalarMultiply(Math.max(robotPos.subtract(intersectionPoint).getNorm(), ROBOT_RAD * 2))));
            chaserMove.setAngle(MathHelper.direction(wf.getFrontGoalCenter().subtract(robotPos)));
            list.add(chaserMove);
        } else if (oppHaveBall || oppNearBall && !haveBall) {
            // 敵からボールを奪う
            ActionRobBall chaserRobBall = this.robBallMap.get(chaser);
            list.add(chaserRobBall);
        } else if (ballVel.getNorm() < 700
                && MathHelper.distance2D(robotPos, ballPos) > ROBOT_RAD * 2
                && oppRobot.isPresent()
                && MathHelper.distance2D(robotPos, ballPos) + ROBOT_RAD * 10
                > MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot())) {
            // ボール速度が遅くて
            // 敵のほうがボールに近いとき
            this.protectBallMap.get(chaser).setTarget(wf.getFrontGoalCenter());
            list.add(this.protectBallMap.get(chaser));
        } else if (receiveFlag) {
            // レシーブ
            ActionReceive chaserReceive = this.receiveMap.get(chaser);
            if (shootTarget.isPresent()) {
                // シュート
                chaserReceive.setKickTarget(shootTarget); // ダイレクトキックできるならする
                chaserReceive.setKickFlag(new Pair<>(EnumKickType.STRAIGHT, KickConverter.toPower(chaser, new Pair<>(EnumKickType.STRAIGHT, SHOOT_SPEED))));
            } else if (passTarget.isPresent()) {
                // パス
                chaserReceive.setKickTarget(passTarget); // ダイレクトキックできるならする
                chaserReceive.setKickFlag(
                        new Pair<>(EnumKickType.STRAIGHT, KickConverter.toPower(chaser, new Pair<>(EnumKickType.STRAIGHT, KickConverter.passSpeed(robotPos, passTarget.get())))));
            } else {
                chaserReceive.setKickTarget(Optional.empty());
            }
            list.add(chaserReceive);
        } else if (shootTarget.isPresent()) {
            // シュート
            chaserKick.setTarget(shootTarget.get());
            chaserKick.kickManually(new Pair<>(EnumKickType.STRAIGHT, KickConverter.toPower(chaser, new Pair<>(EnumKickType.STRAIGHT, SHOOT_SPEED))));
            if (StrategyHelper.isLineInterrupted(robotPos, ROBOT_RAD * 2, shootTarget.get(), oppositeRobots.values().stream().toList())) {
                holdTime = 10;
            } else {
                holdTime = 50;
            }
            list.add(chaserKick);
        } else if (driftTarget.isPresent()) {
            // ドリフト（ゴール前でボールを持ったまま動く）
            chaserKick.setTarget(new Vector2D(wf.getMaxX(), Math.copySign(wf.getGoalMaxY() * 0.8, robotPos.getY())));
            chaserKick.setDriftTarget(driftTarget);
            chaserKick.kickManually(new Pair<>(EnumKickType.STRAIGHT, KickConverter.toPower(chaser, new Pair<>(EnumKickType.STRAIGHT, SHOOT_SPEED))));
            list.add(chaserKick);
        } else if (passTarget.isPresent()) {
            // パス
            chaserKick.setTarget(passTarget.get());
            chaserKick.kickAutomatically(
                    KickConverter.toPower(chaser, new Pair<>(EnumKickType.STRAIGHT, KickConverter.passSpeed(robotPos, passTarget.get()))),
                    KickConverter.toPower(chaser, new Pair<>(EnumKickType.CHIP, KickConverter.chipSpeed(robotPos, passTarget.get()))));
            list.add(chaserKick);
        } else if (dribbleTarget.isPresent()) {
            // ドリブル
            ActionMitoma chaserMitoma = this.mitomaMap.get(chaser);
            chaserMitoma.setTarget(dribbleTarget.get());
            list.add(chaserMitoma);
        } else {
            this.protectBallMap.get(chaser).setTarget(wf.getFrontGoalCenter());
            list.add(this.protectBallMap.get(chaser));
        }

        chaserKick.setIsClear(false);
        if (oppRobot.isPresent() && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < ROBOT_RAD * 5) {
            // 近くに敵がいるときはすぐにける
            chaserKick.setHoldTime(0);

            if (ballPos.getX() < wf.getPenaltyBackX() + 3000) {
                // クリア（適当にける）
                chaserKick.setIsClear(true);
            }
        }
        chaserKick.setHoldTime(holdTime);

        // 障害物設定
        List<AbstractAction> returnList = new ArrayList<>();
        for (AbstractAction action : list) {
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, action.getId(), CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot());
            if (action.getId() == chaser && oppRobot.isPresent() && MathHelper.distance2D(robotPos, ballPos) < MathHelper.distance2D(oppRobot.get().getRobot().position(), robotPos)) {
                returnList.add(new WithPlanner<>(action, action.getId(), this.color, CommonObstacles.getAllExcluded2Robots(this.world.get(), this.color, action.getId(), oppRobot.get().getId(), CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot())));
            } else {
                returnList.add(new WithPlanner<>(action, action.getId(), this.color, obstacles));
            }
        }
        return returnList;
    }

    @Override
    public String getName() {
        return "role_chaser";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}