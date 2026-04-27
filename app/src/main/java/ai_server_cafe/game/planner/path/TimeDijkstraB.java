package ai_server_cafe.game.planner.path;

import ai_server_cafe.config.Config;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class TimeDijkstraB extends TimeDijkstra {
    protected TimeDijkstraB() {
        super("time-dijkstra-3");
    }

    @Nonnull
    protected Pair<Double, Optional<Vector2D>> segmentScore(@Nonnull Vector2D line, Vector2D nextLine,
            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Vector2D> vel,
            @Nonnull @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Double> additional,
            double nextAdditional) {
        final Config.PIDController config = ConfigManager.getInstance().getConfig().controllerConfig;
        Vector2D velEstimate = Vector2D.ZERO;
        if (vel.isPresent()) {
            velEstimate = vel.get();
        }
        final double LN_BRAKE = config.brakeToTargetPosition;
        final double BRAKE = config.brakeToTargetVelocity;
        final double ACCEL = config.accelToTargetVelocity;
        final double MAX_VEL = config.velocityMax;

        {
            double theta = MathHelper.inferiorAngle(nextLine, line);
            double l2 = nextLine.getNorm();
            double phi = MathHelper.getPhi(1.0, 1.0, theta);
            double r = 0.5 * l2 / FastMath.sin(phi);
            double perpendicularAcc = WithPlanner.getMinAccelRatio() * LN_BRAKE;
            double ve = FastMath.min(ControlHelper.getVelocityTrapezoidal(LN_BRAKE, nextAdditional) / FastMath.cos(theta - phi), FastMath.sqrt( r * perpendicularAcc));
            nextAdditional = ControlHelper.getDistanceTrapezoidal(LN_BRAKE, FastMath.min(MAX_VEL, ve) * FastMath.cos(theta - phi));
        }

        Vector2D vl = MathHelper.normalized(line);
        Vector2D vv = MathHelper.normalized(velEstimate);

        double penalty = 0;
        if (additional.isPresent()) {
            double add = additional.get();
            if (velEstimate.getNorm() > 50.0 && 0.5 * velEstimate.getNormSq() / LN_BRAKE > add) {
                penalty = vl.dotProduct(vv) < 0 ? 2000.0 : 1000.0;
            }
        }
        double l1 = line.getNorm();
        double theta2 = MathHelper.directionFrom(nextLine, line);
        double dt = 0.0;
        double vp = velEstimate.dotProduct(MathHelper.normalized(line));
        if (vp < 0) {
            l1 += ControlHelper.getDistanceTrapezoidal(BRAKE, -vp);
            dt += -vp / BRAKE;
            vp = 0.0;
        }
        double phi = MathHelper.getPhi(1.0, 1.0, theta2);
        Vector2D phiVec = MathHelper.getFromPolar(1.0, MathHelper.direction(line) + phi);
        double maxVe = FastMath.min(ControlHelper.getVelocityTrapezoidal(BRAKE, nextAdditional), MAX_VEL);
        Pair<Double, Double> result = ControlHelper.timeForTrapezoidalControl(ACCEL, BRAKE, MAX_VEL, vp, maxVe, l1);
        return new Pair<>(result.getFirst() + penalty + dt, Optional.of(phiVec.scalarMultiply(result.getSecond() * FastMath.cos(phi))));
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

    protected void checkScore(double prevScore, double newScore) {
        double d = newScore - prevScore;
        if (d < 0.0) {
            // System.out.println(Thread.currentThread().getName() + ":" + d);
        }
    }

    protected double vp(double theta, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double additional,
            double brake, double maxVel) {
        return this.vpB(theta, side1, side2, additional, brake, maxVel);
    }
}
