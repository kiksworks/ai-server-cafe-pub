package ai_server_cafe.game.role;

import ai_server_cafe.device.joystick.StatusJoyStick;
import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionController;
import ai_server_cafe.updater.UpdaterJoyStick;
import ai_server_cafe.util.TeamColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleController extends AbstractRole {
    private Map<Integer, ActionController> actionControllerMap;

    public RoleController(TeamColor color, int[] ids) {
        super(color, ids);
        this.actionControllerMap = new HashMap<>();
        for (int id : ids) {
            this.actionControllerMap.put(id, new ActionController(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        List<StatusJoyStick> states = new ArrayList<>(UpdaterJoyStick.getInstance().update().get());
        boolean flag = false;
        for (int id : this.roleIds) {
            if(!flag) {
                flag = true;
                if (states.size() > this.color.getId()) {
                    this.actionControllerMap.get(id).setControllerStatus(states.get(this.color.getId()).clone());
                    list.add(this.actionControllerMap.get(id));
                    continue;
                }
                if (!states.isEmpty()) {
                    this.actionControllerMap.get(id).setControllerStatus(states.getFirst().clone());
                    list.add(this.actionControllerMap.get(id));
                    continue;
                }
            }
            list.add(this.haltMap.get(id));
        }
        return list;
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
