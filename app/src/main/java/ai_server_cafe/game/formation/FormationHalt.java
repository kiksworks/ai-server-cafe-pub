package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.util.TeamColor;

import java.util.ArrayList;
import java.util.List;

public class FormationHalt extends AbstractFormation {

    public FormationHalt(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    public List<AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty())
            return list;
        this.roleHalt.setRoleIds(getVisibleIds(this.world.get(), this.color, this.activeRobots));
        list.add(this.roleHalt);
        return list;
    }

    @Override
    public String getName() {
        return "formation_halt";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
