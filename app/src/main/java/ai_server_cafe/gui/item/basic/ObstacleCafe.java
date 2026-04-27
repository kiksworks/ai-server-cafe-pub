package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.field.obstacle.interfaces.ICircle;
import ai_server_cafe.model.field.obstacle.interfaces.IPolygon;
import ai_server_cafe.model.field.obstacle.interfaces.ISegment;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ObstacleCafe extends AbstractGraphicalComponent {
    private final List<AbstractObstacle> obstacles;
    private final double invert;

    public ObstacleCafe(Color color, List<AbstractObstacle> obstacles, boolean invertView) {
        super(color);
        this.obstacles = new ArrayList<>(obstacles);
        this.invert = invertView ? -1 : 1;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        for (AbstractObstacle ao : this.obstacles) {
            double margin = ao.getMargin();
            if (ao instanceof ICircle) {
                Vector2D center = ((ICircle) ao).getCenter().scalarMultiply(invert);
                double radius = ((ICircle) ao).getRadius() + margin;
                graphics.fillOval((int)(center.getX() - radius), (int)(center.getY() - radius), (int)(2.0 * radius), (int)(2.0 * radius));
            } else if (ao instanceof ISegment) {
                List<Vector2D> vertexes = ((ISegment) ao).getVertexes();
                Vector2D point1 = vertexes.getFirst().scalarMultiply(invert);
                Vector2D point2 = vertexes.getLast().scalarMultiply(invert);
                Vector2D marginVector = MathHelper.normalized(MathHelper.applyRotation2D(point1.subtract(point2), MathHelper.HALF_PI)).scalarMultiply(margin);
                int[] ax = new int[4];
                int[] ay = new int[4];
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        Vector2D point = vertexes.get(i).scalarMultiply(invert).add(marginVector.scalarMultiply(2 * j - 1).scalarMultiply(2 * i - 1));
                        ax[2 * i + j] = (int)point.getX();
                        ay[2 * i + j] = (int)point.getY();
                    }
                }
                graphics.fillPolygon(ax, ay, 4);
                Vector2D arcVector = new Vector2D(marginVector.getX(), -marginVector.getY());
                graphics.fillArc((int)(point1.getX() - margin), (int)(point1.getY() - margin), (int)(2.0 * margin), (int)(2.0 * margin), (int)Math.toDegrees(MathHelper.direction(arcVector)), 180);
                graphics.fillArc((int)(point2.getX() - margin), (int)(point2.getY() - margin), (int)(2.0 * margin), (int)(2.0 * margin), (int)Math.toDegrees(MathHelper.direction(arcVector.negate())), 180);
            } else if (ao instanceof IPolygon) {
                AbstractObstacle exAo = ao.expandMargin(ao.getMargin());
                List<Vector2D> vertexes = new ArrayList<>(((IPolygon) exAo).getVertexes());
                List<Pair<Vector2D, Vector2D>> segments = ((IPolygon) exAo).getSegments();
                List<Vector2D> newVertexes = new ArrayList<>();
                while (!vertexes.isEmpty()) {
                    if (newVertexes.isEmpty()) {
                        newVertexes.add(vertexes.getFirst());
                        vertexes.removeFirst();
                        continue;
                    }
                    Vector2D lastVertex = newVertexes.getLast();
                    for (Pair<Vector2D, Vector2D> segment : segments) {
                        if (segment.getFirst() == lastVertex && !newVertexes.contains(segment.getSecond())) {
                            newVertexes.add(segment.getSecond());
                            vertexes.remove(segment.getSecond());
                            break;
                        }
                        if (segment.getSecond() == lastVertex && !newVertexes.contains(segment.getFirst())) {
                            newVertexes.add(segment.getFirst());
                            vertexes.remove(segment.getFirst());
                            break;
                        }
                    }
                }

                int[] ax = new int[newVertexes.size()];
                int[] ay = new int[newVertexes.size()];
                for (int i = 0; i < newVertexes.size(); i++) {
                    Vector2D vertex = newVertexes.get(i);
                    vertex = vertex.scalarMultiply(invert);
                    ax[i] = (int)vertex.getX();
                    ay[i] = (int)vertex.getY();
                }
                graphics.fillPolygon(ax, ay, newVertexes.size());
            }
        }
    }
}
