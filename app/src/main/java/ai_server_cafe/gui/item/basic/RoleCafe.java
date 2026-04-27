package ai_server_cafe.gui.item.basic;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class RoleCafe extends AbstractGraphicalComponent {
    private final IntegratedRobot robot;

    public RoleCafe(Color color, IntegratedRobot robot) {
        super(color);
        this.robot = robot;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        if (robot.getRole().isEmpty()) return;

        Vector2D position = robot.getRobot().position();
        int radius = (int) ConfigManager.getInstance().getConfig().robotRadius;

        AbstractRole role = robot.getRole().get();
        String name = role.getName();
        // Roleに対応した色の円を描画
        if (name.equals("role_attack_waiter")) {
            // 赤
            graphics.setColor(ColorHelper.ROLE_RED);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_defense")) {
            // 緑
            graphics.setColor(ColorHelper.ROLE_GREEN);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_chaser")) {
            // 白
            graphics.setColor(ColorHelper.ROLE_WHITE);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_support")) {
            // 紫
            graphics.setColor(ColorHelper.ROLE_PURPLE);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_keeper")) {
            // キーパーはチームカラー
            if (robot.getColor() == TeamColor.BLUE) {
                graphics.setColor(ColorHelper.ROLE_BLUE);
            } else {
                graphics.setColor(ColorHelper.ROLE_YELLOW);
            }
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_kick")) {
            graphics.setColor(ColorHelper.ROLE_PINK);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_ballPlacer")) {
            graphics.setColor(ColorHelper.ROLE_ORANGE);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_exiter")) {
            // イエローカード描画
            graphics.setColor(ColorHelper.CARD_YELLOW);
            graphics.fillRect((int) (position.getX() + radius), (int) (position.getY() + radius * 2), radius, (int) (1.4 * radius));
        } else if (name.equals("role_waiter")) {
            graphics.setColor(ColorHelper.ROLE_CYAN);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_PenaltyKicker")) {
            graphics.setColor(ColorHelper.ROLE_ROSE);
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_PenaltyKeeper")) {
            // キーパーはチームカラー
            if (robot.getColor().isYellow()) {
                graphics.setColor(ColorHelper.ROLE_STRONG_YELLOW);
            } else {
                graphics.setColor(ColorHelper.ROLE_STRONG_BLUE);
            }
            graphics.fillOval((int)(position.getX() - radius * 2), (int)(position.getY() - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("role_halt")) {
            // レッドカード描画
            graphics.setColor(ColorHelper.CARD_RED);
            graphics.fillRect((int) (position.getX() + radius), (int) (position.getY() + radius * 2), radius, (int) (1.4 * radius));
        } else {
            // 文字で表示
            graphics.setColor(ColorHelper.LINE_WHITE);
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) (radius * 1.5));
            graphics.setFont(font);
            AffineTransform af = graphics.getTransform();
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
            graphics.drawString(name, (int) (robot.getRobot().getX() + radius), (int) (-1 * (robot.getRobot().getY() + radius * 1.5)));
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
        }
    }
}