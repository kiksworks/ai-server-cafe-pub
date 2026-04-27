package ai_server_cafe.game.strategy;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public enum XGType {
    TYPEA(0, "TypeA"),
    TYPEB(1, "TypeB"),
    TYPEC(2, "TypeC");

    private final int id;
    private final String name;

    XGType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public static XGType getFromId(int id) {
        for (XGType type : XGType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return TYPEA;
    }

    @Nonnull
    public static XGType[] getExcludedAlways() {
        List<XGType> result = new ArrayList<>();
        for (XGType type : XGType.values()) {
            if (type != TYPEA) result.add(type);
        }
        return result.toArray(new XGType[] {});
    }

    public String toString() {
        return this.name;
    }
}
