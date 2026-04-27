package ai_server_cafe.model.field.obstacle;

import ai_server_cafe.model.field.obstacle.base.AbstractObstaclePolygon;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AxisAlignedBoundingBox;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ObstacleBox extends AbstractObstaclePolygon {
    protected final Vector2D min;
    protected final Vector2D max;
    private final Vector2D minX;
    private final Vector2D minY;
    private final AxisAlignedBoundingBox aabb;

    public ObstacleBox() {
        super(0.0);
        this.min = Vector2D.ZERO;
        this.max = Vector2D.ZERO;
        this.minX = Vector2D.ZERO;
        this.minY = Vector2D.ZERO;
        this.aabb = new AxisAlignedBoundingBox();
    }

    public ObstacleBox(@Nonnull Vector2D start, @Nonnull Vector2D end, double margin) {
        super(margin);
        this.min = new Vector2D(FastMath.min(start.getX(), end.getX()), FastMath.min(start.getY(), end.getY()));
        this.max = new Vector2D(FastMath.max(start.getX(), end.getX()), FastMath.max(start.getY(), end.getY()));
        this.minX = new Vector2D(FastMath.min(start.getX(), end.getX()), FastMath.max(start.getY(), end.getY()));
        this.minY = new Vector2D(FastMath.max(start.getX(), end.getX()), FastMath.min(start.getY(), end.getY()));
        this.aabb = new AxisAlignedBoundingBox(this.min, this.max, margin);
    }

    @Override
    public ObstacleBox clone() {
        return new ObstacleBox(this.min, this.max, this.margin);
    }

    @Override
    public AbstractObstacle expandMargin(double value) {
        return new ObstacleBox(this.min, this.max, this.margin + value);
    }

    @Override
    public List<Pair<Vector2D, Vector2D>> getSegments() {
        return List.of(new Pair<>(this.min, this.minX), new Pair<>(this.min, this.minY),
                new Pair<>(this.minX, this.max), new Pair<>(this.minY, this.max));
    }

    @Override
    public List<Vector2D> getVertexes() {
        return List.of(this.min, this.minX, this.minY, this.max);
    }

    @Override
    public boolean isInside(@Nonnull Vector2D vector2D) {
        return this.min.getX() <= vector2D.getX() && vector2D.getX() <= this.max.getX()
                && this.min.getY() <= vector2D.getY() && vector2D.getY() <= this.max.getY();
    }

    @Nonnull
    @Override
    public AxisAlignedBoundingBox getAABB() {
        return this.aabb;
    }

    @Override
    public AbstractObstacle deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        final JsonObject minJO = jsonObject.get("min").getAsJsonObject();
        final JsonObject maxJO = jsonObject.get("max").getAsJsonObject();
        return new ObstacleBox(new Vector2D(minJO.get("x").getAsDouble(), minJO.get("y").getAsDouble()),
                new Vector2D(maxJO.get("x").getAsDouble(), maxJO.get("y").getAsDouble()),
                jsonObject.get("margin").getAsDouble());
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        JsonObject minJO = new JsonObject();
        minJO.addProperty("x", this.min.getX());
        minJO.addProperty("y", this.min.getY());
        jsonObject.add("min", minJO);
        JsonObject maxJO = new JsonObject();
        maxJO.addProperty("x", this.max.getX());
        maxJO.addProperty("y", this.max.getY());
        jsonObject.add("max", maxJO);
        jsonObject.addProperty("margin", this.margin);
        return jsonObject;
    }
}
