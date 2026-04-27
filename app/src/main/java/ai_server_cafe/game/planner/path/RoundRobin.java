package ai_server_cafe.game.planner.path;

import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class RoundRobin extends AbstractRoundRobin {
    protected RoundRobin() {
        super("round-robin");
    }

    @Override
    protected double getScore(@Nonnull List<PathSide> path, Optional<Vector2D> vel) {
        double totalLength = 0.0;
        for (PathSide side : path) {
            totalLength += side.side.getNorm();
        }
        return totalLength + (vel.isPresent() ? -MathHelper.normalized(path.getFirst().side).dotProduct(vel.get()) : 0.0);
    }

    @Override
    protected double getTimeLimit() {
        return 0.008;
    }
}
