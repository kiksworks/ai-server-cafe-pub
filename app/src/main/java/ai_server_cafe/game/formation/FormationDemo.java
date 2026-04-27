package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleDemo;
import ai_server_cafe.game.role.RoleDemoController;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormationDemo extends AbstractFormation {
    private RoleDemo roleDemo;
    private RoleDemoController roleDemoController;

    public FormationDemo(TeamColor color, int[] ids) {
        super(color, ids);
        this.roleDemo = new RoleDemo(color, ids);
        this.roleDemoController = new RoleDemoController(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        if (visibleIds.isEmpty()) return list;
        int roleId = visibleIds.getFirst();
        List<Integer> haltIds = new ArrayList<>();

        AbstractRole role;
        if (ConfigManager.getInstance().getConfig()
                .getCaptain(color) == EnumCaptainType.DEMO) {
            role = this.roleDemo;
        } else {
            role = this.roleDemoController;
        }

        for (int id : visibleIds) {
            if (roleId == id && !this.roleDemo.isFinished()) {
                role.setRoleIds((Arrays.asList(id)));
            } else {
                haltIds.add(id);
            }
        }
        this.roleHalt.setRoleIds(haltIds);
        list.add(role);
        list.add(this.roleHalt);
        return list;
    }

    @Override
    public String getName() {
        return "formation_demo";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
