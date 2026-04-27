package ai_server_cafe.game.captain;

import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.formation.FormationTDribble;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.util.TeamColor;

import javax.annotation.Nonnull;
import java.util.Map;

public class CaptainTDribble extends AbstractCaptain {
    public CaptainTDribble(EnumCaptainType type, TeamColor color) {
        super(type, color);
    }

    @Override
    protected void setFormationMap(@Nonnull Map<EnumFormationType, Class<? extends AbstractFormation>> map) {
        map.put(EnumFormationType.FORCE_START, FormationTDribble.class);
    }

    @Override
    protected void setVelocityMap(@Nonnull Map<EnumFormationType, Double> map) {

    }
}
