package ai_server_cafe.game.planner.path;

import ai_server_cafe.config.Config;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class TimeBellmanFord extends AbstractBellmanFord {
    protected TimeBellmanFord() {
        super("time-bellman-ford");
    }

    @Nonnull
    protected Pair<Double, Optional<Vector2D>> segmentScore(@Nonnull Vector2D line,
                                                            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
                                                            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Double> additional,
                                                            double nextAdditional) {
        final Config.PIDController config = ConfigManager.getInstance().getConfig().controllerConfig;
        Vector2D velEstimate = Vector2D.ZERO;
        if (vel.isPresent()) {
            velEstimate = vel.get();
        }
        double effectiveFactor = 1.0;
        final double LN_BRAKE = config.brakeToTargetVelocity;
        final double BRAKE = config.brakeToTargetPosition;
        final double ACCEL = config.accelToTargetVelocity;
        final double MAX_VEL = config.velocityMax;
        double penalty = 0.0;
        if (additional.isPresent()) {
            double add = additional.get();
            if (0.5 * velEstimate.getNormSq() / LN_BRAKE > add) {
                penalty = 1000.0;
            }
        }
        Vector2D vl = MathHelper.normalized(line);
        Vector2D vv = MathHelper.normalized(velEstimate);
        double cos = FastMath.abs(vl.dotProduct(vv));
        double v0 = velEstimate.dotProduct(vl);
        if (v0 > 0) {
            Pair<Double, Double> pair = timeSegment(ACCEL, BRAKE, v0, MAX_VEL, line.getNorm(), nextAdditional);
            return new Pair<>(pair.getFirst() + penalty, Optional.of(vl.scalarMultiply(pair.getSecond())));
        }
        double t4 = velEstimate.getNorm() * cos / LN_BRAKE;
        double consumeDist = effectiveFactor * 0.5 * velEstimate.getNormSq() * cos * cos / LN_BRAKE;
        Pair<Double, Double> pair =
                timeSegment(ACCEL, BRAKE, 0.0, MAX_VEL, line.getNorm() + consumeDist, nextAdditional);
        return new Pair<>(pair.getFirst() + t4 + penalty, Optional.of(vl.scalarMultiply(pair.getSecond())));
    }

    @Nonnull
    protected Pair<Double, Double> timeSegment(double acc, double brake, double v0, double vMax, double line,
                                               double nextAdditional) {
        double ve = FastMath.min(vMax, FastMath.min(FastMath.sqrt(2.0 * acc * line + v0 * v0),
                ControlHelper.getVelocityTrapezoidal(brake, nextAdditional)));
        double vm = FastMath.min(vMax,
                FastMath.sqrt((2.0 * acc * brake * (line + nextAdditional) + brake * v0 * v0) / (acc + brake)));
        double t1 = (vm - v0) / acc;
        double l1 = v0 * t1 + 0.5 * acc * t1 * t1;
        line -= l1;
        if (line <= 0) {
            t1 = (ve - v0) / acc;
            return new Pair<>(t1, ve);
        }
        double t3 = (vm - ve) / brake;
        double l3 = vm * t3 - 0.5 * brake * t3 * t3;
        line -= l3;
        if (line <= 0) {
            return new Pair<>(t1 + t3, ve);
        }
        double t2 = line / vm;
        return new Pair<>(t1 + t2 + t3, ve);
    }

    @Override
    protected double getScore(@Nonnull List<PathSide> path, Optional<Vector2D> vel) {
        double totalScore = 0.0;
        Optional<Vector2D> nextVel = vel;
        for (int i = 0; i < path.size(); i++) {
            Pair<Double, Optional<Vector2D>> pair = segmentScore(path.get(i).side, nextVel,
                    (i == 0) ? Optional.of(path.get(i).additionalVec.getNorm()) : Optional.empty(),
                    i + 1 < path.size() ? path.get(i + 1).additionalVec.getNorm() : 0.0);
            nextVel = pair.getSecond();
            totalScore += pair.getFirst();
        }
        return totalScore;
    }

    @Override
    protected double getTimeLimit() {
        return 0.008;
    }
}
