package ai_server_cafe.filter;

import ai_server_cafe.util.math.MathHelper;

public class PhysicalParameter {
    // ボールの半径[m]
    public static final double BALL_RADIUS = 42.67 / 2000;
    // ボールの断面積 [m^2]
    public static final double BALL_CROSS_SECTIONAL = MathHelper.PI * BALL_RADIUS * BALL_RADIUS;
    // ボールの重さ[kg]
    public static final double BALL_WEIGHT = 45.93 / 1000;
    // 床とボールの摩擦係数
    public static final double FRIC_COEF = 0.04;
    // 重力加速度[m・s^-2]
    public static final double GRAVITY = 9.8;
    // 空気の密度[kg/(m^3)]
    public static final double AIR_DENSITY = 1.2041;
    // 空気の粘度[Pa・s]
    public static final double AIR_VISCOSITY = 1.822e-5;
    // Clift and Grauvinの近似式によるCd から求めた 20Cの空気に対するボールの抵抗係数k F=-kv^2
    // https://cattech-lab.com/science-tools/sphere-cd/
    public static final double AIR_RESISTANCE_COEFFICIENT = 0.00033456428;
}
