package ai_server_cafe.gui;

import javax.swing.*;
import java.awt.*;

public class SplitPane extends JSplitPane {
    public SplitPane(int type, int width, Component area1, Component area2) {
        super(type, false, area1, area2);
        this.setVisible(true);
        this.setDividerLocation(width);
        this.setDividerSize(4);
    }
}
