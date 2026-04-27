package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleTDribbler;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormationTDribble extends AbstractFormation {
    protected RoleTDribbler roleTDribbler;

    public FormationTDribble(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleTDribbler = new RoleTDribbler(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);

        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        List<IntegratedRobot> visibleRobots = MathHelper.robotListFromIds(friendlyRobots, visibleIds);
        if (visibleRobots.isEmpty())
            return list;
        int dribbler = visibleRobots.getFirst().getId();
        List<Integer> dribblerIds = new ArrayList<>();
        List<Integer> haltIds = new ArrayList<>();
        for (int id : visibleIds) {
            if (id == dribbler)
                dribblerIds.add(id);
            else
                haltIds.add(id);
        }
        roleTDribbler.setRoleIds(dribblerIds);
        roleHalt.setRoleIds(haltIds);
        list.add(roleTDribbler);
        list.add(roleHalt);
        return list;
    }

    @Override
    public String getName() {
        return "t_dribble";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
