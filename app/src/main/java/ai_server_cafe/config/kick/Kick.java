package ai_server_cafe.config.kick;

import ai_server_cafe.updater.ConfigManager;

public class Kick {
    public Data[] data;
    public Kick() {
        this.data = new Data[2 * ConfigManager.MAX_ROBOTS];
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = new Data(i % ConfigManager.MAX_ROBOTS, i < ConfigManager.MAX_ROBOTS);
        }
    }
    public static class Data {
        public int id;
        public boolean chip;
        public double c0;
        public double c1;
        public double c2;
        public boolean hasData;
        public Data(int id, boolean chip) {
            this.hasData = false;
            this.id = id;
            this.chip = chip;
            this.c0 = 0.0;
            this.c1 = 0.0;
            this.c2 = 0.0;
        }
    }
}
