package ai_server_cafe.model.field.obstacle.interfaces;

import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.SerializableJson;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

public final class AxisAlignedBoundingBox extends AbstractCloneable implements SerializableJson<AxisAlignedBoundingBox> {
    private final Vector2D min;
    private final Vector2D max;

    public AxisAlignedBoundingBox() {
        this.min = Vector2D.ZERO;
        this.max = Vector2D.ZERO;
    }

    public AxisAlignedBoundingBox(@Nonnull Vector2D start, @Nonnull Vector2D end) {
        this.min = new Vector2D(FastMath.min(start.getX(), end.getX()), FastMath.min(start.getY(), end.getY()));
        this.max = new Vector2D(FastMath.max(start.getX(), end.getX()), FastMath.max(start.getY(), end.getY()));
    }

    public AxisAlignedBoundingBox(@Nonnull Vector2D start, @Nonnull Vector2D end, double margin) {
        this.min = new Vector2D(FastMath.min(start.getX(), end.getX()) - margin, FastMath.min(start.getY(), end.getY()) - margin);
        this.max = new Vector2D(FastMath.max(start.getX(), end.getX()) + margin, FastMath.max(start.getY(), end.getY()) + margin);
    }

    @SuppressWarnings("unused")
    public boolean isIncluded(@Nonnull AxisAlignedBoundingBox aabb) {
        return !this.isExcluded(aabb);
    }

    public boolean isExcluded(@Nonnull AxisAlignedBoundingBox aabb) {
        return this.min.getX() > aabb.max.getX()
                || aabb.min.getX() > this.max.getX()
                || this.min.getY() > aabb.max.getY()
                || aabb.min.getY() > this.max.getY();
    }

    public boolean isPerfectlyIncluded(@Nonnull AxisAlignedBoundingBox aabb) {
        return isIncluded(aabb.min) && isIncluded(aabb.max);
    }

    public boolean isIncluded(@Nonnull Vector2D vector) {
        return this.min.getX() <= vector.getX() && vector.getX() <= this.max.getX()
                && this.min.getY() <= vector.getY() && vector.getY() <= this.max.getY();
    }

    public boolean isExcluded(@Nonnull Vector2D vector2D) {
        return !this.isIncluded(vector2D);
    }

    public boolean isIncluded(@Nonnull Vector2D start, Vector2D end) {
        return !this.isExcluded(new ObstacleSegment(start, end, 0.0).getAABB());
    }

    @SuppressWarnings("unused")
    public boolean isExcluded(@Nonnull Vector2D start, Vector2D end) {
        return !this.isIncluded(start, end);
    }

    @Override
    @Nonnull
    public AxisAlignedBoundingBox clone() {
        return new AxisAlignedBoundingBox(this.min, this.max);
    }

    @Nonnull
    public AxisAlignedBoundingBox add(@Nonnull AxisAlignedBoundingBox aabb) {
        Vector2D newMin = new Vector2D(FastMath.min(this.min.getX(), aabb.min.getX()), FastMath.min(this.min.getY(), aabb.min.getY()));
        Vector2D newMax = new Vector2D(FastMath.max(this.max.getX(), aabb.max.getX()), FastMath.max(this.max.getY(), aabb.max.getY()));
        return new AxisAlignedBoundingBox(newMin, newMax);
    }

    @Nonnull
    public String toString() {
        return "[min:" + this.min + ", max:" + this.max + "]";
    }

    @Override
    public AxisAlignedBoundingBox deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        return new AxisAlignedBoundingBox(
                new Vector2D(jsonObject.get("minX").getAsDouble(), jsonObject.get("minY").getAsDouble()),
                new Vector2D(jsonObject.get("maxX").getAsDouble(), jsonObject.get("maxY").getAsDouble()));
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("minX", this.min.getX());
        jsonObject.addProperty("minY", this.min.getY());
        jsonObject.addProperty("maxX", this.max.getX());
        jsonObject.addProperty("maxY", this.max.getY());
        return jsonObject;
    }
}
