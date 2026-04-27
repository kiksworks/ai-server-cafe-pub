package ai_server_cafe.game.planner.path;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class TimeDijkstraA extends TimeDijkstra {
    protected TimeDijkstraA() {
        super("time-dijkstra-3");
    }

    @Nonnull
    protected Pair<Double, Optional<Vector2D>> segmentScore(@Nonnull Vector2D line, Vector2D nextLine,
            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Double> additional,
            double nextAdditional) {
        return new TimeDijkstraB().segmentScore(line, nextLine, vel, additional, nextAdditional);
    }

    @Override
    public double getScore(@Nonnull List<PathSide> path, Optional<Vector2D> vel, boolean ignoreVelToObstacle) {
        double totalScore = 0.0;
        Optional<Vector2D> nextVel = vel;
        boolean flag = true;
        for (int i = 0; i < path.size(); i++) {
            Pair<Double, Optional<Vector2D>> pair = segmentScore(path.get(i).side, i + 1 < path.size() ? path.get(i + 1).side : Vector2D.ZERO, nextVel,
                    (i == 0 && !ignoreVelToObstacle) ? Optional.of(path.get(i).additionalVec.getNorm()) : Optional.empty(),
                    i + 1 < path.size() ? path.get(i + 1).additionalVec.getNorm() : 0.0);
            nextVel = pair.getSecond();
            totalScore += pair.getFirst();
        }
        return totalScore;
    }

    protected double vp(double theta, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double additional,
            double brake, double maxVel) {
        return this.vpA(theta, side1, side2, additional, brake, maxVel);
    }
}
