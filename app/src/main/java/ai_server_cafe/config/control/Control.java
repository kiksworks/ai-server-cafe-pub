package ai_server_cafe.config.control;

import ai_server_cafe.updater.ConfigManager;

public class Control {
    public Control.Data[] data;
    public Control() {
        this.data = new Control.Data[2 * ConfigManager.MAX_ROBOTS];
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = new Control.Data(i % ConfigManager.MAX_ROBOTS, i < ConfigManager.MAX_ROBOTS);
        }
    }
    public static class Data {
        public int id;
        public boolean isTypeB;
        public double centripetalRatio;
        public boolean hasData;
        public Data(int id, boolean isTypeB) {
            this.hasData = false;
            this.id = id;
            this.isTypeB = isTypeB;
            this.centripetalRatio = 0.0;
        }
    }
}
