package ai_server_cafe.model.field.obstacle.base;

import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.ICircle;
import ai_server_cafe.model.field.obstacle.interfaces.IPolygon;
import ai_server_cafe.model.field.obstacle.interfaces.ISegment;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractObstaclePolygon extends AbstractObstacle implements IPolygon {
    protected final boolean isConvex;

    public AbstractObstaclePolygon(double margin, boolean isConvex) {
        super(margin);
        this.isConvex = isConvex;
    }

    public AbstractObstaclePolygon(double margin) {
        this(margin, true);
    }

    @Override
    public boolean isCollidedDetail(AbstractObstacle obstacle, double step) {
        if (obstacle instanceof ICircle || obstacle instanceof ISegment) {
            return obstacle.isCollidedDetail(this, step);
        } else if (obstacle instanceof IPolygon) {
            double l0 = 0;
            for (Pair<Vector2D, Vector2D> segment : this.getSegments()) {
                l0 += segment.getFirst().subtract(segment.getSecond()).getNorm();
            }
            double l1 = 0;
            for (Pair<Vector2D, Vector2D> segment : ((IPolygon)obstacle).getSegments()) {
                l1 += segment.getFirst().subtract(segment.getSecond()).getNorm();
            }
            if (l0 > l1) {
                return obstacle.isCollidedDetail(this, step);
            }
            for (Pair<Vector2D, Vector2D> segment : this.getSegments()) {
                Vector2D start = segment.getFirst();
                Vector2D end = segment.getSecond();
                double r = end.subtract(start).getNorm();
                if (r < MathHelper.EPSILON) {
                    if (obstacle.isCollided(start, this.getMargin())) {
                        return true;
                    }
                }
                for (double l = 0; l <= r; l += step) {
                    if (obstacle.isCollided(start.add(MathHelper.normalized(end.subtract(start)).scalarMultiply(l)), this.getMargin())) {
                        return true;
                    }
                }
            }
            // 内部に完全に入っているとき判定されないので逆も行う
            for (Pair<Vector2D, Vector2D> segment : ((IPolygon) obstacle).getSegments()) {
                Vector2D start = segment.getFirst();
                Vector2D end = segment.getSecond();
                if (this.isCollided(start, obstacle.getMargin())) {
                    return true;
                }
                if (this.isCollided(end, obstacle.getMargin())) {
                    return true;
                }
            }
            return false;
        } else {
            throw new IllegalArgumentException("Unknown obstacle type.");
        }
    }

    @Override
    public boolean isCollidedDetail(@Nonnull Vector2D vector2D, double additionalMargin) {
        if (this.isInside(vector2D))
            return true;
        for (Pair<Vector2D, Vector2D> segment : this.getSegments()) {
            if (new ObstacleSegment(segment.getFirst(), segment.getSecond(), this.margin).isCollided(vector2D, additionalMargin)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public List<Vector2D> getTangentPoints(Vector2D vector2D) {
        List<Vector2D> result = new ArrayList<>();
        if (this.isCollided(vector2D)) {
            return result;
        }
        List<Pair<Vector2D, Vector2D>> segments = this.getSegments();
        for (Pair<Vector2D, Vector2D> segment : segments) {
            List<Pair<Vector2D, Vector2D>> others = new ArrayList<>(segments);
            others.remove(segment);
            ObstacleSegment obstacleSegment = new ObstacleSegment(segment.getFirst(), segment.getSecond(), this.margin);
            for (Vector2D candidate : obstacleSegment.getTangentPoints(vector2D)) {
                boolean addFlag = false;
                if (this.isConvex) {
                    boolean flag = true;
                    for (Pair<Vector2D, Vector2D> other : others) {
                        if (ObstacleSegment.hasPip(other.getFirst(), other.getSecond(), candidate)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        addFlag = true;
                    }
                } else if (this.isAvailableTangentPoint(candidate)) {
                    addFlag = true;
                }
                if (addFlag && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    public abstract boolean isInside(Vector2D vector2D);

    public boolean isAvailableTangentPoint(@SuppressWarnings("unused") Vector2D tangentPoint) {
        return false;
    }

    public abstract AbstractObstaclePolygon clone();

    public String toString() {
        return this.customName.isEmpty() ? "polygon" : this.customName;
    }
}
