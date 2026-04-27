package ai_server_cafe.game.planner.path;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class BellmanFord extends AbstractBellmanFord {
    protected BellmanFord() {
        super("bellman-ford");
    }

    @Override
    protected double getScore(@Nonnull List<PathSide> path, Optional<Vector2D> vel) {
        double total = 0.0;
        for (PathSide side : path) {
            total += side.side.getNorm();
        }
        return total;
    }

    @Override
    protected double getTimeLimit() {
        return 0.008;
    }
}
