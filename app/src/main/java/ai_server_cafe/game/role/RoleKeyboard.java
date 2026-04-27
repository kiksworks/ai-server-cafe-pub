package ai_server_cafe.game.role;

import ai_server_cafe.device.keyboard.StatusKeyboard;
import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionKeyboard;
import ai_server_cafe.updater.UpdaterKeyboard;
import ai_server_cafe.util.TeamColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleKeyboard extends AbstractRole {
    private final Map<Integer, ActionKeyboard> actionKeyboardMap;

    public RoleKeyboard(TeamColor color, int[] ids) {
        super(color, ids);
        this.actionKeyboardMap = new HashMap<>();
        for (int id : ids) {
            this.actionKeyboardMap.put(id, new ActionKeyboard(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        StatusKeyboard status = UpdaterKeyboard.getInstance().update();
        for (int id : this.roleIds) {
            if (status != StatusKeyboard.EMPTY) {
                this.actionKeyboardMap.get(id).setKeyboardState(status.clone());
                list.add(this.actionKeyboardMap.get(id));
                status = StatusKeyboard.EMPTY;
            } else {
                list.add(this.haltMap.get(id));
            }
        }
        return list;
    }

    @Override
    public String getName() {
        return "role_keyboard";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
