package ai_server_cafe.updater;

import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UpdaterStats {
    private static UpdaterStats instance = null;
    // キックした数
    private int blueKickCount;
    private int yellowKickCount;
    // パスが成功した数
    private int bluePassCount;
    private int yellowPassCount;
    // パスカットした数
    private int bluePassCutCount;
    private int yellowPassCutCount;
    // シュートした数
    private int blueShootCount;
    private int yellowShootCount;
    // 枠内シュート
    private int blueShootInFrameCount;
    private int yellowShootInFrameCount;
    //累積ゴール期待値
    private double blueShootInFrameXG;
    private double yellowShootInFrameXG;
    // ボール保持率
    private int blueBallPossessionCount;
    private int yellowBallPossessionCount;
    private final Logger logger = LogManager.getLogger("stats updater");

    private UpdaterStats() {
        blueKickCount = 0;
        yellowKickCount = 0;
        bluePassCount = 0;
        yellowPassCount = 0;
        bluePassCutCount = 0;
        yellowPassCutCount = 0;
        blueShootCount = 0;
        yellowShootCount = 0;
        blueShootInFrameCount = 0;
        yellowShootInFrameCount = 0;
        blueShootInFrameXG = 0;
        yellowShootInFrameXG = 0;
        blueBallPossessionCount = 0;
        yellowBallPossessionCount = 0;
    }

    /**
     * インスタンスファクトリ
     * @return instance
     */
    public synchronized static UpdaterStats getInstance() {
        if (instance == null) {
            instance = new UpdaterStats();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    /* ========================= 以下 setter =========================*/

    synchronized public void setKickCount(int count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowKickCount = count;
        } else {
            this.blueKickCount = count;
        }
    }

    synchronized public void setPassCount(int count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowPassCount = count;
        } else {
            this.bluePassCount = count;
        }
    }

    synchronized public void setPassCutCount(int count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowPassCutCount = count;
        } else {
            this.bluePassCutCount = count;
        }
    }

    synchronized public void setShootCout(int count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowShootCount = count;
        } else {
            this.blueShootCount = count;
        }
    }

    synchronized public void setShootInFrameCout(int count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowShootInFrameCount = count;
        } else {
            this.blueShootInFrameCount = count;
        }
    }
    synchronized public void setShootInFrameXG(double count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowShootInFrameXG = FastMath.round(count* 1000.0) / 1000.0;
        } else {
            this.blueShootInFrameXG = FastMath.round(count* 1000.0) / 1000.0;
        }
    }

    synchronized public void setBallPossessionCount(int count, TeamColor color) {
        if (color.isYellow()) {
            this.yellowBallPossessionCount = count;
        } else {
            this.blueBallPossessionCount = count;
        }
    }

    /* ========================= 以下 getter =========================*/

    synchronized public int getKickCount(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowKickCount;
        } else {
            return this.blueKickCount;
        }
    }

    synchronized public int getPassCount(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowPassCount;
        } else {
            return this.bluePassCount;
        }
    }

    synchronized public int getPassCutCount(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowPassCutCount;
        } else {
            return this.bluePassCutCount;
        }
    }

    synchronized public int getShootCount(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowShootCount;
        } else {
            return this.blueShootCount;
        }
    }

    synchronized public int getShootInFrameCount(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowShootInFrameCount;
        } else {
            return this.blueShootInFrameCount;
        }
    }

    synchronized public double getShootInFrameXG(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowShootInFrameXG;
        } else {
            return this.blueShootInFrameXG;
        }
    }

    synchronized public int getBallPossessionCount(TeamColor color) {
        if (color.isYellow()) {
            return this.yellowBallPossessionCount;
        } else {
            return this.blueBallPossessionCount;
        }
    }

}