package ai_server_cafe.game.formation;

import ai_server_cafe.config.Config;
import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormationTimeout extends AbstractFormation {

    public FormationTimeout(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty())
            return list;
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, Vector2D> positionsWithId = new HashMap<>();
        Config config = ConfigManager.getInstance().getConfig();
        for (int id : this.activeRobots) {
            positionsWithId.put(id, new Vector2D(this.world.get().getField().getPenaltyBackX() + 300.0 + id * 240.0,
                    world.get().getField().getMaxY() * ((this.isInverse() ^ config.exitPositive) ? 1 : -1)));
        }
        this.roleWaiter.setRoleIds(visibleIds);
        this.roleWaiter.setWaitPositionsWithId(positionsWithId);
        list.add(roleWaiter);

        return list;
    }

    @Override
    public String getName() {
        return "timeout";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
