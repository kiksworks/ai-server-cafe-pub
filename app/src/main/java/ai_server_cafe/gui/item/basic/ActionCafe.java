package ai_server_cafe.gui.item.basic;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class ActionCafe extends AbstractGraphicalComponent {
    private final IntegratedRobot robot;

    public ActionCafe(Color color, IntegratedRobot robot) {
        super(color);
        this.robot = robot;
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        graphics.setColor(ColorHelper.LINE_WHITE);

        Vector2D position = robot.getRobot().position();
        double x = position.getX();
        double y = position.getY();
        double theta = robot.getRobot().getTheta();
        int radius = (int) ConfigManager.getInstance().getConfig().robotRadius;

        if (robot.getAction().isEmpty()) {
            if (ConfigManager.getInstance().getConfig().getCaptain(this.robot.getColor()) == EnumCaptainType.NONE) {
                return;
            }
            // 文字で表示
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) (radius * 1.5));
            graphics.setFont(font);
            AffineTransform af = graphics.getTransform();
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
            graphics.drawString("EMPTY", (int) robot.getRobot().getX(), (int) (-1 * (robot.getRobot().getY() - radius * 2)));
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
            return;
        }

        AbstractAction action = robot.getAction().get();
        String name = action.getName();
        if (name.contains("_with_planner")) {
            // 円
            graphics.setStroke(new BasicStroke(5.0F));
            graphics.drawOval((int)(x - radius * 3), (int)(y - radius * 3), radius * 6, radius * 6);
            // nameを書き換える(with_plannerを消す)
            name = name.replace("_with_planner", "");
        }
        // アクションに対応した図形を描画
        if (name.equals("move")) {
            // Do Nothing
        } else if (name.equals("receive")) {
            // 円と孤
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.setStroke(new BasicStroke(24.0F));
            graphics.drawArc((int)(x - radius * 4), (int)(y - radius * 4), radius * 8, radius * 8, (int)(theta / FastMath.PI * -180 - 20), 40);
        } else if (name.equals("getBall")) {
            // 円と三角
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.fillPolygon(new int[] {(int)(x + radius * 3.5 * FastMath.cos(theta - 0.3)),(int)(x + radius * 3.5 * FastMath.cos(theta + 0.3)),(int)(x + radius * 4.5 * FastMath.cos(theta))},
                    new int[] {(int)(y + radius * 3.5 * FastMath.sin(theta - 0.3)),(int)(y + radius * 3.5 * FastMath.sin(theta + 0.3)),(int)(y + radius * 4.5 * FastMath.sin(theta))}, 3);
        } else if (name.equals("guard")) {
            // 六角形
            int[] xs = new int[6];
            int[] ys = new int[6];
            for (int i = 0; i < 6; i++) {
                xs[i] = (int)(x + 1.5 * radius * FastMath.cos(2 * FastMath.PI * i / 6));
                ys[i] = (int)(y + 1.5 * radius * FastMath.sin(2 * FastMath.PI * i / 6));
            }
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.drawPolygon(xs, ys, 6);
        } else if (name.equals("protectBall")) {
            // 二重の円
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.drawOval((int)(x - radius * 2), (int)(y - radius * 2), radius * 4, radius * 4);
        } else if (name.equals("rob_ball")) {
            // 衛星
            graphics.setStroke(new BasicStroke(12.0F));
            for (int i = 0; i < 4; i++) {
                double x1 = x + 3 * radius * FastMath.cos(i * FastMath.PI / 2 + 5 * TimeHelper.now());
                double y1 = y + 3 * radius * FastMath.sin(i * FastMath.PI / 2 + 5 * TimeHelper.now());
                graphics.fillOval((int)(x1 - radius * 0.5), (int)(y1 - radius * 0.5), radius, radius);
            }
        } else if (name.equals("mitoma")) {
            // 扇
            graphics.fillArc((int) (x - 4 * radius), (int) (y - 4 * radius), 8 * radius, 8 * radius,
                    (int) -(180 / FastMath.PI * theta - 30), -60);
        } else if (name.equals("kick")) {
            // 円と矢印
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.setStroke(new BasicStroke(24.0F));
            graphics.drawLine((int)x, (int)y, (int)(x + 5 * radius * FastMath.cos(theta)), (int)(y + 5 * radius * FastMath.sin(theta)));
            graphics.fillPolygon(new int[] {(int)(x + radius * 5 * FastMath.cos(theta - 0.2)),(int)(x + radius * 5 * FastMath.cos(theta + 0.2)),(int)(x + radius * 6 * FastMath.cos(theta))},
                    new int[] {(int)(y + radius * 5 * FastMath.sin(theta - 0.2)),(int)(y + radius * 5 * FastMath.sin(theta + 0.2)),(int)(y + radius * 6 * FastMath.sin(theta))}, 3);
        } else if (name.equals("goalKeep")) {
            // 三角2つ
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.fillPolygon(new int[] {(int)(x + radius * 3.5 * FastMath.cos(theta - 0.3 - 0.5 * FastMath.PI)),(int)(x + radius * 3.5 * FastMath.cos(theta + 0.3 - 0.5 * FastMath.PI)),(int)(x + radius * 4.5 * FastMath.cos(theta - 0.5 * FastMath.PI))},
                    new int[] {(int)(y + radius * 3.5 * FastMath.sin(theta - 0.3 - 0.5 * FastMath.PI)),(int)(y + radius * 3.5 * FastMath.sin(theta + 0.3 - 0.5 * FastMath.PI)),(int)(y + radius * 4.5 * FastMath.sin(theta - 0.5 * FastMath.PI))}, 3);
            graphics.fillPolygon(new int[] {(int)(x + radius * 3.5 * FastMath.cos(theta + 0.5 * FastMath.PI - 0.3)),(int)(x + radius * 3.5 * FastMath.cos(theta + 0.5 * FastMath.PI + 0.3)),(int)(x + radius * 4.5 * FastMath.cos(theta + 0.5 * FastMath.PI))},
                    new int[] {(int)(y + radius * 3.5 * FastMath.sin(theta + 0.5 * FastMath.PI - 0.3)),(int)(y + radius * 3.5 * FastMath.sin(theta + 0.5 * FastMath.PI + 0.3)),(int)(y + radius * 4.5 * FastMath.sin(theta + 0.5 * FastMath.PI))}, 3);
        } else if (name.equals("ballPlace")) {
            graphics.fillPolygon(new int[] {(int)(x + radius * 3.0 * FastMath.cos(theta - 0.3)),(int)(x + radius * 3.0 * FastMath.cos(theta + 0.3)),(int)(x + radius * 1.5 * FastMath.cos(theta))},
                    new int[] {(int)(y + radius * 3.0 * FastMath.sin(theta - 0.3)),(int)(y + radius * 3.0 * FastMath.sin(theta + 0.3)),(int)(y + radius * 1.5 * FastMath.sin(theta))}, 3);
        } else if (name.equals("halt")) {
            graphics.setStroke(new BasicStroke(12.0F));
            graphics.drawOval((int)(x - radius * 2), (int)(y - radius * 2), radius * 4, radius * 4);
            graphics.drawLine((int)(x - radius * 1.41), (int)(y - radius * 1.41), (int)(x + radius * 1.41), (int)(y + radius * 1.41));
            graphics.drawLine((int)(x - radius * 1.41), (int)(y + radius * 1.41), (int)(x + radius * 1.41), (int)(y - radius * 1.41));
        } else if (name.equals("hold")) {
            float dash[] = {24.0F * 2.0F, 24.0F * 2.0F};
            graphics.setStroke(new BasicStroke(24.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3.0f, dash, 0.0f));
            graphics.drawLine((int)x, (int)y, (int)(x + 5 * radius * FastMath.cos(theta)), (int)(y + 5 * radius * FastMath.sin(theta)));
            graphics.fillPolygon(new int[] {(int)(x + radius * 5 * FastMath.cos(theta - 0.2)),(int)(x + radius * 5 * FastMath.cos(theta + 0.2)),(int)(x + radius * 6 * FastMath.cos(theta))},
                    new int[] {(int)(y + radius * 5 * FastMath.sin(theta - 0.2)),(int)(y + radius * 5 * FastMath.sin(theta + 0.2)),(int)(y + radius * 6 * FastMath.sin(theta))}, 3);
        } else {
            // 文字で表示
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) (radius * 1.5));
            graphics.setFont(font);
            AffineTransform af = graphics.getTransform();
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
            graphics.drawString(robot.getAction().get().getName().toString(), (int) robot.getRobot().getX(), (int) (-1 * (robot.getRobot().getY() - radius * 2)));
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
        }
    }
}