package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.util.TeamColor;

import java.util.ArrayList;
import java.util.List;

public class RoleHalt extends AbstractRole {

    public RoleHalt(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent() || roleIds.isEmpty()) {
            return list;
        }

        for (int id : roleIds) {
            list.add(this.haltMap.get(id));
        }

        return list;
    }

    @Override
    public String getName() {
        return "role_halt";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}