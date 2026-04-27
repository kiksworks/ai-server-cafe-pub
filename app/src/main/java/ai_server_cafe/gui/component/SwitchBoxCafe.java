package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SwitchBoxCafe extends AbstractButton implements IComponentCafe {
    private int textOffset;
    private final int size;
    @SuppressWarnings("unused")
	private final String text;
    @SuppressWarnings("unused")
	private int count;
    private final Color backgroundFalse;
    private final Color backgroundTrue;
    private final Color button;
    private final IFuncParam1<Void, Boolean> onSwitch;
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public SwitchBoxCafe(String text, double width, boolean valueDefault, int size, double x, double y, Color backgroundFalse, Color backgroundTrue, Color button, IFuncParam1<Void, Boolean> onSwitch, EnumVisibleType type) {
        this.textOffset = FastMath.max((int)width - 2 * size, 0);
        this.setModel(new DefaultButtonModel());
        this.setSelected(valueDefault);
        this.setLayout(null);
        this.setBounds((int) x, (int) y, (int)width, size);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (new Rectangle(SwitchBoxCafe.this.textOffset, 0, 2 * size, size).contains(e.getPoint())) {
                    setSelected(!isSelected());
                }
            }
        });
        this.size = size;
        this.count = 0;
        this.backgroundFalse = backgroundFalse;
        this.backgroundTrue = backgroundTrue;
        this.button = button;
        this.onSwitch = onSwitch;
        this.text = text;
        this.type = type;
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = size;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(2 * this.size + this.textOffset, this.size);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
    }

    @Override
    public int getHeight() {

        return getPreferredSize().height;
    }

    @Override
    public int getWidth() {
        return getPreferredSize().width;
    }

    @Override
    public void setSelected(boolean value) {
        this.count = 10;
        if (this.onSwitch != null) {
            this.onSwitch.function(value);
        }
        super.setSelected(value);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        if (graphics instanceof Graphics2D) {
            ((Graphics2D)graphics).setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D)graphics).setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        Font font = new Font(config.font, config.fontStyle, config.fontSize);
        graphics.setFont(font);
        graphics.setColor(ColorHelper.SCREEN_DARK);
        graphics.drawString(this.text, this.textOffset - 8 - graphics.getFontMetrics().stringWidth(this.text), (this.size / 2 + graphics.getFontMetrics().getHeight() / 2) - 2);
        if (this.isSelected()) {
            graphics.setColor(this.backgroundTrue);
        } else {
            graphics.setColor(this.backgroundFalse);
        }
        graphics.fillOval(this.textOffset, 0, this.size, this.size);
        graphics.fillOval(this.size + this.textOffset, 0, this.size, this.size);
        graphics.fillRect(this.size / 2 + this.textOffset, 0, this.size, this.size);
        graphics.setColor(this.button);
        int offset = this.textOffset;
        if (this.isSelected()) {
            offset = this.size + this.textOffset;
        }
        graphics.fillOval(offset + (int)(0.1 * this.size), (int)(0.1 * this.size), (int)(0.8 * this.size), (int)(0.8 * this.size));
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }


    public SwitchBoxCafe setDisplayType(int id, EnumDisplayType type) {
        if (id == 1)
            this.yType = type;
        else if (id == 2)
            this.widthType = type;
        else if (id == 3)
            this.heightType = EnumDisplayType.DEFAULT;
        else
            this.xType = type;
        return this;
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.textOffset = FastMath.max(rect.width - 2 * this.size, 0);
        this.setBounds(rect);
        return rect;
    }
}
