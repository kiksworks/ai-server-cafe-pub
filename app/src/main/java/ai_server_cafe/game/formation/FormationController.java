package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleController;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class FormationController extends AbstractFormation {
    private RoleController roleController;
    public FormationController(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleController = new RoleController(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if(this.world.isEmpty() || this.teamInfo.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = MathHelper.toList(this.activeRobots);
        int keeper = this.teamInfo.get().getGoalkeeper();
        if (!visibleIds.contains(keeper)) return list;
        this.roleController.setRoleIds(List.of(keeper));
        return List.of(this.roleController);
    }

    @Override
    public String getName() {
        return "controller";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
