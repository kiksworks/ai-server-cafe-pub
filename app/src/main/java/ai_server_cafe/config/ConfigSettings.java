package ai_server_cafe.config;

import ai_server_cafe.util.interfaces.AbstractCloneable;

public class ConfigSettings extends AbstractCloneable {
    public String configName;

    public ConfigSettings() {
        this.configName = "config.json";
    }

    @Override
    public AbstractCloneable clone() {
        ConfigSettings result = new ConfigSettings();
        result.configName = this.configName;
        return result;
    }
}
