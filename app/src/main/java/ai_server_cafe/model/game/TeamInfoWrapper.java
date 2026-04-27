package ai_server_cafe.model.game;

import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.util.interfaces.AbstractCloneable;

import java.util.Optional;

public class TeamInfoWrapper extends AbstractCloneable {
    private final Optional<GcRefereeMessage.Referee.TeamInfo> info;
    public TeamInfoWrapper(Optional<GcRefereeMessage.Referee.TeamInfo> info) {
        this.info = info;
    }

    @Override
    public TeamInfoWrapper clone() {
        return new TeamInfoWrapper(this.info);
    }

    public Optional<GcRefereeMessage.Referee.TeamInfo> getInfo() {
        return this.info;
    }
}
