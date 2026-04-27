package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.awt.*;

public class BallCafe extends AbstractGraphicalComponent {
    private final FilteredBall ball;

    public BallCafe(@Nonnull FilteredBall ball, Color color) {
        super(color);
        this.ball = ball.clone();
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(30.0F));
        final double x = this.ball.getX();
        final double y = this.ball.getY();
        final double z = this.ball.getZ();
        final double ballRadius = ConfigManager.getInstance().getConfig().ballRadius;
        final double radius = ballRadius + 0.08 * z;
        // 黒縁
        graphics.setColor(Color.BLACK);
        graphics.fillOval((int)(x - (ballRadius + radius)), (int)(y - (ballRadius + radius)), 2 * (int)(ballRadius + radius), 2 * (int)(ballRadius + radius));
        // ボールを描画
        graphics.setColor(this.color);
        graphics.fillOval((int)(x - radius), (int)(y - radius), 2 * (int)radius, 2 * (int)radius);
        // ロストしていたら水色
        if (this.ball.isLost()) graphics.setColor(ColorHelper.ARCHIVE_SKY);
        final double radius0 = 500.0;
        if (UpdaterWorld.getInstance().getWorld(false).getHaveBall(TeamColor.BLUE)
            || UpdaterWorld.getInstance().getWorld(false).getHaveBall(TeamColor.YELLOW)) {
            // ボールがロボットの近くにあるとき、
            // くるくるする
            graphics.drawOval((int)(x - 2 * radius0), (int)(y - 2 * radius0), 4 * (int)radius0, 4 * (int)radius0);
            graphics.setStroke(new BasicStroke(60.0F));
            for (int i = 0; i < 4; i++) {
                double theta = i * FastMath.PI / 2 + 5 * TimeHelper.now();
                int x1 = (int)(x + 2 * (radius0 - radius) * FastMath.cos(theta));
                int x2 = (int)(x + 2 * (radius0 + radius) * FastMath.cos(theta));
                int y1 = (int)(y + 2 * (radius0 - radius) * FastMath.sin(theta));
                int y2 = (int)(y + 2 * (radius0 + radius) * FastMath.sin(theta));
                graphics.drawLine(x1, y1, x2, y2);
            }
        } else {
            graphics.drawOval((int)(x - radius0), (int)(y - radius0), 2 * (int)radius0, 2 * (int)radius0);
        }
    }
}
