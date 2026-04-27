package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleKeeper extends AbstractRole {
    private Optional<Vector2D> ballPlacePos;
    private boolean avoidBall;

    public RoleKeeper(TeamColor color, int[] ids) {
        super(color, ids);
        this.ballPlacePos = Optional.empty();
        this.avoidBall = false;
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent() || roleIds.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        Field wf = this.world.get().getField();

        int keeper = roleIds.getFirst();
        if (!visibleIds.contains(keeper)) {
            return list;
        }
        // ロボット半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;

        IntegratedRobot keeperRobot = friendlyRobots.get(keeper);

        Vector2D robotPos = keeperRobot.getRobot().position();

        final Vector2D faceBallPos =
                robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, keeperRobot.getRobot().getTheta()));

        // キーパー以外の味方ロボット
        List<Integer> otherIds = new ArrayList<>(visibleIds);
        otherIds.remove((Integer) keeper);
        List<IntegratedRobot> waiterRobots = MathHelper.robotListFromIds(friendlyRobots, otherIds);

        if (!this.avoidBall && this.ballPlacePos.isEmpty()
                && MathHelper.isCollidedWithBox(new Vector2D(wf.getMinX(), wf.getPenaltyMinY()), new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMaxY()), ballPos, 0)
                && ballVel.getNorm() < 1000) {
            // ボールがペナルティーエリア内で留まっているとき
            // キックする
            List<Vector2D> waiterPositions = new ArrayList<>();
            for (IntegratedRobot robot : waiterRobots) {
                // パス目標候補
                Vector2D position = robot.getRobot().position();
                Vector2D velocity = robot.getRobot().velocity();
                if (velocity.getNorm() > 500) {
                    position = position.add(MathHelper.normalized(velocity).scalarMultiply(Math.pow(velocity.getNorm(), 2) / (2 * 5000)));
                }
                waiterPositions.add(position);
            }

            ActionKick kick = this.kickMap.get(keeper);
            kick.setIsKeeper(true);
            List<Vector2D> passTargets = StrategyHelper.findPassTarget(this.world.get(), this.color, waiterPositions, robotPos);
            if (!passTargets.isEmpty()) {
                List<Vector2D> sortedTargets = StrategyHelper.sortPassTarget(this.world.get(), this.color, passTargets, robotPos, keeperRobot.getRobot().getTheta());
                Vector2D passTarget = sortedTargets.getFirst();
                kick.setTarget(sortedTargets.getFirst());
                kick.kickAutomatically(
                        KickConverter.toPower(keeper, new Pair<>(EnumKickType.STRAIGHT, KickConverter.passSpeed(robotPos, passTarget))),
                        KickConverter.toPower(keeper, new Pair<>(EnumKickType.CHIP, KickConverter.chipSpeed(robotPos, passTarget))));
                this.visualizerTargets = sortedTargets;
            } else {
                kick.setTarget(wf.getFrontGoalCenter());
                kick.kickManually(new Pair<>(EnumKickType.CHIP, 255));
                this.visualizerTargets = Arrays.asList(wf.getFrontGoalCenter());
            }

            list.add(kick);

        } else if (!this.avoidBall && this.ballPlacePos.isEmpty()
                && ballVel.dotProduct(MathHelper.normalized(wf.getBackGoalCenter().subtract(ballPos))) < 1000
                && MathHelper.isCollidedWithBox(new Vector2D(wf.getMinX(), wf.getPenaltyMinY()), new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMaxY()), ballPos, 2000)
                && waiterRobots.stream().noneMatch(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
            @Override
            public Boolean function(IntegratedRobot robot) {
                final Vector2D robotPos = robot.getRobot().position();
                final Vector2D goal = wf.getBackGoalCenter();
                return MathHelper.distance2D(goal, robotPos) < MathHelper.distance2D(goal, ballPos)
                        && MathHelper.inferiorAngle(MathHelper.direction(ballPos.subtract(goal)),
                        MathHelper.direction(robotPos.subtract(goal))) < 0.2 * Math.PI;
            }
        }))) {
            // キーパー以外に守りがいないとき
            // ボールを奪いにいく
            ActionKick kick = this.kickMap.get(keeper);
            if (world.get().getHaveBall(this.color.getInvert())) {
                // 敵がボールを持っているとき
                kick.setTarget(ballPos.add(ballPos).subtract(wf.getBackGoalCenter()));
                kick.kickManually(new Pair<>(EnumKickType.NONE, 0));
            } else {
                kick.setTarget(wf.getFrontGoalCenter());
                kick.kickManually(new Pair<>(EnumKickType.CHIP, 255));
            }
            list.add(kick);
            this.visualizerTargets = List.of(ballPos);
        } else {
            list.add(this.goalKeepMap.get(keeper));
            this.visualizerTargets = new ArrayList<>();
        }

        List<AbstractAction> returnList = new ArrayList<>();
        List<AbstractObstacle> obstacles = new ArrayList<>();
        if (this.ballPlacePos.isPresent()) {
            obstacles.add(new ObstacleSegment(ballPos, this.ballPlacePos.get(), 650));
        }
        if (avoidBall) {
            obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける
        }
        if (robotPos.getX() > wf.getPenaltyBackX() || Math.abs(robotPos.getY()) > wf.getPenaltyMaxY()) {
            // 味方ロボットを避ける
            obstacles.addAll(CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color, new int[] {keeper}, keeperRobot.getRobot(), ROBOT_RAD));
        }
        if (Math.abs(robotPos.getY()) > wf.getGoalMaxY() || (Math.abs(ballPos.getY()) > wf.getGoalMaxY() && ballVel.getNorm() < 200.0)) {
            obstacles.addAll(CommonObstacles.getFriendlyGoal(this.world.get().getField(), ROBOT_RAD));
        }
        for (AbstractAction action : list) {
            returnList.add(new WithPlanner<>(action, action.getId(), this.color, obstacles));
        }

        return returnList;
    }

    @Override
    public String getName() {
        return "role_keeper";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setBallPlacePos(Optional<Vector2D> ballPlacePos) {
        this.ballPlacePos = ballPlacePos;
    }

    public void setAvoidBall(boolean avoidBall) {
        this.avoidBall = avoidBall;
    }
}