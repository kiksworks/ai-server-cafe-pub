package ai_server_cafe.util.gui;

import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public enum EnumLogLevel {
    ALL("ALL", 0),
    TRACE("TRACE", 1),
    DEBUG("DEBUG", 2),
    INFO("INFO", 3),
    WARN("WARN", 4),
    ERROR("ERROR", 5),
    FATAL("FATAL", 6),
    OFF("OFF", 7);

    private final String tag;
    private final int rank;
    EnumLogLevel(String tag, int rank) {
        this.tag = tag;
        this.rank = rank;
    }

    public int getRank() {
        return this.rank;
    }

    public List<EnumLogLevel> getUpper() {
        List<EnumLogLevel> result = new ArrayList<>();
        for (EnumLogLevel level : EnumLogLevel.values()) {
            if (level.getRank() >= this.rank) {
                result.add(level);
            }
        }
        return result;
    }

    public String getTag() {
        return this.tag;
    }

    @Nonnull
    private String getSplitTag() {
        return " [" + this.tag + "] ";
    }

    @Nonnull
    public String splitLogger(@Nonnull String raw, int max) {
        StringBuilder result = new StringBuilder();
        String[] splits = raw.split("\n\r");
        int start = max == -1 ? 0 : FastMath.max(0, splits.length - FastMath.min(splits.length, max) - 1);
        for (int i = start; i < splits.length; i++) {
            if (isMatch(splits[i], this)) {
                result.append(splits[i]).append("\n\r");
            }
        }
        return result.toString();
    }

    @Nonnull
    public String splitLogger(@Nonnull String raw) {
        return splitLogger(raw, -1);
    }

    private static boolean isMatch(String s, @Nonnull EnumLogLevel level) {
        List<EnumLogLevel> levels = level.getUpper();
        for (EnumLogLevel l : levels) {
            if (s.contains(l.getSplitTag()))
                return true;
        }
        return false;
    }

    public static EnumLogLevel getFromRank(int rank) {
        if (rank > 7 || rank < 0)
            return EnumLogLevel.ALL;
        return EnumLogLevel.values()[rank];
    }
}
