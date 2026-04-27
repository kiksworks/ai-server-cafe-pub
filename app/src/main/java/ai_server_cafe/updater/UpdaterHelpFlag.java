package ai_server_cafe.updater;

import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;

import java.util.Optional;

public final class UpdaterHelpFlag {
    private static UpdaterHelpFlag instance = null;

    // キックオフ、セットプレーでボールに触れたロボットのID
    private Optional<Integer> blueLastKickerId;
    private Optional<Integer> yellowLastKickerId;

    private UpdaterHelpFlag() {
        this.blueLastKickerId = Optional.empty();
        this.yellowLastKickerId = Optional.empty();
    }

    /**
     * インスタンスファクトリ
     * @return instance
     */
    public synchronized static UpdaterHelpFlag getInstance() {
        if (instance == null) {
            instance = new UpdaterHelpFlag();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    /* ========================= 以下 setter =========================*/

    synchronized public void setLastKickerId(Optional<Integer> id, TeamColor color) {
        if (color.isYellow()) {
            this.yellowLastKickerId = id;
        } else {
            this.blueLastKickerId = id;
        }
    }

    /* ========================= 以下 getter =========================*/

    synchronized public WrapperWeakCloneable<Optional<Integer>> getLastKickerId(TeamColor color) {
        if (color.isYellow()) {
            return new WrapperWeakCloneable<>(this.yellowLastKickerId);
        } else {
            return new WrapperWeakCloneable<>(this.blueLastKickerId);
        }
    }

}