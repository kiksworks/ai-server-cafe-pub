package ai_server_cafe.game.captain;

import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.formation.FormationBallPlacement;
import ai_server_cafe.game.formation.FormationHalt;
import ai_server_cafe.game.formation.FormationKickoff;
import ai_server_cafe.game.formation.FormationPenaltyKick;
import ai_server_cafe.game.formation.FormationSetplay;
import ai_server_cafe.game.formation.FormationSteady;
import ai_server_cafe.game.formation.FormationStop;
import ai_server_cafe.game.formation.FormationTimeout;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.util.TeamColor;

import javax.annotation.Nonnull;
import java.util.Map;

public class CaptainFirst extends AbstractCaptain {
    public CaptainFirst(EnumCaptainType type, TeamColor color) {
        super(type, color);
    }

    @Override
    protected void setFormationMap(@Nonnull Map<EnumFormationType, Class<? extends AbstractFormation>> map) {
        map.put(EnumFormationType.HALT, FormationHalt.class);
        map.put(EnumFormationType.TIMEOUT, FormationTimeout.class);
        map.put(EnumFormationType.STOP, FormationStop.class);
        map.put(EnumFormationType.FORCE_START, FormationSteady.class);
        map.put(EnumFormationType.PENALTY, FormationPenaltyKick.class);
        map.put(EnumFormationType.SHOOTOUT, FormationPenaltyKick.class);
        map.put(EnumFormationType.KICKOFF, FormationKickoff.class);
        map.put(EnumFormationType.SET_PLAY, FormationSetplay.class);
        map.put(EnumFormationType.BALL_PLACEMENT, FormationBallPlacement.class);
    }

    @Override
    protected void setVelocityMap(@Nonnull Map<EnumFormationType, Double> map) {
        map.put(EnumFormationType.STOP, 1200.0);
        map.put(EnumFormationType.TIMEOUT, 1200.0);
    }
}
