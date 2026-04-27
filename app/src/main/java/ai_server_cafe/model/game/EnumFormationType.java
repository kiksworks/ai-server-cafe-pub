package ai_server_cafe.model.game;

public enum EnumFormationType {
    UNKNOWN(0),
    HALT(1),
    STOP(2),
    FORCE_START(3),
    KICKOFF(4),
    PENALTY(5),
    SHOOTOUT(6),
    SET_PLAY(7),
    BALL_PLACEMENT(8),
    TIMEOUT(9);

    int id;

    EnumFormationType(int id) {
         this.id = id;
    }

    public static EnumFormationType getFromId(int id) {
        for (EnumFormationType ert : EnumFormationType.values()) {
            if (ert.getId() == id) {
                return ert;
            }
        }
        return UNKNOWN;
    }

    public int getId() {
        return this.id;
    }
}
