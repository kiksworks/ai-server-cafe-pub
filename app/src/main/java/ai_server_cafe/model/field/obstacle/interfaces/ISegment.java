package ai_server_cafe.model.field.obstacle.interfaces;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.List;

public interface ISegment {
    Pair<Vector2D, Vector2D> getSegment();

    List<Vector2D> getVertexes();
}
