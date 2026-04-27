package ai_server_cafe.util.gui;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Random;

public class ColorHelper {
    public static final Color SCREEN_DARK = new Color(50, 50, 50);
    public static final Color LINE_WHITE = new Color(255, 255, 255);
    public static final Color LINE_BLUE = new Color(50, 50, 255);
    public static final Color LINE_YELLOW = new Color(255, 255, 50);
    public  static  final  Color UI_SMOKE = new Color(172,200,227);
    public static final Color WALL_RED = new Color(120, 50, 50);
    public static final Color ROBOT_BLUE = new Color(50, 90, 192);
    public static final Color ROBOT_YELLOW = new Color(192, 192, 20);
    public static final Color WEIGHT_VIOLET = new Color(125, 50, 192, 127);
    public static final Color FIELD_GREEN = new Color(100, 190, 90);
    public static final Color FIELD_DARK = new Color(80, 80, 80);
    public static final Color LATTE_BROWN = new Color(150, 110, 70);
    public static final Color BALL_ORANGE = new Color(220, 140, 10);
    public static final Color TARGET_EMERALD = new Color(50, 220, 150);
    public static final Color SENSOR_CYAN = new Color(0, 255, 255);
    public static final Color VELOCITY_AQUA = new Color(50, 200, 220);
    public static final Color VELOCITY_PEONY = new Color(255, 166, 200);
    public static final Color ARCHIVE_SKY = new Color(90, 180, 250);
    public static final Color SWITCH_GRAY = new Color(120, 120, 120);
    public static final Color GROUND_BLACK = new Color(20, 20, 20);

    public static final Color ROLE_ROSE = new Color(224, 20, 92, 150);
    public static final Color ROLE_RED = new Color(240, 30, 0, 150);
    public static final Color ROLE_ORANGE = new Color(220, 140, 10, 150);
    public static final Color ROLE_YELLOW = new Color(192, 192, 20, 150);
    public static final Color ROLE_GREEN = new Color(50, 230, 50, 150);
    public static final Color ROLE_CYAN = new Color(86, 191, 149, 150);
    public static final Color ROLE_BLUE = new Color(20, 50, 192, 150);
    public static final Color ROLE_PURPLE = new Color(180, 20, 250, 150);
    public static final Color ROLE_PINK = new Color(250, 157, 210, 150);
    public static final Color ROLE_WHITE = new Color(230, 240, 250, 150);
    public static final Color ROLE_STRONG_YELLOW = new Color(255, 255, 0, 255);
    public static final Color ROLE_STRONG_BLUE = new Color(0, 100, 255, 255);
    public static final Color ROLE_ARCHIVE_SKY = new Color(90, 180, 250,120);


    public static final Color CARD_YELLOW = new Color(255, 230, 0);
    public static final Color CARD_RED = new Color(255, 0, 0);

    public static final Color MEMORY_YELLOW = new Color(220, 220, 20, 120);
    public static final Color MEMORY_GREEN = new Color(80, 220, 20, 120);
    public static final Color MEMORY_BLACK = new Color(70, 70, 70);
    public static final Color PATH_GOLD = new Color(220, 180, 10);

    public static final Color OBSTACLE_SCARLET = new Color(120, 50, 50, 100);

    @Nonnull
    public static Color getRandomColor() {
        Random random = new Random();
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}
