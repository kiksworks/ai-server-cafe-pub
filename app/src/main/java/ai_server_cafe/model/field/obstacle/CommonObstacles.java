package ai_server_cafe.model.field.obstacle;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FieldObject;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.Equation;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonObstacles {
    @Nonnull
    public static AbstractObstacle getOppositePenaltyArea(@Nonnull Field field, double margin) {
        return new ObstacleBox(new Vector2D(field.getPenaltyFrontX(), field.getPenaltyMinY()),
                new Vector2D(field.getMaxX() + 10000.0, field.getPenaltyMaxY()), margin);
    }

    @Nonnull
    public static AbstractObstacle getFriendlyPenaltyArea(@Nonnull Field field, double margin) {
        return new ObstacleBox(new Vector2D(field.getMinX() - 10000.0, field.getPenaltyMinY()),
                new Vector2D(field.getPenaltyBackX(), field.getPenaltyMaxY()), margin);
    }

    @Nonnull
    public static List<AbstractObstacle> getFieldOutSideLines(@Nonnull Field field, double margin) {
        List<AbstractObstacle> result = new ArrayList<>();
        result.add(new ObstacleSegment(new Vector2D(-field.getFieldWidth() / 2 - margin,
                -field.getFieldHeight() / 2 - margin),
                new Vector2D(field.getFieldWidth() / 2 + margin, -field.getFieldHeight() / 2 - margin), margin));
        result.add(new ObstacleSegment(new Vector2D(-field.getFieldWidth() / 2 - margin,
                -field.getFieldHeight() / 2 - margin),
                new Vector2D(-field.getFieldWidth() / 2 - margin, field.getFieldHeight() / 2 + margin), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getFieldWidth() / 2 + margin,
                field.getFieldHeight() / 2 + margin),
                new Vector2D(field.getFieldWidth() / 2 + margin, -field.getFieldHeight() / 2 - margin), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getFieldWidth() / 2 + margin,
                field.getFieldHeight() / 2 + margin),
                new Vector2D(-field.getFieldWidth() / 2 - margin, field.getFieldHeight() / 2 + margin), margin));
        return result;
    }

    @Nonnull
    public static List<AbstractObstacle> getFieldOutSideLinesDemo(@Nonnull Field field, double margin) {
        List<AbstractObstacle> result = new ArrayList<>();
        result.add(new ObstacleSegment(new Vector2D(-field.getGameWidth() / 2 - margin,
                -field.getGameHeight() / 2 - margin),
                new Vector2D(field.getGameWidth() / 2 + margin, -field.getGameHeight() / 2 - margin), margin));
        result.add(new ObstacleSegment(new Vector2D(-field.getGameWidth() / 2 - margin,
                -field.getGameHeight() / 2 - margin),
                new Vector2D(-field.getGameWidth() / 2 - margin, field.getGoalMinY() - margin), margin));
        result.add(new ObstacleSegment(new Vector2D(-field.getGameWidth() / 2 - margin,
                field.getGoalMinY() - margin),
                new Vector2D(-field.getGameWidth() / 2 - margin - field.getGoalWidth(), field.getGoalMinY() - margin), margin));
        result.add(new ObstacleSegment(new Vector2D(-field.getGameWidth() / 2 - margin - field.getGoalWidth(),
                field.getGoalMinY() - margin),
                new Vector2D(-field.getGameWidth() / 2 - margin - field.getGoalWidth(), field.getGoalMaxY() + margin), margin));
        result.add(new ObstacleSegment(new Vector2D(-field.getGameWidth() / 2 - margin - field.getGoalWidth(),
                -field.getGoalMaxY() + margin),
                new Vector2D(-field.getGameWidth() / 2 - margin, field.getGoalMaxY() + margin), margin));
        result.add(new ObstacleSegment(new Vector2D(-field.getGameWidth() / 2 - margin,
                field.getGoalMaxY() + margin),
                new Vector2D(-field.getGameWidth() / 2 - margin, field.getGameHeight() / 2 + margin), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getGameWidth() / 2 + margin,
                field.getGameHeight() / 2 + margin),
                new Vector2D(field.getGameWidth() / 2 + margin, -field.getGameHeight() / 2 - margin), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getGameWidth() / 2 + margin,
                field.getGameHeight() / 2 + margin),
                new Vector2D(-field.getGameWidth() / 2 - margin, field.getGameHeight() / 2 + margin), margin));
        return result;
    }

    @Nonnull
    public static List<AbstractObstacle> getFriendlyGoal(@Nonnull Field field, double margin) {
        List<AbstractObstacle> result = new ArrayList<>();
        result.add(new ObstacleSegment(new Vector2D(field.getGoalBackX(), field.getGoalMinY()),
                new Vector2D(field.getMinX(), field.getGoalMinY()), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getGoalBackX(), field.getGoalMaxY()),
                new Vector2D(field.getMinX(), field.getGoalMaxY()), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getGoalBackX(), field.getGoalMinY()),
                new Vector2D(field.getGoalBackX(), field.getGoalMaxY()), margin));
        return result;
    }

    @Nonnull
    public static List<AbstractObstacle> getOppositeGoal(@Nonnull Field field, double margin) {
        List<AbstractObstacle> result = new ArrayList<>();
        result.add(new ObstacleSegment(new Vector2D(field.getGoalFrontX(), field.getGoalMinY()),
                new Vector2D(field.getMaxX(), field.getGoalMinY()), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getGoalFrontX(), field.getGoalMaxY()),
                new Vector2D(field.getMaxX(), field.getGoalMaxY()), margin));
        result.add(new ObstacleSegment(new Vector2D(field.getGoalFrontX(), field.getGoalMinY()),
                new Vector2D(field.getGoalFrontX(), field.getGoalMaxY()), margin));
        return result;
    }

    @Nonnull
    public static List<AbstractObstacle> getFriendlyRobotsIdsExcluded(@Nonnull World world, TeamColor color, int[] ids, FieldObject fo, double margin, boolean filter) {
        List<AbstractObstacle> result = new ArrayList<>();
        List<Integer> idsList = Arrays.stream(ids).boxed().toList();
        for (IntegratedRobot robot : world.getFriendlyRobotMap(color).values()) {
            if (!idsList.contains(robot.getId())) {
                Vector2D targetPos = robot.getRobot().position();
                Vector2D targetVel = robot.getRobot().velocity();
                if (filter && ignoreRobot(targetPos, targetVel, fo.position(), fo.velocity())) {
                    continue;
                }
                result.add(
                        new ObstacleCircle(getDynamicRobotPos(targetPos, targetVel, fo.position()),
                                getDynamicRobotRadius(targetPos, targetVel, fo.position()), margin));
            }
        }
        return result;
    }

    @Nonnull
    public static AbstractObstacle getDynamicRobotObstacle(Vector2D obstaclePos, Vector2D obstacleVel, Vector2D currentPos, double margin) {
        return new ObstacleCircle(getDynamicRobotPos(obstaclePos, obstacleVel, currentPos),
                getDynamicRobotRadius(obstaclePos, obstacleVel, currentPos), margin);
    }

    @Nonnull
    public static List<AbstractObstacle> getFriendlyRobotsIdsExcluded(@Nonnull World world, TeamColor color, int[] ids, FieldObject fo, double margin) {
        return getFriendlyRobotsIdsExcluded(world, color, ids, fo, margin, true);
    }

    @Nonnull
    public static List<AbstractObstacle> getOppositeRobotsIdsExcluded(@Nonnull World world, TeamColor color, int[] ids, FieldObject fo, double margin, boolean filter) {
        List<AbstractObstacle> result = new ArrayList<>();
        List<Integer> idsList = Arrays.stream(ids).boxed().toList();
        for (IntegratedRobot robot :  world.getOppositeRobotMap(color).values()) {
            if (!idsList.contains(robot.getId())) {
                Vector2D targetPos = robot.getRobot().position();
                Vector2D targetVel = robot.getRobot().velocity();
                if (filter && ignoreRobot(targetPos, targetVel, fo.position(), fo.velocity())) {
                    continue;
                }
                result.add(new ObstacleCircle(getDynamicRobotPos(targetPos, targetVel, fo.position()),
                        getDynamicRobotRadius(targetPos, targetVel, fo.position()), margin));
            }
        }
        return result;
    }

    @Nonnull
    public static List<AbstractObstacle> getOppositeRobotsIdsExcluded(@Nonnull World world, TeamColor color, int[] ids, FieldObject fo, double margin) {
        return getOppositeRobotsIdsExcluded(world, color, ids, fo, margin, true);
    }

    @Nonnull
    public static List<AbstractObstacle> getOppositeRobots(@Nonnull World world, TeamColor color, FieldObject fo, double margin, boolean filter) {
        return getOppositeRobotsIdsExcluded(world, color, new int[] {}, fo, margin, filter);
    }

    @Nonnull
    public static List<AbstractObstacle> getOppositeRobots(@Nonnull World world, TeamColor color, FieldObject fo, double margin) {
        return getOppositeRobots(world, color, fo, margin, false);
    }

    @Nonnull
    public static List<AbstractObstacle> getAllExcludedARobot(@Nonnull World world, TeamColor color, int id,
                                                              double margin, FieldObject fo) {
        List<AbstractObstacle> result = new ArrayList<>();
        result.add(getFriendlyPenaltyArea(world.getField(), margin));
        result.add(getOppositePenaltyArea(world.getField(), margin));
        result.addAll(getOppositeRobots(world, color, fo, margin, true));
        result.addAll(getFriendlyRobotsIdsExcluded(world, color, new int[]{id}, fo, margin, true));
        return result;
    }

    @Nonnull
    public static List<AbstractObstacle> getAllExcluded2Robots(@Nonnull World world, TeamColor color, int friendlyId, int oppositeId, double margin, FieldObject fo) {
        List<AbstractObstacle> result = new ArrayList<>();
        result.add(getFriendlyPenaltyArea(world.getField(), margin));
        result.add(getOppositePenaltyArea(world.getField(), margin));
        result.addAll(getOppositeRobotsIdsExcluded(world, color, new int[] {oppositeId}, fo, margin));
        result.addAll(getFriendlyRobotsIdsExcluded(world, color, new int[] {friendlyId}, fo, margin));
        return result;
    }

    @Nonnull
    public static AbstractObstacle getObstacleBall(@Nonnull Vector2D pos, @Nonnull Vector2D vel, double dt,
                                                   double margin) {
        return new ObstacleSegment(pos, pos.add(vel.scalarMultiply(dt)), margin);
    }

    public static double getMarginRobot() {
        return ConfigManager.getInstance().getConfig().robotRadius;
    }

    public static double getRobotRadius() {
        return ConfigManager.getInstance().getConfig().robotRadius;
    }

    // |r + t*v| = v0*t となる t
    private static double getDynamicMarginTime(@Nonnull Vector2D targetPos, @Nonnull Vector2D targetVel, @Nonnull Vector2D pos) {
        final double velMax = ConfigManager.getInstance().getConfig().controllerConfig.velocityMax;
        final double v0 = 0.8 * velMax;
        Vector2D r = targetPos.subtract(pos);
        double max = 0.0001 * velMax;
        double a = targetVel.getNormSq() - v0 * v0;
        double b = 2.0 * targetVel.dotProduct(r);
        double c = r.getNormSq();
        double[] roots = Equation.realRoots2dim(a, b, c);
        if (roots.length == 0 || roots[0] <= 0) {
            return FastMath.min(targetPos.subtract(pos).getNorm() / v0, max);
        } else {
            double result = Double.MAX_VALUE;
            for (double root : Arrays.stream(roots).boxed().toList()) {
                if (root > 0) {
                    result = FastMath.min(result, root);
                }
            }
            return FastMath.min(result, max);
        }
    }

    private static double getDynamicRobotRadius(@Nonnull Vector2D targetPos, @Nonnull Vector2D targetVel, @Nonnull Vector2D pos) {
        double acc = ConfigManager.getInstance().getConfig().controllerConfig.accelToTargetVelocity;
        double dt = getDynamicMarginTime(targetPos, targetVel, pos);
        return getRobotRadius() + 0.5 * acc * dt * dt;
    }

    private static Vector2D getDynamicRobotPos(@Nonnull Vector2D targetPos, @Nonnull Vector2D targetVel, @Nonnull Vector2D pos) {
        double dt = getDynamicMarginTime(targetPos, targetVel, pos);
        return targetPos.add(targetVel.scalarMultiply(dt));
    }

    private static boolean ignoreRobot(@Nonnull Vector2D targetPos, @Nonnull Vector2D targetVel, @Nonnull Vector2D ownPos, @Nonnull Vector2D ownVel) {
        boolean isTargetMoving = targetVel.getNorm() > 200.0;
        double ourMovingVel = MathHelper.normalized(targetPos.subtract(ownPos)).dotProduct(ownVel);
        double oppositeMovingVel = MathHelper.normalized(ownPos.subtract(targetPos)).dotProduct(targetVel);
        // 差が500.0以内だと両方foulが取られる 余裕を持って1500.0に設定
        boolean state = oppositeMovingVel > 0 && ourMovingVel > 0 && ourMovingVel + 1500.0 < oppositeMovingVel;
        return isTargetMoving && state;
    }
}
