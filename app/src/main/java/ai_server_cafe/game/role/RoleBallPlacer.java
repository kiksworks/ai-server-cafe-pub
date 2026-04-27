package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleBallPlacer extends AbstractRole {
    private Optional<Vector2D> ballPlacePos;

    public RoleBallPlacer(TeamColor color, int[] ids) {
        super(color, ids);
        this.ballPlacePos = Optional.of(Vector2D.ZERO);
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent()) {
            return list;
        }

        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();

        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;
        int chaser = MathHelper.nearestRobotToPosition(MathHelper.robotListFromIds(friendlyRobots, this.roleIds), ballPos).get().getId();

        if (this.ballPlacePos.isEmpty()) return list;

        this.ballPlaceMap.get(chaser).setAbpTarget(this.ballPlacePos.get());
        list.add(this.ballPlaceMap.get(chaser));

        this.visualizerTargets = List.of(ballPlacePos.get());

        return list;
    }

    @Override
    public String getName() {
        return "role_ballPlacer";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setBallPlacePos(Optional<Vector2D> ballPlacePos) {
        this.ballPlacePos = ballPlacePos;
    }
}