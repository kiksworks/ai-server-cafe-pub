package ai_server_cafe.gui.component;

import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.swing.*;
import java.awt.*;

public class ProgressBarCafe extends JProgressBar implements IComponentCafe {
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private int lastValue;
    private final IFuncParam1<Integer, Integer> progress;

    /**
     *
     * @param x               x
     * @param y               y
     * @param width           width
     * @param height          height
     * @param progress        百分率を出力にする
     * @param initialValue    初期値
     * @param type            表示タイプ
     * @param isStringPainted 文字を表示するかどうか
     *                        (デフォルトは%で、任意の文字列を表示するならupdateをオーバーライドして設定する)
     */
    public ProgressBarCafe(double x, double y, double width, double height, IFuncParam1<Integer, Integer> progress,
                           int initialValue, EnumVisibleType type, boolean isStringPainted) {
        this.setStringPainted(isStringPainted);
        this.setVisible(true);
        this.setLayout(null);
        this.setBounds(new Rectangle((int) x, (int) y, (int) width, (int) height));
        this.lastValue = initialValue;
        this.setValue(initialValue);
        this.progress = progress;
        this.setMaximum(100);
        this.type = type;
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.setBounds(rect);
        return rect;
    }

    public ProgressBarCafe setDisplayType(int id, EnumDisplayType type) {
        if (id == 1)
            this.yType = type;
        else if (id == 2)
            this.widthType = type;
        else if (id == 3)
            this.heightType = type;
        else
            this.xType = type;
        return this;
    }

    public void paintComponent(Graphics graphics) {
        int value = this.progress.function(this.lastValue);
        if(value != this.lastValue) {
            this.setValue(value);
            this.lastValue = value;
        }
        super.paintComponent(graphics);
    }
}
