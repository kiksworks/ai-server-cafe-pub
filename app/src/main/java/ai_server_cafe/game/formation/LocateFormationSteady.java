package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleLocate;
import ai_server_cafe.util.TeamColor;

import java.util.ArrayList;
import java.util.List;

public class LocateFormationSteady extends AbstractFormation{
    private RoleLocate roleLocate;

    public LocateFormationSteady(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleLocate = new RoleLocate(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color,this.activeRobots);
        if (visibleIds.isEmpty()) return list;
        this.roleLocate.setRoleIds(visibleIds);
        list.add(roleLocate);
        return list;
    }

    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
