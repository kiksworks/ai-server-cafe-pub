package ai_server_cafe.game.planner.path;

import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * plannerで使う用のデータクラス
 */
public final class DataSet extends AbstractCloneable {
    public final Vector2D start;
    public final Vector2D goal;
    public final List<AbstractObstacle> obstacles;
    final double step;
    final int depth;
    final int additionalDepth;
    public final Optional<Vector2D> vel;
    public boolean allowObstacleEnd;
    public int id;

    public DataSet(@Nonnull Vector2D start, @Nonnull Vector2D goal, @Nonnull List<AbstractObstacle> obstacles,
            double step, int depth, int additionalDepth, Optional<Vector2D> vel, boolean allowObstacleEnd, int id) {
        this.start = start;
        this.goal = goal;
        this.obstacles = new ArrayList<>();
        for (AbstractObstacle obstacle : obstacles) {
            this.obstacles.add(obstacle.clone());
        }
        this.step = step;
        this.depth = depth;
        this.additionalDepth = additionalDepth;
        this.vel = vel;
        this.allowObstacleEnd = allowObstacleEnd;
    }

    public DataSet(@Nonnull Vector2D start, @Nonnull Vector2D goal, @Nonnull List<AbstractObstacle> obstacles,
                   double step, int depth, int additionalDepth, Optional<Vector2D> vel, int id) {
        this(start, goal, obstacles, step, depth, additionalDepth, vel, false, id);
    }

    @Override
    @Nonnull
    public DataSet clone() {
        return new DataSet(start, goal, obstacles, step, depth, additionalDepth, vel, allowObstacleEnd, id);
    }
}
