package ai_server_cafe.util.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractThreadCafe extends Thread {
    protected final String name;
    protected final Logger logger;

    protected AbstractThreadCafe(String name) {
        this.name = name;
        this.logger = LogManager.getLogger(name);
    }

    public void run() {
        Thread.currentThread().setName(this.name);
        try {
            this.runThread();
        } catch(Exception exception) {
            this.logger.error("Unknown error occurred at thread : {}", this.name);
        }
    }

    synchronized public void terminate() {
        this.onTerminate();
        this.interrupt();
    }

    protected void onTerminate() {}

    protected abstract void runThread();
}
