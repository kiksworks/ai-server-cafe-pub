package ai_server_cafe.util.math;

import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.KickManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;

public class KickConverter {
    public static int toPower(int id, @Nonnull Pair<EnumKickType, Double> kickFlagSpeed) {
        return toPower(id, kickFlagSpeed, true);
    }

    public static int toPower(int id, @Nonnull Pair<EnumKickType, Double> kickFlagSpeed, boolean useData) {
        if (useData && KickManager.getInstance().hasData(id, kickFlagSpeed.getFirst() == EnumKickType.CHIP)) {
            return KickManager.getInstance().speedToPower(kickFlagSpeed.getSecond(), id, kickFlagSpeed.getFirst() == EnumKickType.CHIP);
        }
        if (kickFlagSpeed.getFirst() == EnumKickType.CHIP) {
            return (int)MathHelper.clamp(0.08 * kickFlagSpeed.getSecond() - 17, 20, 255);
        } else {
            return (int)MathHelper.clamp(0.033 * kickFlagSpeed.getSecond() - 17, 20, 255);
        }
    }

    public static double toSpeed(int id, @Nonnull Pair<EnumKickType, Integer> kickFlagPower, boolean useData) {
        if (useData && KickManager.getInstance().hasData(id, kickFlagPower.getFirst() == EnumKickType.CHIP)) {
            return KickManager.getInstance().powerToSpeed(kickFlagPower.getSecond(), id, kickFlagPower.getFirst() == EnumKickType.CHIP);
        }
        final boolean isChip = kickFlagPower.getFirst() == EnumKickType.CHIP;
        double power = kickFlagPower.getSecond();
        final double limit = isChip ? 5300.0 : 100000.0;
        if (isChip) {
            return MathHelper.clamp(80 * power, 0.0, limit);
        } else {
            return MathHelper.clamp(30  * power + 500, 0.0, limit);
        }
    }

    public static double toSpeed(int id, @Nonnull Pair<EnumKickType, Integer> kickFlagPower, TeamColor color) {
        return toSpeed(id, kickFlagPower, ConfigManager.getInstance().getConfig().isUseKickRegulatorData(color));
    }

    /**
     * パスのキック速度を計算する
     * @param kickerPosition
     * @param target
     * @return
     */
    public static double passSpeed(Vector2D kickerPosition, Vector2D target) {
        final double ballBrake = ConfigManager.getInstance().getConfig().ballBrake;
        return MathHelper.clamp(FastMath.sqrt(2 * ballBrake * MathHelper.distance2D(kickerPosition, target)) + 1500, 1000, 5000);
    }

    /**
     * チップのキック速度を計算
     * @param kickerPosition ボールをける位置
     * @param target 目標位置
     * @return チップキックの速さ
     */
    public static double chipSpeed(Vector2D kickerPosition, Vector2D target) {
        return FastMath.sqrt(2 * 2000 * MathHelper.distance2D(kickerPosition, target));
    }
}
