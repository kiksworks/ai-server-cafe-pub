package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.awt.*;
import java.util.Map;

public class PassScoreCafe extends AbstractGraphicalComponent {
    Map<Vector2D, Double> scoreMap;
    private boolean invert;

    public PassScoreCafe(Color color, Map<Vector2D, Double> map, boolean isInvert) {
        super(color);
        this.scoreMap = map;
        this.invert = isInvert;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        double minScore = this.scoreMap.values().stream().min(InterfaceHelper.getComparator(new IFuncParam1<Double, Double>() {
            @Override
            public Double function(Double aDouble) {
                return aDouble;
            }
        })).orElse(0.0);

        double maxScore = this.scoreMap.values().stream().max(InterfaceHelper.getComparator(new IFuncParam1<Double, Double>() {
            @Override
            public Double function(Double aDouble) {
                return aDouble;
            }
        })).orElse(100.0);

        for (Map.Entry<Vector2D, Double> entry : this.scoreMap.entrySet()) {
            Vector2D position = entry.getKey().scalarMultiply(this.invert ? -1.0 : 1.0);
            double score = entry.getValue();
            int radius = (int) (ConfigManager.getInstance().getConfig().robotRadius * 5 * (score - minScore) / (maxScore - minScore));

            graphics.setColor(this.color);
            graphics.fillOval((int)(position.getX() - radius), (int)(position.getY() - radius), radius * 2, radius * 2);
        }
    }
}