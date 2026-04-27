package ai_server_cafe.model.field.obstacle;

import ai_server_cafe.model.field.obstacle.base.AbstractObstaclePolygon;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AxisAlignedBoundingBox;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.util.math.MathHelper;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ObstacleParallelogram extends AbstractObstaclePolygon {
    private final AxisAlignedBoundingBox aabb;
    private final Vector2D start;
    private final Vector2D side1;
    private final Vector2D side2;
    private final Vector2D end;

    public ObstacleParallelogram() {
        super(0.0);
        this.aabb = new AxisAlignedBoundingBox();
        this.start = Vector2D.ZERO;
        this.side1 = Vector2D.ZERO;
        this.side2 = Vector2D.ZERO;
        this.end = Vector2D.ZERO;
    }

    public ObstacleParallelogram(@Nonnull Vector2D start, @Nonnull Vector2D side1, @Nonnull Vector2D side2, double margin) {
        super(margin, true);
        this.end = start.add(side1).add(side2);
        this.start = start;
        this.side1 = side1;
        this.side2 = side2;
        this.aabb = new AxisAlignedBoundingBox(start, end, margin);
    }

    @Override
    public boolean isInside(@Nonnull Vector2D vector2D) {
        if (MathHelper.isEpsilon(this.start.subtract(vector2D).getNorm()) || MathHelper.isEpsilon(this.end.subtract(vector2D).getNorm())) {
            return true;
        }
        Vector2D vector2D1 = vector2D.subtract(this.start);
        Vector2D vector2D2 = this.end.subtract(vector2D);
        return MathHelper.inferiorAngle(vector2D1, this.side1) <= MathHelper.inferiorAngle(this.side1, this.side2)
                && MathHelper.inferiorAngle(vector2D1, this.side2) <= MathHelper.inferiorAngle(this.side1, this.side2)
                && MathHelper.inferiorAngle(vector2D2, this.side1) <= MathHelper.inferiorAngle(this.side1, this.side2)
                && MathHelper.inferiorAngle(vector2D2, this.side2) <= MathHelper.inferiorAngle(this.side1, this.side2);
    }

    @Nonnull
    @Override
    public AxisAlignedBoundingBox getAABB() {
        return this.aabb;
    }

    @Override
    public AbstractObstaclePolygon clone() {
        return new ObstacleParallelogram(this.start, this.side1, this.side2, this.margin);
    }

    @Override
    public AbstractObstaclePolygon expandMargin(double value) {
        return new ObstacleParallelogram(this.start, this.side1, this.side2, this.margin + value);
    }

    @Override
    public List<Pair<Vector2D, Vector2D>> getSegments() {
        return List.of(new Pair<>(this.start, this.start.add(this.side1)), new Pair<>(this.start, this.start.add(this.side2)),
                new Pair<>(this.end, this.start.add(this.side1)), new Pair<>(this.end, this.start.add(this.side2)));
    }

    @Override
    public List<Vector2D> getVertexes() {
        return List.of(this.start, this.start.add(this.side1), this.start.add(this.side2), this.end);
    }

    @Override
    public AbstractObstacle deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        final JsonObject startJO = jsonObject.get("start").getAsJsonObject();
        final JsonObject side1JO = jsonObject.get("side1").getAsJsonObject();
        final JsonObject side2JO = jsonObject.get("side2").getAsJsonObject();
        final double margin = jsonObject.get("margin").getAsDouble();
        return new ObstacleParallelogram(RecordData1.getVector2D(startJO),
                RecordData1.getVector2D(side1JO), RecordData1.getVector2D(side2JO), margin);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("start", RecordData1.convertVector2D(this.start));
        jsonObject.add("side1", RecordData1.convertVector2D(this.side1));
        jsonObject.add("side2", RecordData1.convertVector2D(this.side2));
        jsonObject.addProperty("margin", this.margin);
        return jsonObject;
    }
}
