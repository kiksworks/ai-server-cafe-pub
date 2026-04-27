package ai_server_cafe.util;

public enum TeamColor {
    BLUE(0),
    YELLOW(1);

    final int id;

    TeamColor(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static TeamColor getColor(int id) {
        for(TeamColor color : TeamColor.values()) {
            if (color.getId() == id) return color;
        }
        return TeamColor.BLUE;
    }

    public boolean isYellow() {
        return this == YELLOW;
    }

    public TeamColor getInvert() {
        if (this.isYellow()) {
            return TeamColor.BLUE;
        } else {
            return TeamColor.YELLOW;
        }
    }
}
