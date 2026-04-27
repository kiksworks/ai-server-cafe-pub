package ai_server_cafe.util.math;

import javax.annotation.Nonnull;

public final class Vector2I {
    private final int x;
    private final int y;
    public Vector2I(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    @Nonnull
    public Vector2I add(@Nonnull Vector2I vector2I) {
        return new Vector2I(this.x + vector2I.getX(), this.y + vector2I.getY());
    }
}
