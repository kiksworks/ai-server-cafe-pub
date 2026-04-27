package ai_server_cafe.util.math;

import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;

public class Equation {
    @Nonnull
    public static double[] realRoots2dim(double a, double b, double c) {
        if (a == 0.0) return new double[]{};
        double d = b * b - 4.0 * a * c;
        if (d < 0.0) return new double[]{};
        if (d == 0.0) return new double[]{-b * 0.5 / a};
        return new double[]{-b * 0.5 / a + FastMath.sqrt(d) * 0.5 / a, -b * 0.5 / a - FastMath.sqrt(d) * 0.5 / a};
    }

    @Nonnull
    public static double[] realRoots3dim(double a, double b, double c, double d) {
        if (a == 0.0) return new double[]{};
        double x = 3.0 * (27.0 * a * a * d * d - 18.0 * a * b * c * d + 4.0 * b * b * b * d +
                4.0 * a * c * c * c - b * b * c * c);
        double y = -2.0 * b * b * b + 9.0 * a * b * c - 27.0 * a * a * d;
        if (x < 0) {
            double cos = y / (54.0 * a * a * a);
            double sin = FastMath.sqrt(-x) / (18.0 * a * a);
            double theta = FastMath.atan2(sin, cos);
            double r = FastMath.sqrt(cos * cos + sin * sin);
            double theta3 = theta / 3.0;
            double r3 = FastMath.cbrt(r);
            double alpha = r3 * FastMath.cos(theta3);
            double beta = r3 * FastMath.sin(theta3);
            double v = -b / (3.0 * a) + 2.0 * alpha;
            double v1 = -b / (3.0 * a) - alpha + FastMath.sqrt(3.0) * beta;
            double v2 = -b / (3.0 * a) - alpha - FastMath.sqrt(3.0) * beta;
            if (alpha == 0.0) {
                if (beta == 0.0) return new double[]{-b / (3.0 * a)};
            } else if (beta == 0.0) {
                return new double[]{v, -b / (3.0 * a) - alpha};
            } else if (-alpha - FastMath.sqrt(3.0) * beta == 2.0 * alpha) {
                return new double[]{v, v1};
            } else if (-alpha + FastMath.sqrt(3.0) * beta == 2.0 * alpha) {
                return new double[]{v, v2};
            }
            return new double[]{v, v2,
                    v1};
        } else {
            double alpha = y / (54.0 * a * a * a);
            double beta = FastMath.sqrt(x) / (18.0 * a * a);
            return new double[]{-b / (3.0 * a) + FastMath.cbrt(alpha + beta) + FastMath.cbrt(alpha - beta)};
        }
    }

    @Nonnull
    public static double[] realRoots4dim(double a, double b, double c, double d,
                                         double e) {
        if (a == 0.0) return new double[]{};
        double p = (8.0 * a * c - 3.0 * b * b) / (8.0 * a * a);
        double q = (b * b * b - 4.0 * a * b * c + 8.0 * a * a * d) / (8.0 * a * a * a);
        double r =
                (-3.0 * b * b * b * b + 16.0 * a * b * b * c - 64.0 * a * a * b * d + 256.0 * a * a * a * e) /
                        (256.0 * a * a * a * a);
        if (q == 0.0) {
            double i = p * p - 4.0 * r;
            if (i < 0.0) {
                return new double[]{};
            } else if (i == 0.0) {
                if (-p < 0.0)
                    return new double[]{};
                else if (p == 0.0)
                    return new double[]{-b / (4.0 * a)};
                else
                    return new double[]{-b / (4.0 * a) - FastMath.sqrt(-p * 0.5), -b / (4.0 * a) + FastMath.sqrt(-p * 0.5)};
            } else {
                if (-p + FastMath.sqrt(i) < 0.0) {
                    return new double[]{};
                } else {
                    double a1 = -p * 0.5 + FastMath.sqrt(i) * 0.5;
                    double v = -b / (4.0 * a) - FastMath.sqrt(a1);
                    double v1 = -b / (4.0 * a) + FastMath.sqrt(a1);
                    if (-p - FastMath.sqrt(i) < 0.0) {
                        if (-p + FastMath.sqrt(i) == 0.0)
                            return new double[]{-b / (4.0 * a)};
                        else
                            return new double[]{v, v1};
                    } else {
                        if (-p - FastMath.sqrt(i) == 0.0)
                            return new double[]{-b / (4.0 * a), v, v1};
                        else {
                            double a2 = -p * 0.5 - FastMath.sqrt(i) * 0.5;
                            return new double[]{v,
                                    -b / (4.0 * a) - FastMath.sqrt(a2),
                                    -b / (4.0 * a) + FastMath.sqrt(a2), v1};
                        }
                    }
                }
            }
        } else {
            double t0 = realRoots3dim(1.0, -p, -4.0 * r, 4.0 * p * r - q * q)[0];
            double f = t0 - p;
            if (f <= 0.0)
                return new double[]{};
            else {
                double g = -t0 - p + 2.0 * q / FastMath.sqrt(f);
                double h = -t0 - p - 2.0 * q / FastMath.sqrt(f);
                double v = -b / (4.0 * a) + 0.5 * FastMath.sqrt(f);
                double v2 = -b / (4.0 * a) - 0.5 * FastMath.sqrt(f);
                double v3 = -b / (4.0 * a) - 0.5 * FastMath.sqrt(f) - 0.5 * FastMath.sqrt(g);
                double v1 = -b / (4.0 * a) + 0.5 * FastMath.sqrt(f) + 0.5 * FastMath.sqrt(h);
                if (q < 0.0) {
                    if (h < 0.0)
                        return new double[]{};
                    else if (g < 0.0) {
                        if (h == 0.0)
                            return new double[]{v};
                        else
                            return new double[]{v - 0.5 * FastMath.sqrt(h),
                                    v1};
                    } else {
                        if (g == 0.0)
                            return new double[]{v - 0.5 * FastMath.sqrt(h),
                                    v2,
                                    v1};
                        else
                            return new double[]{v - 0.5 * FastMath.sqrt(h),
                                    v1,
                                    v3,
                                    v2 + 0.5 * FastMath.sqrt(g)};
                    }
                } else {
                    if (g < 0.0)
                        return new double[]{};
                    else if (h < 0.0) {
                        if (g == 0.0)
                            return new double[]{v2};
                        else
                            return new double[]{v3,
                                    v2 + 0.5 * FastMath.sqrt(g)};
                    } else {
                        if (h == 0.0)
                            return new double[]{v3,
                                    v,
                                    v2 + 0.5 * FastMath.sqrt(g)};
                        else
                            return new double[]{v - 0.5 * FastMath.sqrt(h),
                                    v1,
                                    v3,
                                    v2 + 0.5 * FastMath.sqrt(g)};
                    }
                }
            }
        }
    }
}
