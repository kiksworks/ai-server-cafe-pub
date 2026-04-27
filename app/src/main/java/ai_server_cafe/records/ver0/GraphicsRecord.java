package ai_server_cafe.records.ver0;

import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.lang.ArrayUtils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Cafeでよく使用するGraphics2Dを保存するクラス
 * @see RecordData0.TypeData
 */
public class GraphicsRecord extends Graphics2D {
    private final String layerName;
    private Color color;
    private AffineTransform transform;
    private final RecordData0 recordData0;
    private Font font;

    public GraphicsRecord(String layerName, RecordData0 recordData0) {
        this.layerName = layerName;
        this.recordData0 = recordData0;
        this.color = ColorHelper.LINE_WHITE;
        this.transform = new AffineTransform();
        this.font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    }

    @Override
    public void setColor(Color c) {
        this.color = c;
    }

    @Override
    public void drawString(String str, int x, int y) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawString";
        layer0.type.str = str;
        layer0.type.ax = new int[]{x};
        layer0.type.ay = new int[]{y};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        layer0.type.fontName = font.getFontName();
        layer0.type.fontType = font.getStyle();
        layer0.type.fontSize = font.getSize();
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawString(String str, float x, float y) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawString";
        layer0.type.str = str;
        layer0.type.ad = new double[]{x, y};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        layer0.type.fontName = font.getFontName();
        layer0.type.fontType = font.getStyle();
        layer0.type.fontSize = font.getSize();
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void translate(int x, int y) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "translate";
        layer0.type.ax = new int[]{x};
        layer0.type.ay = new int[]{y};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void translate(double tx, double ty) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "translate";
        layer0.type.ad = new double[]{tx, ty};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "clipRect";
        layer0.type.ax = new int[] {x, width};
        layer0.type.ay = new int[]{y, height};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawLine";
        layer0.type.ax = new int[] {x1, x2};
        layer0.type.ay = new int[]{y1, y2};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "fillRect";
        layer0.type.ax = new int[] {x, width};
        layer0.type.ay = new int[]{y, height};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "fillOval";
        layer0.type.ax = new int[] {x, width};
        layer0.type.ay = new int[]{y, height};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawArc";
        layer0.type.ax = new int[] {x, width, startAngle};
        layer0.type.ay = new int[]{y, height, arcAngle};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "fillArc";
        layer0.type.ax = new int[] {x, width, startAngle};
        layer0.type.ay = new int[]{y, height, arcAngle};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawPolyline";
        layer0.type.ax = xPoints;
        layer0.type.ay = yPoints;
        layer0.type.nPoints = nPoints;
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawPolygon";
        layer0.type.ax = xPoints;
        layer0.type.ay = yPoints;
        layer0.type.nPoints = nPoints;
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "fillPolygon";
        layer0.type.ax = xPoints;
        layer0.type.ay = yPoints;
        layer0.type.nPoints = nPoints;
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void rotate(double theta) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "rotate";
        layer0.type.ad = new double[]{0.0, 0.0, theta};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "rotate";
        layer0.type.ad = new double[]{x, y, theta};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void scale(double sx, double sy) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "scale";
        layer0.type.ad = new double[]{sx, sy};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void shear(double shx, double shy) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "shear";
        layer0.type.ad = new double[]{shx, shy};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "setClip";
        layer0.type.ax = new int[]{x, width};
        layer0.type.ay = new int[]{y, height};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "copyArea";
        layer0.type.ax = new int[]{x, width, dx};
        layer0.type.ay = new int[]{y, height, dy};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "clearRect";
        layer0.type.ax = new int[]{x, width};
        layer0.type.ay = new int[]{y, height};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawRoundRect";
        layer0.type.ax = new int[]{x, width, arcWidth};
        layer0.type.ay = new int[]{y, height, arcHeight};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "fillRoundRect";
        layer0.type.ax = new int[]{x, width, arcWidth};
        layer0.type.ay = new int[]{y, height, arcHeight};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        RecordData0.LayerData0 layer0 = new RecordData0.LayerData0();
        layer0.name = this.layerName;
        layer0.color = MathHelper.getColorId(this.color);
        layer0.type.type = "drawOval";
        layer0.type.ax = new int[]{x, width};
        layer0.type.ay = new int[]{y, height};
        double[] matrix = new double[6];
        this.transform.getMatrix(matrix);
        layer0.type.affineMatrix = matrix;
        this.recordData0.layerData0 = (RecordData0.LayerData0[]) ArrayUtils.add(this.recordData0.layerData0, layer0);

    }

    @Override
    public void draw(Shape s) {}

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return false;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {}

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {}

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {}

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {}

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public void dispose() {}

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {}

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {}

    @Override
    public void fill(Shape s) {}

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return false;
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return null;
    }

    @Override
    public void setComposite(Composite comp) {}

    @Override
    public void setPaint(Paint paint) {}

    @Override
    public void setStroke(Stroke s) {}

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {}

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return null;
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {}

    @Override
    public void addRenderingHints(Map<?, ?> hints) {}

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    @Override
    public Graphics create() {
        return null;
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public void setPaintMode() {}

    @Override
    public void setXORMode(Color c1) {}

    @Override
    public Font getFont() {
        return null;
    }

    @Override
    public void setFont(Font font) {
        this.font = font;
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return null;
    }

    @Override
    public Rectangle getClipBounds() {
        return null;
    }

    @Override
    public Shape getClip() {
        return null;
    }

    @Override
    public void setClip(Shape clip) {}

    @Override
    public void transform(AffineTransform Tx) {}

    @Override
    public void setTransform(AffineTransform Tx) {
        this.transform = Tx;
    }

    @Override
    public AffineTransform getTransform() {
        return this.transform;
    }

    @Override
    public Paint getPaint() {
        return null;
    }

    @Override
    public Composite getComposite() {
        return null;
    }

    @Override
    public void setBackground(Color color) {}

    @Override
    public Color getBackground() {
        return null;
    }

    @Override
    public Stroke getStroke() {
        return null;
    }

    @Override
    public void clip(Shape s) {}

    @Override
    public FontRenderContext getFontRenderContext() {
        return null;
    }
}
