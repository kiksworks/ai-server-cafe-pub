package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.KickConverter;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class RoleTShooter extends AbstractRole {
    public RoleTShooter(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty() || this.roleIds.isEmpty()) {
            return list;
        }
        this.kickMap.get(this.roleIds.getFirst()).setTarget(Vector2D.ZERO);
        this.kickMap.get(this.roleIds.getFirst()).kickManually(new Pair<>(
                EnumKickType.STRAIGHT, KickConverter.toPower(this.roleIds.getFirst(), new Pair<>(EnumKickType.STRAIGHT, 5000.0))));
        this.kickMap.get(this.roleIds.getFirst()).setHoldTime(30);
        List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, this.roleIds.getFirst(), CommonObstacles.getMarginRobot(),
                this.world.get().getFriendlyRobotMap(this.color).get(this.roleIds.getFirst()).getRobot());
        list.add(new WithPlanner<>(this.kickMap.get(this.roleIds.getFirst()), this.roleIds.getFirst(), this.color, obstacles));
        return list;
    }

    @Override
    public String getName() {
        return "t_shooter";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
