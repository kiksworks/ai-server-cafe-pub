package ai_server_cafe.gui.item.basic;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.gui.EnumDisplayType;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.awt.*;

public class MemoryCafe extends AbstractGraphicalComponent {
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public MemoryCafe(double x, double y, double width, double height, EnumDisplayType xType,
                      EnumDisplayType yType, EnumDisplayType widthType,
                      EnumDisplayType heightType) {
        super(ColorHelper.LINE_WHITE, xType, yType, widthType, heightType);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(this.paneWidth, this.paneHeight));
        double total = Runtime.getRuntime().maxMemory() / 1000000.0;
        double allocated = Runtime.getRuntime().totalMemory() / 1000000.0;
        double using = allocated - Runtime.getRuntime().freeMemory() / 1000000.0;
        graphics.setColor(ColorHelper.GROUND_BLACK);
        graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
        graphics.setColor(ColorHelper.MEMORY_BLACK);
        graphics.fillRect(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);
        graphics.setColor(ColorHelper.MEMORY_YELLOW);
        graphics.fillRect(rect.x + 1, rect.y + 1, (int)((rect.width - 2) * allocated / total), rect.height - 2);
        graphics.setColor(ColorHelper.MEMORY_GREEN);
        graphics.fillRect(rect.x + 1, rect.y + 1, (int)((rect.width - 2) * using / total), rect.height - 2);
        graphics.setColor(ColorHelper.GROUND_BLACK);
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        Font f = new Font(config.font, config.fontStyle, config.fontSize);
        graphics.setFont(f);
        String totalStr = String.format("%4.1f", total);
        String usingStr = String.format("%4.1f", using);
        String allocatedStr = String.format("%4.1f", allocated);
        int spaceUsing = totalStr.length() - usingStr.length();
        int spaceAllocated = totalStr.length() - allocatedStr.length();
        graphics.drawString("Total : " + totalStr + "MB", rect.x + rect.width + 3, rect.y + graphics.getFontMetrics().getHeight());
        graphics.drawString("Using : ", rect.x + rect.width + 3, rect.y + graphics.getFontMetrics().getHeight() * 2);
        graphics.drawString(usingStr, rect.x + rect.width + 3 + graphics.getFontMetrics().stringWidth("Using : ") + graphics.getFontMetrics().stringWidth("0") * spaceUsing, rect.y + graphics.getFontMetrics().getHeight() * 2);
        graphics.drawString(" / ", rect.x + rect.width + 3 + graphics.getFontMetrics().stringWidth("Using : " + totalStr), rect.y + graphics.getFontMetrics().getHeight() * 2);
        graphics.drawString(allocatedStr, rect.x + rect.width + 3 + graphics.getFontMetrics().stringWidth("Using : " + totalStr + " / ") + graphics.getFontMetrics().stringWidth("0") * spaceAllocated, rect.y + graphics.getFontMetrics().getHeight() * 2);
        graphics.drawString("MB", rect.x + rect.width + 3 + graphics.getFontMetrics().stringWidth("Using : " + totalStr + " / " + totalStr), rect.y + graphics.getFontMetrics().getHeight() * 2);
    }
}
