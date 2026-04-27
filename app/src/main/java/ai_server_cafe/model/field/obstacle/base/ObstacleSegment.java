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
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ObstacleSegment extends AbstractObstacle implements ISegment {
    protected Vector2D start;
    protected Vector2D end;
    protected AxisAlignedBoundingBox aabb;

    public ObstacleSegment() {
        super(0.0);
        this.start = Vector2D.ZERO;
        this.end = Vector2D.ZERO;
        this.aabb = new AxisAlignedBoundingBox();
    }

    public ObstacleSegment(Vector2D start, Vector2D end, double margin) {
        super(margin);
        this.start = start;
        this.end = end;
        this.aabb = new AxisAlignedBoundingBox(start, end, margin);
    }

    @Override
    public Pair<Vector2D, Vector2D> getSegment() {
        return new Pair<>(this.start, this.end);
    }

    public List<Vector2D> getVertexes() {
        return List.of(this.start, this.end);
    }

    @Override
    public boolean isCollidedDetail(Vector2D vector2D, double additionalMargin) {
        Optional<Vector2D> optPip = getPerpendicularIntersectionPoint(this.start, this.end, vector2D);
        if (optPip.isPresent()) {
            return optPip.get().subtract(vector2D).getNorm() <= this.getMargin() + additionalMargin;
        }
        return this.start.subtract(vector2D).getNorm() <= this.getMargin() + additionalMargin
                || this.end.subtract(vector2D).getNorm() <= this.getMargin() + additionalMargin;
    }

    public static Optional<Vector2D> getPerpendicularIntersectionPoint(Vector2D start, @Nonnull Vector2D end, Vector2D target) {
        if (end.subtract(start).getNormSq() < MathHelper.EPSILON) {
            return Optional.empty();
        }
        Vector2D p = MathHelper.normalized(end.subtract(start));
        Vector2D result = p.scalarMultiply(target.subtract(start).dotProduct(p)).add(start);
        // 外側にあるときはempty
        if (end.subtract(start).getNormSq() < start.subtract(result).getNormSq() || end.subtract(start).getNormSq() < end.subtract(result).getNormSq())
            return Optional.empty();
        return Optional.of(result);
    }

    public static boolean hasPip(Vector2D start, @Nonnull Vector2D end, Vector2D target) {
        return getPerpendicularIntersectionPoint(start, end, target).isPresent();
    }

    @Override
    public boolean isCollidedDetail(AbstractObstacle obstacle, double step) {
        if (obstacle instanceof ICircle) {
            return obstacle.isCollidedDetail(this, step);
        } else if (obstacle instanceof ISegment || obstacle instanceof IPolygon) {
            Vector2D start = this.getSegment().getFirst();
            Vector2D end = this.getSegment().getSecond();
            double r = end.subtract(start).getNorm();
            if (obstacle instanceof ISegment) {
                Vector2D start1 = ((ISegment)obstacle).getSegment().getFirst();
                Vector2D end1 = ((ISegment)obstacle).getSegment().getSecond();
                if (r > start1.subtract(end1).getNorm()) {
                    return obstacle.isCollidedDetail(this, step);
                }
            }
            if (r < MathHelper.EPSILON) {
                return obstacle.isCollided(start, this.getMargin());
            }
            for (double l = 0; l <= r; l += step) {
                if (obstacle.isCollided(start.add(MathHelper.normalized(end.subtract(start)).scalarMultiply(l)), this.getMargin())) {
                    return true;
                }
            }
            return false;
        } else {
            throw new IllegalArgumentException("Unknown obstacle type.");
        }
    }

    @Override
    public List<Vector2D> getTangentPoints(Vector2D vector2D) {
        List<Vector2D> result = new ArrayList<>();
        if (this.isCollided(vector2D)) {
            return result;
        }
        for (Vector2D startTp : ObstacleCircle.getTangentPoints(this.start, this.getMargin(), vector2D)) {
            if (!hasPip(this.start, this.end, startTp)) {
                result.add(startTp);
            }
        }
        for (Vector2D endTp : ObstacleCircle.getTangentPoints(this.end, this.getMargin(), vector2D)) {
            if (!hasPip(this.start, this.end, endTp)) {
                result.add(endTp);
            }
        }
        return result;
    }

    @Override
    @Nonnull
    public AxisAlignedBoundingBox getAABB() {
        return this.aabb;
    }

    @Override
    public ObstacleSegment clone() {
        return new ObstacleSegment(this.start, this.end, this.margin);
    }

    @Override
    public String toString() {
        return this.customName.isEmpty() ? "Segment:[start:" + this.start + ", end:" + this.end + "]" : this.customName;
    }

    @Override
    public AbstractObstacle expandMargin(double value) {
        return new ObstacleSegment(this.start, this.end, this.margin + value);
    }

    @Override
    public AbstractObstacle deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        final JsonObject startJO = jsonObject.get("start").getAsJsonObject();
        final JsonObject endJO = jsonObject.get("end").getAsJsonObject();
        return new ObstacleSegment(RecordData1.getVector2D(startJO), RecordData1.getVector2D(endJO),
                jsonObject.get("margin").getAsDouble());
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("start", RecordData1.convertVector2D(this.start));
        jsonObject.add("end", RecordData1.convertVector2D(this.end));
        jsonObject.addProperty("margin", this.margin);
        return jsonObject;
    }
}
