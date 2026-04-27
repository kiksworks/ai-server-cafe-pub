package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleTAction;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.RobotTask;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterTestTasks;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormationTestSteady extends AbstractFormation {
    protected RoleTAction roleTAction;

    public FormationTestSteady(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleTAction = new RoleTAction(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        Vector2D ballPos = world.get().getBall().position();
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color,this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        if (visibleIds.isEmpty()) return list;


        List<IntegratedRobot> visibleRobots = MathHelper.robotListFromIds(friendlyRobots, visibleIds);
        // ドリブル判定
        {
            Optional<IntegratedRobot> nearestRobot = MathHelper.nearestRobotToPosition(visibleRobots, ballPos);
            if (nearestRobot.isPresent()) {
                UpdaterWorld.getInstance().setHaveBall(
                        MathHelper.distancePositionToRobot(ballPos, nearestRobot.get().getRobot()) < ROBOT_RAD * 2, this.color);
            }
        }
        List<Integer> haltIds = new ArrayList<>();
        Map<Integer, RobotTask> fixedTasks = new HashMap<>();
        Map<String, Map<Integer, RobotTask>> roleTasks = new HashMap<>();
        Deque<RobotTask> nonFixedTasks = new ArrayDeque<>();
        for (RobotTask task : UpdaterTestTasks.getInstance().getTasks(this.color).get()) {
            if (task.color == this.color) {
                if (task.id == -1) {
                    nonFixedTasks.addFirst(task);
                } else if (visibleIds.contains(task.id)) {
                    fixedTasks.put(task.id, task);
                }
            }
        }
        for (int id : visibleIds) {
            if (!fixedTasks.containsKey(id)) {
                if (!nonFixedTasks.isEmpty()) {
                    fixedTasks.put(id, nonFixedTasks.pollFirst());
                } else {
                    haltIds.add(id);
                }
            }
        }
        for (int id : fixedTasks.keySet()) {
            RobotTask task = fixedTasks.get(id);
            if (task.isTypeAction()) {
                if (!roleTasks.containsKey("action")) {
                    roleTasks.put("action", new HashMap<>());
                }
                roleTasks.get("action").put(id, task);
            } else if (task.isTypeRole()) {
                if (!roleTasks.containsKey(task.taskName)) {
                    roleTasks.put(task.taskName, new HashMap<>());
                }
                roleTasks.get(task.taskName).put(id, task);
            }
        }
        for (String key : roleTasks.keySet()) {
            if (key.equals("action")) {
                this.roleTAction.setRoleIds(roleTasks.get(key).keySet().stream().toList());
                this.roleTAction.setTaskMap(roleTasks.get(key));
                list.add(this.roleTAction);
            } else {
                // TODO implement other roles
            }
        }
        this.roleHalt.setRoleIds(haltIds);
        list.add(this.roleHalt);
        return list;
    }

    @Override
    public String getName() {
        return "test_steady";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
