package ai_server_cafe.gui;

import ai_server_cafe.gui.registry.RegistryConfigComponents;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.interfaces.IFunction;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConfigWindow extends JFrame {
    private static ConfigWindow instance = null;

    public ConfigWindow(String title, int defaultWidth, int defaultHeight, IFunction<Void> onExit) {
        super(title);
        if (onExit != null)
            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    onExit.function(e);
                }
            });
        this.setBounds(500, 100, defaultWidth, defaultHeight);
        RegistryConfigComponents.registerContents(this);
    }

    public static ConfigWindow getInstance() {
        if (instance == null) {
            instance = new ConfigWindow("Config Editor", 700, 700, null);
        }
        return instance;
    }

    public void reset() {
        this.getContentPane().removeAll();
        this.getContentPane().revalidate();
        RegistryConfigComponents.registerContents(this);
        updateTitle();
        repaint();
    }

    public void updateTitle() {
        String title = "Config Editor (";
        if (ConfigManager.getInstance().isDirty()) {
            title = title + "*";
        }
        title = title + ConfigManager.getInstance().getConfigSettings().configName + ")";
        setTitle(title);
    }
}
