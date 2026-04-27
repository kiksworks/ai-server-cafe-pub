package ai_server_cafe.util.game;

import ai_server_cafe.game.captain.AbstractCaptain;
import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.util.TeamColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class InstanceHelper {
    @Nonnull
    public static <T extends AbstractFormation> T makeFormation(@Nonnull Class<? extends T> clazz, TeamColor color, int[] activeRobots) {
        try {
            T instance = clazz.getConstructor(TeamColor.class, int[].class).newInstance(color, activeRobots);
            return instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static <T extends AbstractCaptain> Optional<T> makeCaptain(@Nullable Class<T> clazz, @Nonnull EnumCaptainType type, TeamColor color) {
        try {
            if (clazz == null)
                return Optional.empty();
            T instance = clazz.getConstructor(EnumCaptainType.class, TeamColor.class).newInstance(type, color);
            return Optional.of(instance);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
