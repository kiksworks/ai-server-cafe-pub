package ai_server_cafe.game.planner.path;

import ai_server_cafe.util.interfaces.AbstractCloneable;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class PathSide extends AbstractCloneable {
    public final Vector2D side;
    public final Vector2D additionalVec;
    public PathSide(Vector2D side, Vector2D additionalVec) {
        this.side = side;
        this.additionalVec = additionalVec;
    }

    @Override
    public PathSide clone() {
        return new PathSide(this.side, this.additionalVec);
    }

    public String toString() {
        return "[" + side + ", " + additionalVec + "]";
    }
}
