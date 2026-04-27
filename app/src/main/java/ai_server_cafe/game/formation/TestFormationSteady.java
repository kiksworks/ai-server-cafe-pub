package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.TestRoleChaser;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestFormationSteady extends AbstractFormation {
    protected TestRoleChaser testRoleChaser;

    public TestFormationSteady(TeamColor color, int[] ids) {
        super(color, ids);
        this.testRoleChaser = new TestRoleChaser(color, ids);
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
        int chaser = visibleIds.getFirst();
        List<Integer> haltIds = new ArrayList<>();
        for (int id : visibleIds) {
            if (chaser == id && !this.testRoleChaser.isFinished()) {
                this.testRoleChaser.setRoleIds(Arrays.asList(id));
            } else {
                haltIds.add(id);
            }
        }
        this.roleHalt.setRoleIds(haltIds);
        list.add(this.testRoleChaser);
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
