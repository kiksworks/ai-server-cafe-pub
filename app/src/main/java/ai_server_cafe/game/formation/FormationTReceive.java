package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleTShooter;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormationTReceive extends AbstractFormation {
    protected RoleTShooter roleTShooter;
    private boolean isPlus;
    public FormationTReceive(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleTShooter = new RoleTShooter(color, ids);
        this.isPlus = false;
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        Vector2D ballPos = world.get().getBall().position();
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        List<IntegratedRobot> visibleRobots = MathHelper.robotListFromIds(friendlyRobots, visibleIds);

        int mode = 1;
        if (mode == 0) {
            if (visibleIds.size() < 2) return list;
            int shooter = visibleIds.get(1);
            int receiver = visibleIds.get(0);
            List<Integer> shooterIds = new ArrayList<>();
            List<Integer> receiverIds = new ArrayList<>();
            List<Integer> haltIds = new ArrayList<>();
            for (int id : visibleIds) {
                if (id == shooter) {
                    shooterIds.add(id);
                } else if (id == receiver) {
                    receiverIds.add(id);
                } else {
                    haltIds.add(id);
                }
            }
            if (this.world.get().getBall().velocity().getNorm() > 500.0) {
                this.roleChaser.setRoleIds(receiverIds);
                haltIds.addAll(shooterIds);
            } else {
                this.roleTShooter.setRoleIds(shooterIds);
                haltIds.addAll(receiverIds);
            }
            this.roleHalt.setRoleIds(haltIds);
            list.add(this.roleTShooter);
            list.add(this.roleChaser);
            list.add(this.roleHalt);
        } else if (mode == 1) {
            if (visibleIds.isEmpty()) return list;
            int mover = visibleIds.get(0);
            List<Integer> moverIds = new ArrayList<>();
            List<Integer> haltIds = new ArrayList<>();
            for (int id : visibleIds) {
                if (id == mover) {
                    moverIds.add(id);
                } else {
                    haltIds.add(id);
                }
            }
            Vector2D posP = new Vector2D(1500, 0.0);
            Vector2D posM = new Vector2D(-1500, 0.0);
            FilteredRobot moveRobot = this.world.get().getFriendlyRobotMap(this.color).get(mover).getRobot();
            if (isPlus && moveRobot.position().subtract(posP).getNorm() < 10) {
                this.isPlus = false;
            } else if (!isPlus && moveRobot.position().subtract(posM).getNorm() < 10) {
                this.isPlus = true;
            }
            this.roleWaiter.setWaitPositionss(List.of(isPlus ? posP : posM));
            this.roleWaiter.setRoleIds(moverIds);
            this.roleHalt.setRoleIds(haltIds);
            list.add(roleWaiter);
            list.add(roleHalt);
        }
        return list;
    }

    @Override
    public String getName() {
        return "t_receive";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
