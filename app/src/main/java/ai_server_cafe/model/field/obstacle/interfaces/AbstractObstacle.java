package ai_server_cafe.model.field.obstacle.interfaces;

import ai_server_cafe.model.field.obstacle.IntegratedObstacle;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.SerializableJson;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractObstacle extends AbstractCloneable implements SerializableJson<AbstractObstacle> {
    public static final ObstacleCircle EMPTY = new ObstacleCircle(Vector2D.ZERO, 0.0, 0.0) {
        @Override
        public boolean isCollidedDetail(Vector2D vector2D, double additionalMargin) {
            return false;
        }

        @Override
        public boolean isCollidedDetail(AbstractObstacle obstacle, double step) {
            return false;
        }

        @Override
        @Nonnull
        public List<Vector2D> getTangentPoints(Vector2D vector2D) {
            return new ArrayList<>();
        }

        @Nonnull
        @Override
        public AxisAlignedBoundingBox getAABB() {
            return new AxisAlignedBoundingBox(Vector2D.ZERO, Vector2D.ZERO);
        }

        @Override
        public ObstacleCircle clone() {
            return EMPTY;
        }

        @Override
        @Nonnull
        public String toString() {
            return this.customName.isEmpty() ? "empty" : this.customName;
        }

        @Override
        public AbstractObstacle expandMargin(double value) {
            return this.clone();
        }
    };

    protected final double margin;
    protected final String customName;

    public AbstractObstacle(double margin) {
        this.margin = margin;
        this.customName = "";
    }

    public boolean isCollided(Vector2D vector2D, double additionalMargin) {
        if (additionalMargin == 0.0) {
            if (this.getAABB().isExcluded(vector2D)) {
                return false;
            }
        } else if (this.getAABB().isExcluded(new ObstacleCircle(vector2D, additionalMargin, 0.0).getAABB())) {
            return false;
        }
        return this.isCollidedDetail(vector2D, additionalMargin);
    }

    public abstract boolean isCollidedDetail(Vector2D vector2D, double additionalMargin);

    public boolean isCollided(Vector2D vector2D) {
        return this.isCollided(vector2D, 0);
    }

    public abstract boolean isCollidedDetail(AbstractObstacle obstacle, double step);

    public boolean isCollided(@Nonnull AbstractObstacle obstacle, double step) {
        if (obstacle instanceof IntegratedObstacle)
            throw new IllegalArgumentException("Unexpected argument : obstacle is IntegratedObstacle");
        if (this.getAABB().isExcluded(obstacle.getAABB())) {
            return false;
        }
        return this.isCollidedDetail(obstacle, step);
    }

    public abstract List<Vector2D> getTangentPoints(Vector2D vector2D);

    @Nonnull
    public abstract AxisAlignedBoundingBox getAABB();

    public double getMargin() {
        return this.margin;
    }

    public abstract AbstractObstacle clone();

    @Override
    public abstract String toString();

    public abstract AbstractObstacle expandMargin(double value);
}
