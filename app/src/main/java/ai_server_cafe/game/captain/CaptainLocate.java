package ai_server_cafe.game.captain;

import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.formation.FormationHalt;
import ai_server_cafe.game.formation.LocateFormationSteady;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.util.TeamColor;

import java.util.Map;

public class CaptainLocate extends AbstractCaptain{
    public CaptainLocate(EnumCaptainType type, TeamColor color) {
        super(type, color);
    }

    @Override
    protected void setFormationMap(Map<EnumFormationType, Class<? extends AbstractFormation>> map) {
        map.put(EnumFormationType.HALT, FormationHalt.class);
        map.put(EnumFormationType.FORCE_START, LocateFormationSteady.class);
    }

    @Override
    protected void setVelocityMap(Map<EnumFormationType, Double> map) {

    }
}
