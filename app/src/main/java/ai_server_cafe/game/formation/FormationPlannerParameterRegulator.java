package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RolePlannerParameterRegulator;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.util.TeamColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FormationPlannerParameterRegulator extends AbstractFormation {
    private final RolePlannerParameterRegulator rolePlannerParameterRegulator;
    private static final Logger LOGGER = LogManager.getLogger("kick-regulator");

    public FormationPlannerParameterRegulator(TeamColor color, int[] ids) {
        super(color, ids);
        this.rolePlannerParameterRegulator = new RolePlannerParameterRegulator(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        if(visibleIds.isEmpty()) return list;

        // fieldの大きさに応じてregulator位置を変更
        final Field wf = this.world.get().getField();
        this.rolePlannerParameterRegulator.setX(-wf.getGameWidth() / 2.0);
        this.rolePlannerParameterRegulator.setY(wf.getPenaltyMaxY() + 300.0);

        this.rolePlannerParameterRegulator.setRoleIds(visibleIds);
        list.add(this.rolePlannerParameterRegulator);
        return list;
    }

    @Override
    public String getName() {
        return "planner_parameter_regulator";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
