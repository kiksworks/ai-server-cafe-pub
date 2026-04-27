package ai_server_cafe.game.captain;

import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.formation.FormationHalt;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.util.TeamColor;

import java.util.Map;

public class CaptainHalt extends AbstractCaptain {
    public CaptainHalt(EnumCaptainType type, TeamColor color) {
        super(type, color);
    }

    @Override
    protected void setFormationMap(Map<EnumFormationType, Class<? extends AbstractFormation>> map) {
        map.put(EnumFormationType.HALT, FormationHalt.class);
        map.put(EnumFormationType.TIMEOUT, FormationHalt.class);
        map.put(EnumFormationType.STOP, FormationHalt.class);
        map.put(EnumFormationType.FORCE_START, FormationHalt.class);
        map.put(EnumFormationType.PENALTY, FormationHalt.class);
        map.put(EnumFormationType.SHOOTOUT, FormationHalt.class);
        map.put(EnumFormationType.KICKOFF, FormationHalt.class);
        map.put(EnumFormationType.SET_PLAY, FormationHalt.class);
        map.put(EnumFormationType.BALL_PLACEMENT, FormationHalt.class);
    }

    @Override
    protected void setVelocityMap(Map<EnumFormationType, Double> map) {

    }
}
