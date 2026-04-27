package ai_server_cafe.game.action;

import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.TeamColor;

public class ActionHalt extends AbstractAction {
    public ActionHalt(int id, TeamColor color) {
        super(id, color);
    }

    @Override
    public Command execute() {
        return new Command().setHalt(true);
    }

    @Override
    public String getName() {
        return "halt";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
