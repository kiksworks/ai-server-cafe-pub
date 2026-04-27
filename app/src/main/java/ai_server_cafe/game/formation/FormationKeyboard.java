package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleKeyboard;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class FormationKeyboard extends AbstractFormation {
    private final RoleKeyboard roleKeyboard;
    public FormationKeyboard(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleKeyboard = new RoleKeyboard(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        if(this.world.isEmpty() || this.teamInfo.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> visibleIds = MathHelper.toList(this.activeRobots);
        int keeper = this.teamInfo.get().getGoalkeeper();
        if (!visibleIds.contains(keeper)) return new ArrayList<>();
        this.roleKeyboard.setRoleIds(List.of(keeper));
        return List.of(this.roleKeyboard);
    }

    @Override
    public String getName() {
        return "keyboard";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
