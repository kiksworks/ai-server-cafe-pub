package ai_server_cafe.gui.item.basic;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PathCafe extends AbstractGraphicalComponent {
    private final List<Vector2D> path;
    private final Vector2D start;
    private final float stroke;
    private final boolean invert;
    private final Vector2D vel;
    public PathCafe(Color color, @Nonnull Optional<List<Vector2D>> path, Vector2D start, float stroke, boolean isInvert, Vector2D vel) {
        super(color);
        this.path = path.isPresent() ? path.get() : new ArrayList<>();
        this.start = start;
        this.stroke = stroke;
        this.invert = isInvert;
        this.vel = vel == null ? Vector2D.ZERO : vel;
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        Config.PathPlannerConfig.PlannerVisibility config = ConfigManager.getInstance().getConfig().pathPlannerConfig.plannerVisibility;
        // 点線の間隔
        float dash1[] = {this.stroke, this.stroke * 3};
        // 点線
        BasicStroke dashStroke = new BasicStroke(this.stroke,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                3.0f,
                dash1,
                0.0f);
        graphics.setStroke(config.dashView ? dashStroke : new BasicStroke(this.stroke));
        Vector2D segmentStart = this.start;
        for (Vector2D end : this.path) {
            Vector2D segmentEnd = end.scalarMultiply(this.invert ? -1.0 : 1.0).add(segmentStart);
            graphics.drawLine((int) segmentEnd.getX(), (int) segmentEnd.getY(), (int) segmentStart.getX(), (int) segmentStart.getY());
            segmentStart = segmentEnd;
        }
        if (config.temporaryDestinationView) {
            graphics.setColor(ColorHelper.LATTE_BROWN);
            graphics.setStroke(new BasicStroke(20.0F));
            Vector2D tempVel = this.vel.scalarMultiply(this.invert ? -1.0 : 1.0);
            graphics.drawLine((int)start.getX(), (int)start.getY(), (int)(start.getX() + tempVel.getX()), (int)(start.getY() + tempVel.getY()));
        }
    }
}
