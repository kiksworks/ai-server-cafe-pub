package ai_server_cafe.model.field.obstacle.dynamic;

import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class DynamicObstacleCircle extends ObstacleSegment implements IDynamic {
    private final Vector2D pos;
    private final Vector2D vel;
    private double dt;

    public DynamicObstacleCircle() {
        super();
        this.pos = Vector2D.ZERO;
        this.vel = Vector2D.ZERO;
        this.dt = 0.0;
    }

    public DynamicObstacleCircle(Vector2D pos,
                                 Vector2D vel, double radius, double margin) {
        super(pos, pos.scalarMultiply(1.0), margin + radius);
        this.dt = 0.0;
        this.pos = pos;
        this.vel = vel;
    }

    public DynamicObstacleCircle setDt(double dt) {
        this.dt = dt;
        this.end = this.start.add(this.vel.scalarMultiply(dt));
        return this;
    }

    @Override
    @SuppressWarnings("unused")
    public DynamicObstacleCircle clone() {
        return new DynamicObstacleCircle(this.pos, this.vel, 0.0, this.margin).setDt(this.dt);
    }

    public String toString() {
        return this.customName.isEmpty() ? "dynamicCircle" : this.customName;
    }

    @Override
    public double getDt() {
        return this.dt;
    }
}
