package ai_server_cafe.model.field.obstacle.base;

import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AxisAlignedBoundingBox;
import ai_server_cafe.model.field.obstacle.interfaces.ICircle;
import ai_server_cafe.model.field.obstacle.interfaces.IPolygon;
import ai_server_cafe.model.field.obstacle.interfaces.ISegment;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.util.math.MathHelper;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ObstacleCircle extends AbstractObstacle implements ICircle {
    protected final Vector2D center;
    protected final double radius;
    protected final AxisAlignedBoundingBox aabb;

    public ObstacleCircle() {
        super(0.0);
        this.center = Vector2D.ZERO;
        this.radius = 0.0;
        this.aabb = new AxisAlignedBoundingBox();
    }

    public ObstacleCircle(@Nonnull Vector2D center, double radius, double margin) {
        super(margin);
        this.center = center;
        this.radius = radius;
        this.aabb = new AxisAlignedBoundingBox(new Vector2D(center.getX() - radius, center.getY() - radius),
                new Vector2D(center.getX() + radius, center.getY() + radius), margin);
    }

    @Override
    public Vector2D getCenter() {
        return this.center;
    }

    @Override
    public double getRadius() {
        return this.radius;
    }

    @Override
    public boolean isCollidedDetail(@Nonnull Vector2D vector2D, double additionalMargin) {
        return vector2D.subtract(this.getCenter()).getNorm() <= this.getMargin() + this.radius + additionalMargin;
    }

    @Override
    public boolean isCollidedDetail(AbstractObstacle obstacle, double step) {
        if (obstacle instanceof ICircle || obstacle instanceof ISegment || obstacle instanceof IPolygon) {
            return obstacle.isCollided(this.getCenter(), this.getMargin() + this.radius);
        } else {
            throw new IllegalArgumentException("Unknown obstacle type.");
        }
    }

    @Override
    public List<Vector2D> getTangentPoints(Vector2D vector2D) {
        if (this.isCollided(vector2D)) {
            return new ArrayList<>();
        }
        return getTangentPoints(this.center, this.margin + this.radius, vector2D);
    }

    @Override
    @Nonnull
    public AxisAlignedBoundingBox getAABB() {
        return this.aabb;
    }

    @Nonnull
    public static List<Vector2D> getTangentPoints(@Nonnull Vector2D center, double radius, @Nonnull Vector2D target) {
        double r = center.subtract(target).getNorm();
        if (r < MathHelper.EPSILON) {
            return new ArrayList<>();
        }
        double theta = FastMath.asin(radius / r);
        Vector2D base = MathHelper.normalized(center.subtract(target));

        return List.of(MathHelper.applyRotation2D(base, theta).scalarMultiply(r * FastMath.cos(theta)).add(target),
                MathHelper.applyRotation2D(base, -theta).scalarMultiply(r * FastMath.cos(-theta)).add(target));
    }

    @Override
    public ObstacleCircle clone() {
        return new ObstacleCircle(this.center, this.radius, this.margin);
    }

    @Override
    public String toString() {
        return this.customName.isEmpty() ? "Circle:[center:" + this.center + ", radius:" + this.radius + "]" : this.customName;
    }

    @Override
    public AbstractObstacle expandMargin(double value) {
        return new ObstacleCircle(this.center, this.radius, this.margin + value);
    }

    @Override
    public AbstractObstacle deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        return new ObstacleCircle(RecordData1.getVector2D(jsonObject.get("center").getAsJsonObject()),
                jsonObject.get("radius").getAsDouble(), jsonObject.get("margin").getAsDouble());
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("center", RecordData1.convertVector2D(this.center));
        jsonObject.addProperty("radius", this.radius);
        jsonObject.addProperty("margin", this.margin);
        return jsonObject;
    }
}
