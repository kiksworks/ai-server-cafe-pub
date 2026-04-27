package ai_server_cafe.model.field.obstacle;

import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.AxisAlignedBoundingBox;
import ai_server_cafe.util.interfaces.SerializableJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class IntegratedObstacle extends AbstractObstacle {
    private final List<AbstractObstacle> obstacles;
    private AxisAlignedBoundingBox aabb;

    public IntegratedObstacle() {
        super(0.0);
        this.obstacles = new ArrayList<>();
        this.aabb = new AxisAlignedBoundingBox();
    }

    public IntegratedObstacle(@Nonnull AbstractObstacle init) {
        super(0.0);
        this.obstacles = new ArrayList<>();
        this.obstacles.add(init);
        this.aabb = init.getAABB();
    }

    private IntegratedObstacle(List<AbstractObstacle> obstacles, AxisAlignedBoundingBox aabb) {
        super(0.0);
        this.obstacles = new ArrayList<>(obstacles);
        this.aabb = aabb;
    }

    @Override
    public boolean isCollidedDetail(Vector2D vector2D, double additionalMargin) {
        for (AbstractObstacle ao : this.obstacles) {
            if (ao.isCollided(vector2D, additionalMargin)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCollidedDetail(AbstractObstacle obstacle, double step) {
        for (AbstractObstacle ao : this.obstacles) {
            if (ao.isCollided(obstacle, step)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nonnull
    public List<Vector2D> getTangentPoints(Vector2D vector2D) {
        List<Vector2D> result = new ArrayList<>();
        for (AbstractObstacle ao : this.obstacles) {
            result.addAll(ao.getTangentPoints(vector2D));
        }
        return result;
    }

    @Override
    @Nonnull
    public AxisAlignedBoundingBox getAABB() {
        return this.aabb;
    }

    @Override
    public double getMargin() {
        return 0.0;
    }

    @Override
    @Nonnull
    public IntegratedObstacle clone() {
        return new IntegratedObstacle(this.obstacles, this.aabb);
    }

    @Override
    @Nonnull
    public String toString() {
        return "IntegratedObstacles:" + this.obstacles;
    }

    @Nonnull
    @Override
    public AbstractObstacle expandMargin(double value) {
        return this.clone();
    }

    @Nonnull
    public List<AbstractObstacle> getObstacles() {
        return new ArrayList<>(this.obstacles);
    }

    public void add(AbstractObstacle obstacle) {
        this.obstacles.add(obstacle);
        this.aabb = this.aabb.add(obstacle.getAABB());
    }

    @Override
    public AbstractObstacle deserialize(JsonObject jsonObject) {
        final JsonArray jsonArray = jsonObject.get("obstacles").getAsJsonArray();
        final IntegratedObstacle init = new IntegratedObstacle();
        for (JsonElement element : jsonArray.asList()) {
            init.add(SerializableJson.deserializeJson(element.getAsJsonObject()));
        }
        return init;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for(AbstractObstacle obstacle : this.obstacles) {
            jsonArray.add(SerializableJson.serializeJson(obstacle));
        }
        jsonObject.add("obstacles", jsonArray);
        return jsonObject;
    }
}
