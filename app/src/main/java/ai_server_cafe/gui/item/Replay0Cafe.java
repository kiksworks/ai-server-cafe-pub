package ai_server_cafe.gui.item;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.records.ver0.RecordData0;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * RecordData0からGraphics2Dを復元するクラス
 */
public class Replay0Cafe extends AbstractGraphicalComponent {
    private final RecordData0.LayerData0 layer;
    public Replay0Cafe(@Nonnull RecordData0.LayerData0 layer) {
        super(MathHelper.getColor(layer.color));
        this.layer = layer;
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(20.0F));
        RecordData0.TypeData type = this.layer.type;
        try {
            if (type.fontSize != 0) {
                graphics.setFont(new Font(type.fontName, type.fontType, type.fontSize));
            }
            if (type.affineMatrix.length != 0) {
                graphics.transform(new AffineTransform(type.affineMatrix));
            }
            // must write SuperClass
            for (Method m : graphics.getClass().getSuperclass().getMethods()) {
                try {
                    if (m.getName().equals(type.type)) {
                        // invokeを用いてRecordData0から使用したGraphics2Dを復元する
                        m.invoke(graphics, getParameters(type));
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    // DO NOTHING
                }
            }
            if (type.affineMatrix.length != 0) {
                graphics.transform(new AffineTransform(type.affineMatrix).createInverse());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoninvertibleTransformException e) {
            // DO NOTHING
        }
    }

    /**
     * @implNote invokeを用いるときはメソッドで定義された引数の順序に注意する
     * Ex:Graphics2DのdrawStringは引数がString, int, intの順序で定義されているので，その順番で復元する
     * @return invokeするのに必要な引数の配列を生成する
     */
    @Nonnull
    private Object[] getParameters(@Nonnull RecordData0.TypeData type) {
        Object[] result = {};
        // axとayのサイズチェック
        if(type.ax.length != type.ay.length) return result;
        if(StringUtils.isNotEmpty(type.str)) result = ArrayUtils.add(result, type.str);
        if(type.nPoints > 0) {
            result = ArrayUtils.add(result, type.ax);
            result = ArrayUtils.add(result, type.ay);
            result = ArrayUtils.add(result, type.nPoints);
            return result;
        }
        for(int i = 0;i < type.ax.length; i++) {
            result = ArrayUtils.add(result, type.ax[i]);
            result = ArrayUtils.add(result, type.ay[i]);
        }
        for(double d : type.ad) {
            result = ArrayUtils.add(result, d);
        }
        return result;
    }
}
