package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleKickoffWaiter extends AbstractRole {

    private boolean avoidBall;

    public RoleKickoffWaiter(TeamColor color, int[] ids) {
        super(color, ids);
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

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        final Vector2D ballPos = this.world.get().getBall().position();

        final Field wf = this.world.get().getField();

        List<IntegratedRobot> roleRobots = MathHelper.robotListFromIds(friendlyRobots, this.roleIds);

        List<Vector2D> waitPositions = new ArrayList<Vector2D>(Arrays.asList(
                new Vector2D(-2.0 * ROBOT_RAD, 1000),
                new Vector2D(-2.0 * ROBOT_RAD, -1000),
                new Vector2D(wf.getMinX() / 2, wf.getPenaltyMaxY() + 500),
                new Vector2D(wf.getMinX() / 2, wf.getPenaltyMinY() - 500),
                new Vector2D(wf.getMinX() / 2, wf.getPenaltyMaxY() + 500),
                new Vector2D(wf.getMinX() / 2, wf.getPenaltyMinY() - 500)
                ));

        List<IntegratedRobot> tmpRobots = new ArrayList<>(roleRobots);

        Map<Integer, Vector2D> targetPositions = new HashMap<Integer, Vector2D>();

        for (Vector2D position : waitPositions) {
            if (tmpRobots.isEmpty()) {
                break;
            }
            IntegratedRobot nearest = MathHelper.nearestRobotToPosition(tmpRobots, position).get();
            targetPositions.put(nearest.getId(), position);
            tmpRobots.remove(nearest);
        }

        for (int id : roleIds) {
            this.moveMap.get(id).setPos(targetPositions.get(id));
            this.moveMap.get(id).setAngle(MathHelper.direction(ballPos, friendlyRobots.get(id).getRobot().position()));
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
            if (avoidBall) {
                obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける
            }
            if (this.moveMap.get(id).isFinished()) {
                list.add(this.haltMap.get(id));
            } else {
                list.add(new WithPlanner<>(this.moveMap.get(id), id, this.color, obstacles));
            }
        }

        return list;
    }

    @Override
    public String getName() {
        return "role_attack_waiter";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setAvoidBall(boolean avoidBall) {
        this.avoidBall = avoidBall;
    }
}