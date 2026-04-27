package ai_server_cafe.model.field.obstacle.interfaces;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface ICircle {
    Vector2D getCenter();

    double getRadius();
}
