package ai_server_cafe.game.captain;

import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.formation.FormationController;
import ai_server_cafe.game.formation.FormationKeyboard;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;

import java.util.Map;

public class CaptainController extends AbstractCaptain {
    public CaptainController(EnumCaptainType type, TeamColor color) {
        super(type, color);
    }

    @Override
    protected void setFormationMap(Map<EnumFormationType, Class<? extends AbstractFormation>> map) {
        map.put(EnumFormationType.FORCE_START, UpdaterWorld.getInstance().getControllerMode() ?
                FormationController.class : FormationKeyboard.class);
    }

    @Override
    protected void setVelocityMap(Map<EnumFormationType, Double> map) {}
}
