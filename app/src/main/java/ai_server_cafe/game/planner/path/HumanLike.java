package ai_server_cafe.game.planner.path;

import ai_server_cafe.model.field.obstacle.IntegratedObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HumanLike extends AbstractPathPlanner {
    protected HumanLike() {
        super("human-like");
    }

    @Override
    protected double getTimeLimit() {
        return 0.008;
    }

    @Override
    protected Optional<List<PathSide>> getPath(Vector2D start, Vector2D goal, Optional<Vector2D> vel,
                                               List<IntegratedObstacle> integratedObstacles, double step, double margin,
                                               int depth, int additionalDepth, double maxPathLength,
                                               double maxAdditional, double timeLimit, double now,
                                               List<AbstractObstacle> obstacles) {
        Optional<AbstractObstacle> obstacle = this.getFirstCollided(start, goal, obstacles, step, maxPathLength, timeLimit, now);
        if (obstacle.isEmpty()) {
            return Optional.of(new ArrayList<>(List.of(new PathSide(goal.subtract(start), Vector2D.ZERO))));
        }
        int n = (int) (FastMath.PI * maxAdditional / step);
        double dTheta = 2.0 * FastMath.PI / n;
        List<Vector2D> tempGoals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Vector2D tempGoal = MathHelper.getFromPolar(maxAdditional, i * dTheta).add(start);
            tempGoals.add(this.getFirstCollidedPos(start, tempGoal, obstacles, step, maxPathLength, timeLimit, now));
        }
        Optional<Vector2D> optMin =
                tempGoals.stream().min(InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
                    @Override
                    public Double function(Vector2D vector2D) {
                        return getScore(vel, start, vector2D, goal);
                    }
                }));
        if (optMin.isPresent()) {
            Vector2D newGoal = optMin.get();
            PathSide side = new PathSide(newGoal.subtract(start), Vector2D.ZERO);
            return Optional.of(new ArrayList<>(List.of(side)));
        }
        return Optional.empty();
    }

    private double getScore(@Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
                            Vector2D start, Vector2D temp, Vector2D goal) {
        if (vel.isPresent()) {
            Vector2D r = temp.subtract(start);
            double velC = vel.get().getNorm() - vel.get().dotProduct(MathHelper.normalized(r));
            double rC = goal.subtract(temp).getNorm();
            return rC + 0.5 * velC;
        }
        return temp.subtract(goal).getNormSq();
    }
}
