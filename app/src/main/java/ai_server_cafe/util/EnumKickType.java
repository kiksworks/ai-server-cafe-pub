package ai_server_cafe.util;

public enum EnumKickType {
    NONE(0),
    CHIP(1),
    STRAIGHT(2);

    private final int id;

    EnumKickType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
