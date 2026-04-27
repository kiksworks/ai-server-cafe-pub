package ai_server_cafe.util.game;

import ai_server_cafe.config.Config;
import ai_server_cafe.model.field.AbstractFilteredObject;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;

public class FilterHelper {
    public static <T> boolean isOverOutSide(@Nonnull AbstractFilteredObject<T> fo, Config config) {
        if (MathHelper.isNanOrInfinity(fo.getX()) || MathHelper.isNanOrInfinity(fo.getY()) || MathHelper.isNanOrInfinity(fo.getZ()) || MathHelper.isNanOrInfinity(fo.getTheta())
                || MathHelper.isNanOrInfinity(fo.getVx()) || MathHelper.isNanOrInfinity(fo.getVy()) || MathHelper.isNanOrInfinity(fo.getVz()) || MathHelper.isNanOrInfinity(fo.getOmega()))
            return true;
        double width = 0.5 * config.maxFieldWidth;
        double height = 0.5 * config.maxFieldHeight;
        return FastMath.abs(fo.getX()) > width || FastMath.abs(fo.getY()) > height;
    }
}
