package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleSupport extends AbstractRole {
    private static final Logger LOGGER = LogManager.getLogger("obstacle-test");

    public RoleSupport(TeamColor color, int[] ids) {
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

        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        Field wf = this.world.get().getField();
        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;
        int id = MathHelper.nearestRobotToPosition(MathHelper.robotListFromIds(friendlyRobots, this.roleIds), ballPos).get().getId();
        IntegratedRobot robot = friendlyRobots.get(id);
        Vector2D robotPos = robot.getRobot().position();

        // 敵chaser
        Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);
        boolean oppHaveBall = oppRobot.isPresent() && MathHelper.distance2D(
                oppRobot.get().getRobot().position().add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, oppRobot.get().getRobot().getTheta())), ballPos) < 80.0;

        // 位置決定
        Vector2D movePos;
        if (oppHaveBall) {
            movePos = ballPos.add(MathHelper.normalized(wf.getBackGoalCenter().subtract(ballPos)).scalarMultiply(ROBOT_RAD * 3));
        } else if (oppRobot.isPresent() && MathHelper.distancePositionToRobot(robotPos, oppRobot.get().getRobot()) > ROBOT_RAD * 7
                && !MathHelper.isCollidedWithBox(new Vector2D(wf.getPenaltyFrontX(), wf.getPenaltyMinY()), new Vector2D(wf.getMaxX(), wf.getPenaltyMaxY()), oppRobot.get().getRobot().position(), ROBOT_RAD)) {
            Vector2D oppPos = oppRobot.get().getRobot().position();
            movePos = ballPos.add(MathHelper.normalized(oppPos.subtract(ballPos)).scalarMultiply(ROBOT_RAD * 5));
        } else {
            movePos = ballPos.add(MathHelper.normalized(wf.getBackGoalCenter().subtract(ballPos)).scalarMultiply(ROBOT_RAD * 7));
        }

        // アクションの設定
        ActionMove actionMove = this.moveMap.get(id);
        actionMove.setPos(movePos);
        actionMove.setAngle(MathHelper.direction(ballPos.subtract(robotPos)));
        list.add(actionMove);

        // 障害物設定
        List<AbstractAction> returnList = new ArrayList<>();
        for (AbstractAction action : list) {
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, action.getId(), CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot());
            //List<AbstractObstacle2> obstacles1 = CommonObstacles2.getAllExcludedARobot(this.world.get(), this.color, action.getId(), 100.0);
            //double now = TimeHelper.now();
            //PlannerHelper.getIOs(obstacles);
            //double now1 = TimeHelper.now();
            //double d1 = now1 - now;
            //PlannerHelper.getIO2s(obstacles1);
            //double d2 = TimeHelper.now() - now1;
            //LOGGER.info("{}, {}", d1, d2);
            returnList.add(new WithPlanner<>(action, action.getId(), this.color, obstacles));
        }
        return returnList;
    }

    @Override
    public String getName() {
        return "role_support";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}