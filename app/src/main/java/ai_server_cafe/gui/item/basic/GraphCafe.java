package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.Vector2I;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphCafe extends AbstractGraphicalComponent {
    private final double width;
    private final double height;
    private final double x;
    private final double y;
    private final Map<Property, Double> propMap;
    private final Map<String, Pair<Color, List<Vector2D>>> plotMap;
    private double minX;
    private static final int margin = 12;
    private static final int legendMinX = 40;
    private static final int legendOffsetY = 21;
    private static final int legendLen = 20;
    public int count;
    public int legendSize;

    public GraphCafe(double x, double y, double width, double height, EnumDisplayType xType,
                     EnumDisplayType yType, EnumDisplayType widthType,
                     EnumDisplayType heightType) {
        super(ColorHelper.LINE_WHITE, xType, yType, widthType, heightType);
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.propMap = new HashMap<>();
        this.plotMap = new LinkedHashMap<>();
        this.propMap.put(Property.Y_MIN, 0.0D);
        this.propMap.put(Property.Y_MAX, 20.0D);
        this.propMap.put(Property.Y_SCALE_WIDTH, 5.0D);
        this.propMap.put(Property.Y_SUBSCALE_WIDTH, 1.0D);
        this.propMap.put(Property.X_SCALE_WIDTH, 5.0D);
        this.propMap.put(Property.X_SUBSCALE_WIDTH, 1.0D);
        this.propMap.put(Property.X_WIDTH, 25.0D);
        this.propMap.put(Property.X_MAX_WIDTH, 30.0D);
        this.minX = 0.0;
        this.count = 0;
        this.legendSize = 0;
    }

    public GraphCafe setProperty(Property prop, double value) {
        synchronized (this.propMap) {
            this.propMap.put(prop, value);
        }
        return this;
    }

    public GraphCafe addPlot(String name, @Nonnull Vector2D point) {
        synchronized (this.plotMap) {
            double min = this.minX;
            if (!this.plotMap.containsKey(name)) {
                return this;
            }
            if (this.minX + this.propMap.get(Property.X_WIDTH) < point.getX()) {
                this.minX = point.getX() - this.propMap.get(Property.X_WIDTH);
            }
            if (point.getX() < this.minX) {
                this.minX = point.getX();
            }
            this.plotMap.get(name).getSecond().add(point);
            if (min != this.minX) {
                final double maxX = this.minX + this.propMap.get(Property.X_WIDTH);
                for (String key : this.plotMap.keySet()) {
                    this.plotMap.get(key).getSecond().removeIf(InterfaceHelper.getPredicate(
                            new IFuncParam1<Boolean, Vector2D>() {
                                @Override
                                public Boolean function(Vector2D vector2D) {
                                    return vector2D.getX() < minX || maxX < vector2D.getX();
                                }
                            }));
                }
            }
        }
        return this;
    }

    public GraphCafe addPlot(String name, double x, double y) {
        this.addPlot(name, new Vector2D(x, y));
        return this;
    }

    public GraphCafe addPlotAll(String name, @Nonnull List<Vector2D> list) {
        for (Vector2D point : list) {
            this.addPlot(name, point);
        }
        return this;
    }

    public GraphCafe newGraphForce(String name, Color color) {
        synchronized (this.plotMap) {
            this.plotMap.put(name, new Pair<>(color, new ArrayList<>()));
        }
        return this;
    }

    public GraphCafe newGraph(String name, Color color) {
        synchronized (this.plotMap) {
            if (!this.plotMap.containsKey(name)) {
                this.plotMap.put(name, new Pair<>(color, new ArrayList<>()));
            }
        }
        return this;
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        Rectangle area = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(this.paneWidth, this.paneHeight));
        double graphWidth;
        double yMin;
        double yMax;
        double graphHeight;
        double scaleX;
        double subscaleX;
        double scaleY;
        double subscaleY;
        synchronized (this.propMap) {
            graphWidth = this.propMap.get(Property.X_MAX_WIDTH);
            yMin = this.propMap.get(Property.Y_MIN);
            yMax = this.propMap.get(Property.Y_MAX);
            graphHeight = yMax - yMin;
            scaleX = this.propMap.get(Property.X_SCALE_WIDTH);
            scaleY = this.propMap.get(Property.Y_SCALE_WIDTH);
            subscaleX = this.propMap.get(Property.X_SUBSCALE_WIDTH);
            subscaleY = this.propMap.get(Property.Y_SUBSCALE_WIDTH);
        }
        Map<String, Pair<Color, List<Vector2D>>> copied = new LinkedHashMap<>();
        synchronized (this.plotMap) {
            for (String key : this.plotMap.keySet()) {
                copied.put(key, new Pair<>(this.plotMap.get(key).getFirst(), new ArrayList<>(this.plotMap.get(key).getSecond())));
            }
        }
            int plotAreaMinX = area.x + 3 * margin;
            int plotAreaMaxY = area.y + area.height - this.legendSize;
            int plotAreaMaxX = area.x + area.width - margin;
            int plotAreaMinY = area.y + margin;
            int plotWidth = plotAreaMaxX - plotAreaMinX - 1;
            int plotHeight = plotAreaMaxY - plotAreaMinY - 1;
            // 目盛線など
            graphics.setColor(ColorHelper.SCREEN_DARK);
            graphics.drawRect(plotAreaMinX, plotAreaMinY, plotWidth, plotHeight);
            int lineNumberX = (int)(graphWidth / scaleX);
            int lastLineX = (int)((this.minX + graphWidth) / scaleX);
            int lineNumberY = (int)(graphHeight / scaleY);
            int lastLineY = (int)((yMin + graphHeight) / scaleY);
            for (int i = 0; i <= lineNumberX; i++) {
                int scaleXi = scaled((lastLineX - i) * scaleX, plotAreaMinX, plotWidth, this.minX, graphWidth, false);
                if (scaleXi < plotAreaMinX || plotAreaMaxX < scaleXi)
                    continue;
                graphics.drawLine(scaleXi, plotAreaMinY, scaleXi, plotAreaMaxY - 1);
                String s = String.format("%.1f", (lastLineX - i) * scaleX);
                graphics.drawString(s, scaleXi - graphics.getFontMetrics().stringWidth(s) / 2, plotAreaMaxY + graphics.getFontMetrics().getHeight() - 3);
            }

            for (int i = 0; i <= lineNumberY; i++) {
                int scaleYi = scaled((lastLineY - i) * scaleY, plotAreaMinY, plotHeight, yMin, graphHeight, true);
                if (scaleYi < plotAreaMinY || plotAreaMaxY < scaleYi)
                    continue;
                graphics.drawLine(plotAreaMinX, scaleYi, plotAreaMaxX - 1, scaleYi);
                String s = String.format("%.1f", (lastLineY - i) * scaleY);
                graphics.drawString(s, plotAreaMinX - graphics.getFontMetrics().stringWidth(s) - 1, scaleYi + graphics.getFontMetrics().getHeight() / 2 - 3);
            }

            int legendX = legendMinX;
            int legendY = plotAreaMaxY + legendOffsetY;
            // グラフ

            graphics.setStroke(new BasicStroke(2.0F));
        this.legendSize = 0;
        if (!copied.isEmpty()) {
            this.legendSize = 2 * (graphics.getFontMetrics().getHeight() + 2);
        }
            for (String key : copied.keySet()) {
                Color color = copied.get(key).getFirst();
                int needLength = legendLen + 2 + graphics.getFontMetrics().stringWidth(key);
                if (needLength + legendX > this.paneWidth) {
                    legendX = legendMinX;
                    legendY += graphics.getFontMetrics().getHeight() + 2;
                    this.legendSize += graphics.getFontMetrics().getHeight() + 2;
                }
                graphics.setColor(color);
                graphics.drawLine(legendX, legendY, legendX + legendLen, legendY);
                legendX += legendLen + 2;
                graphics.setColor(ColorHelper.SCREEN_DARK);
                graphics.drawString(key, legendX, legendY + graphics.getFontMetrics().getHeight() / 3);
                legendX += graphics.getFontMetrics().stringWidth(key) + 10;
                graphics.setColor(color);

                Vector2D lastPoint = null;
                for (Vector2D vector2D : copied.get(key).getSecond()) {
                    if (lastPoint != null) {
                        Vector2D p1 = lastPoint;
                        Vector2D p2 = vector2D;
                        if (isOutside(lastPoint, yMin, graphHeight) && isOutside(vector2D, yMin, graphHeight)) {
                            lastPoint = vector2D;
                            continue;
                        } else if (isOutside(lastPoint, yMin, graphHeight)) {
                            double dx = vector2D.getX() - lastPoint.getX();
                            double dy = vector2D.getY() - lastPoint.getY();
                            double x = FastMath.max((yMin - lastPoint.getY()), (lastPoint.getY() - graphHeight - yMin)) * FastMath.abs(dx / dy);
                            p1 = new Vector2D(lastPoint.getX() + x, lastPoint.getY() + x * dy / dx);
                        } else if (isOutside(vector2D, yMin, graphHeight)) {
                            double dx = vector2D.getX() - lastPoint.getX();
                            double dy = vector2D.getY() - lastPoint.getY();
                            double x = FastMath.max((yMin - vector2D.getY()), (vector2D.getY() - graphHeight - yMin)) * FastMath.abs(dx / dy);
                            p2 = new Vector2D(vector2D.getX() - x, vector2D.getY() - x * dy / dx);
                        }
                        Vector2I lastPos = scaled(p1, plotAreaMinX, plotAreaMinY, plotWidth, plotHeight, this.minX, yMin, graphWidth, graphHeight);
                        Vector2I newPos = scaled(p2, plotAreaMinX, plotAreaMinY, plotWidth, plotHeight, this.minX, yMin, graphWidth, graphHeight);
                        graphics.drawLine(lastPos.getX(), lastPos.getY(), newPos.getX(), newPos.getY());
                    }
                    lastPoint = vector2D;
                }
            }
    }

    private static boolean isOutside(@Nonnull Vector2D point, double y, double height) {
        return point.getY() < y || y + height < point.getY();
    }

    @Nonnull
    private static Vector2I scaled(@Nonnull Vector2D point, int x, int y, int width, int height, double xD, double yD, double widthD, double heightD) {
        int relativeX = x + (int)((point.getX() - xD) * width / widthD);
        int relativeY = y + height - (int)((point.getY() - yD) * height / heightD);
        return new Vector2I(relativeX, relativeY);
    }

    private static int scaled(double value, int min, int length, double minD, double lengthD, boolean isY) {
        return min + (isY ? length - (int)((value - minD) * length / lengthD) : (int)((value - minD) * length / lengthD));
    }

    public enum Property {
        Y_MIN,
        Y_MAX,
        Y_SCALE_WIDTH,
        Y_SUBSCALE_WIDTH,
        X_MAX_WIDTH,
        X_SCALE_WIDTH,
        X_SUBSCALE_WIDTH,
        X_WIDTH
    }
}
