package ai_server_cafe.util.thread;

public abstract class AbstractLoopThreadCafe extends AbstractThreadCafe {
    protected AbstractLoopThreadCafe(String name) {
        super(name);
    }

    @Override
    protected void runThread() {
        try {
            this.init();
        } catch(Exception exception) {
            this.logger.error("Unknown error occurred on init function");
            exception.printStackTrace();
        }
        while(!this.isInterrupted()) {
            try {
                this.loop();
            } catch (Exception exception) {
                this.logger.error("Unknown error occurred on loop function : {}", (Object[]) exception.getStackTrace());
                exception.printStackTrace();
            }
        }
        this.logger.info("Thread {} is successfully terminated", this.name);
    }

    protected abstract void loop();

    protected abstract void init();
}
