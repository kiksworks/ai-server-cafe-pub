package ai_server_cafe.game.planner.path;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class Dijkstra extends AbstractDijkstra {
    protected Dijkstra() {
        super("dijkstra");
    }

    @Override
    public double getScore(@Nonnull List<PathSide> path, Optional<Vector2D> vel, boolean ignoreVelToObstacle) {
        double totalLength = 0.0;
        for (PathSide side : path) {
            totalLength += side.side.getNorm();
        }
        return totalLength;
    }

    @Override
    protected double getTimeLimit() {
        return 0.008;
    }
}
