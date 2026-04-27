package ai_server_cafe.util.gui;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * DEFAULT = そのまま, 絶対座標系
 * RATIO = Paneのサイズに割合をかける, 引数に0 ~ 1しか指定できない
 * FIT = Paneのサイズから引数分マイナスした座標または大きさ
 */
public enum EnumDisplayType {
    DEFAULT, RATIO, FIT;

    @Nonnull
    public static Rectangle getSize(@Nonnull Vector2D pos, EnumDisplayType xType, EnumDisplayType yType, @Nonnull Vector2D size, EnumDisplayType widthType, EnumDisplayType heightType,
                                    @Nonnull Vector2D paneSize) {
        double x = pos.getX();
        if (xType == DEFAULT) {
            x = pos.getX();
        } else if (xType == RATIO) {
            x = paneSize.getX() * pos.getX();
        } else if (xType == FIT) {
            x = FastMath.max(paneSize.getX() - pos.getX(), 0);
        }
        double y = pos.getY();
        if (yType == DEFAULT) {
            y = pos.getY();
        } else if (yType == RATIO) {
            y = paneSize.getY() * pos.getY();
        } else if (yType == FIT) {
            y = FastMath.max(paneSize.getY() - pos.getY(), 0);
        }
        double width = size.getX();
        if (widthType == DEFAULT) {
            width = size.getX();
        } else if (widthType == RATIO) {
            width = paneSize.getX() * size.getX();
        } else if (widthType == FIT) {
            width = FastMath.max(paneSize.getX() - size.getX() - x, 0);
        }
        double height = size.getY();
        if (heightType == DEFAULT) {
            height = size.getY();
        } else if (heightType == RATIO) {
            height = paneSize.getY() * size.getY();
        } else if (heightType == FIT) {
            height = FastMath.max(paneSize.getY() - size.getY() - y, 0);
        }
        return new Rectangle((int) x, (int) y, (int) width, (int) height);
    }

    public Vector2D getPos(Vector2D pos, Vector2D paneSize) {

        return pos;
    }
}
