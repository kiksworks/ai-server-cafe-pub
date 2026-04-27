package ai_server_cafe.game.planner.path;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class BreadthFirst extends AbstractDijkstra {
    public BreadthFirst() {
        super("breadth-first");
    }

    @Override
    public double getScore(@Nonnull List<PathSide> path, Optional<Vector2D> vel, boolean ignoreVelToObstacle) {
        return path.size();
    }

    @Override
    protected double getTimeLimit() {
        return 0.008;
    }
}
