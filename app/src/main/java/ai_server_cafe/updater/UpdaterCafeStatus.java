package ai_server_cafe.updater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UpdaterCafeStatus {
    private final Logger logger;
    private static UpdaterCafeStatus instance;
    private double fps;

    private UpdaterCafeStatus() {
        this.logger = LogManager.getLogger("Cafe-Status");
        this.fps = 0;
    }

    public synchronized static UpdaterCafeStatus getInstance() {
        if (instance == null) {
            instance = new UpdaterCafeStatus();
        }
        return instance;
    }

    public synchronized void setFPS(double fps) {
        this.fps = fps;
    }

    public synchronized double getFps() {
        return this.fps;
    }
}
