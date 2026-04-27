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
import java.util.ArrayList;
import java.util.List;

public class ObstacleTriangle extends AbstractObstaclePolygon {
    private final Vector2D point1;
    private final Vector2D point2;
    private final Vector2D point3;
    private final AxisAlignedBoundingBox aabb;

    public ObstacleTriangle() {
        super(0.0);
        this.point1 = Vector2D.ZERO;
        this.point2 = Vector2D.ZERO;
        this.point3 = Vector2D.ZERO;
        this.aabb = new AxisAlignedBoundingBox();
    }

    public ObstacleTriangle(@Nonnull Vector2D point1, @Nonnull Vector2D point2, @Nonnull Vector2D point3, double margin) {
        super(margin);
        // pointが同じインスタンスを持たないように
        this.point1 = point1.scalarMultiply(1);
        this.point2 = point2.scalarMultiply(1);
        this.point3 = point3.scalarMultiply(1);
        this.aabb = new AxisAlignedBoundingBox(new Vector2D(MathHelper.min(this.point1.getX(), this.point2.getX(), this.point3.getX()),
                MathHelper.min(this.point1.getY(), this.point2.getY(), this.point3.getY())),
                new Vector2D(MathHelper.max(this.point1.getX(), this.point2.getX(), this.point3.getX()),
                        MathHelper.max(this.point1.getY(), this.point2.getY(), this.point3.getY())), margin);
    }

    @Override
    public boolean isInside(@Nonnull Vector2D vector2D) {
        List<Vector2D> vertexes = List.of(this.point1, this.point2, this.point3);
        for (Vector2D targetVer : vertexes) {
            List<Vector2D> others = new ArrayList<>(vertexes);
            others.remove(targetVer);
            Vector2D targetVec = vector2D.subtract(targetVer);
            double theta = MathHelper.inferiorAngle(others.get(0).subtract(targetVer), others.get(1).subtract(targetVer));
            boolean flag = MathHelper.inferiorAngle(targetVec, others.get(0).subtract(targetVer)) <= theta
                    && MathHelper.inferiorAngle(targetVec, others.get(1).subtract(targetVer)) <= theta;
            if (!flag) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ObstacleTriangle clone() {
        return new ObstacleTriangle(this.point1, this.point2, this.point3, this.margin);
    }

    @Override
    public ObstacleTriangle expandMargin(double value) {
        return new ObstacleTriangle(this.point1, this.point2, this.point3, value + this.margin);
    }

    @Override
    public List<Pair<Vector2D, Vector2D>> getSegments() {
        return List.of(new Pair<>(this.point1, this.point2), new Pair<>(this.point2, this.point3),
                new Pair<>(this.point3, this.point1));
    }

    @Override
    public List<Vector2D> getVertexes() {
        return List.of(this.point1, this.point2, this.point3);
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
        final JsonObject point1JO = jsonObject.get("point1").getAsJsonObject();
        final JsonObject point2JO = jsonObject.get("point2").getAsJsonObject();
        final JsonObject point3JO = jsonObject.get("point3").getAsJsonObject();
        return new ObstacleTriangle(RecordData1.getVector2D(point1JO),
                RecordData1.getVector2D(point2JO),
                RecordData1.getVector2D(point3JO),
                jsonObject.get("margin").getAsDouble());
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("point1", RecordData1.convertVector2D(this.point1));
        jsonObject.add("point2", RecordData1.convertVector2D(this.point2));
        jsonObject.add("point3", RecordData1.convertVector2D(this.point3));
        jsonObject.addProperty("margin", this.margin);
        return jsonObject;
    }
}
