package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import static java.awt.event.KeyEvent.VK_ENTER;

public class TextAreaCafe extends JTextArea implements IComponentCafe {
    private final IFuncParam1<Void, String> onTextArea;
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private int offset;

    /**
     * テキストエリアを出力する
     *
     * @param x          TextAreaのx座標
     * @param y          TextAreaのｙ座標
     * @param width      TextAreaの幅
     * @param rows       TextAreaの行数
     * @param onTextArea IFuncParam1
     * @param type       EnumVisibleType
     */
    public TextAreaCafe(String init, double x, double y, double width, int rows, IFuncParam1<Void, String> onTextArea, EnumVisibleType type) {
        super(1, 1);
        this.setVisible(true);
        this.setLayout(null);
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.type = type;
        this.setText(init);
        this.onTextArea = onTextArea;
        final String[] inputText = {""};
        this.setBounds((int) x, (int) y, (int) width, this.getFontMetrics(this.getFont()).getHeight() * rows);
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                try {
                    String[] splits = getText().split("\n");
                    inputText[0] = "";
                    for (String s : splits) {
                        inputText[0] += s;
                    }
                    System.gc();
                    setText(inputText[0]);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    // DO nothing
                }
            }
        });
        //EnterKeyが押されたとき一行目の文字を読み込む
        this.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == VK_ENTER)
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
            }
            @Override
            public void keyTyped(KeyEvent e) {}
        });
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = this.getFontMetrics(this.getFont()).getHeight() * rows;
    }

    @Override
    public String getText() {
        return super.getText();
    }

    @Override
    public void setText(String t) {
        if (this.onTextArea != null)
            this.onTextArea.function(t);
        super.setText(t);
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    public TextAreaCafe setDisplayType(int id, EnumDisplayType type) {
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
        this.setBounds(rect.x + this.offset, rect.y, rect.width, rect.height);
        return rect;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e == null) return;
        if (e.getButton() == 1) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
        }
    }

    public TextAreaCafe setOffset(int x) {
        this.offset = x;
        return this;
    }
}
