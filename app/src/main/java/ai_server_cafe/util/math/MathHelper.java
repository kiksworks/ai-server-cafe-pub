package ai_server_cafe.util.math;

import ai_server_cafe.model.field.FieldObject;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MathHelper {
    public static final double TWO_PI = 2.0 * FastMath.PI;
    public static final double HALF_PI = 0.5 * FastMath.PI;
    public static final double PI = FastMath.PI;
    public static final double EPSILON = 1000.0 * 1000.0 / Double.MAX_VALUE;
    public static final double DELTA = 1e-12;

    public static final Vector3D AXIS_X = new Vector3D(1.0, 0.0, 0.0);
    public static final Vector3D AXIS_Y = new Vector3D(0.0, 1.0, 0.0);
    public static final Vector3D AXIS_Z = new Vector3D(0.0, 0.0, 1.0);

    public static boolean isDelta(double value) {
        return FastMath.abs(value) < DELTA;
    }
    public static double wrap2PI(double theta) {
        double wrapped = MathHelper.mod(theta, TWO_PI);
        if (wrapped < 0) {
            wrapped += TWO_PI;
        }
        return wrapped;
    }

    public static double wrapPI(double theta) {
        double wrapped = MathHelper.mod(theta, TWO_PI);
        if (wrapped > FastMath.PI) {
            wrapped -= TWO_PI;
        } else if (wrapped <= -FastMath.PI) {
            wrapped += TWO_PI;
        }
        return wrapped;
    }

    public static double min(@Nonnull double... doubles) {
        double result = Double.MAX_VALUE;
        for (double d : doubles) {
            result = FastMath.min(d, result);
        }
        return result;
    }

    public static double max(@Nonnull double... doubles) {
        double result = -Double.MAX_VALUE;
        for (double d : doubles) {
            result = FastMath.max(d, result);
        }
        return result;
    }

    /**
     *
     * @param theta1
     * @param theta2
     * @return abs(wrapPI(theta1 - theta2))
     */
    public static double inferiorAngle(double theta1, double theta2) {
        return FastMath.abs(MathHelper.wrapPI(theta1 - theta2));
    }

    /**
     *
     * @param vector1
     * @param vector2
     * @return abs(wrapPI(angle(vector1, vector2)))
     */
    public static double inferiorAngle(Vector2D vector1, Vector2D vector2) {
        return MathHelper.inferiorAngle(MathHelper.direction(vector1), MathHelper.direction(vector2));
    }

    /**
     * 基準からの角度
     * @param argTheta
     * @param refTheta
     * @return wrapPi(argTheta - refTheta)
     */
    public static double directionFrom(double argTheta, double refTheta) {
        return MathHelper.wrapPI(argTheta - refTheta);
    }

    /**
     * 基準からの角度
     * @param argVector
     * @param refVector
     * @return wrapPi(direction(argTheta) - direction(refTheta))
     */
    public static double directionFrom(Vector2D argVector, Vector2D refVector) {
        return MathHelper.directionFrom(MathHelper.direction(argVector), MathHelper.direction(refVector));
    }

    public static double direction(Vector2D vector) {
        return FastMath.atan2(vector.getY(), vector.getX());
    }

    /**
     *
     * @param end
     * @param start
     * @return (end - start)のx軸からの角度
     */
    public static double direction(Vector2D end, Vector2D start) {
        return MathHelper.direction(end.subtract(start));
    }

    /**
     *
     * @param theta1
     * @param theta2
     * @return theta1がtheta2の左側に見えるか
     */
    public static boolean isLeftOf(double theta1, double theta2) {
        return wrapPI(theta1 - theta2) > 0;
    }

    /**
     *
     * @param theta1
     * @param theta2
     * @return theta1がtheta2の右側に見えるか
     */
    public static boolean isRightOf(double theta1, double theta2) {
        return wrapPI(theta1 - theta2) < 0;
    }

    /**
     * @param a double
     * @param b double
     * @return double a % b
     */
    public static double mod(double a, double b) {
        return a - (int) (a / b) * b;
    }

    /**
     * @param a line1point1
     * @param b line1point2
     * @param c line2point1
     * @param d line2point2
     * @return intersectionPoint 2直線の交点
     */
    public static Vector2D intersection(Vector2D a, Vector2D b, Vector2D c, Vector2D d) {
        double denominator =
                (a.getX() - b.getX()) * (c.getY() - d.getY()) - (a.getY() - b.getY()) * (c.getX() - d.getX());
        if (denominator == 0) {
            return new Vector2D(-900, -900);
        }
        double px = ((a.getX() * b.getY() - a.getY() * b.getX()) * (c.getX() - d.getX()) -
                (a.getX() - b.getX()) * (c.getX() * d.getY() - c.getY() * d.getX())) / denominator;
        double py = ((a.getX() * b.getY() - a.getY() * b.getX()) * (c.getY() - d.getY()) -
                (a.getY() - b.getY()) * (c.getX() * d.getY() - c.getY() * d.getX())) / denominator;
        return new Vector2D(px, py);
    }

    @Nonnull
    public static Vector2D applyRotation2D(@Nonnull Vector2D vector2D, double radian) {
        Rotation rot = new Rotation(MathHelper.AXIS_Z, radian, RotationConvention.VECTOR_OPERATOR);
        Vector3D vector3D = rot.applyTo(new Vector3D(vector2D.getX(), vector2D.getY(), 0.0));
        return new Vector2D(vector3D.getX(), vector3D.getY());
    }

    @Nonnull
    public static Vector3D applyRotation(@Nonnull Vector3D vector3D, double radian, Vector3D axis) {
        Rotation rot = new Rotation(axis, radian, RotationConvention.VECTOR_OPERATOR);
        return rot.applyTo(vector3D);
    }

    @Nonnull
    public static RealMatrix makeIdentity(int rows, int columns) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix.setEntry(i, j, i == j ? 1.0 : 0.0);
            }
        }
        return matrix;
    }

    @Nonnull
    public static RealMatrix makeDiagonal(@Nonnull double[] values) {
        RealMatrix matrix = MathHelper.makeFill(values.length, values.length, 0.0);
        for (int i = 0; i < values.length; i++) {
            matrix.setEntry(i, i, values[i]);
        }
        return matrix;
    }

    @Nonnull
    public static RealMatrix makeVectorMatrix(@Nonnull double[] values) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(values.length, 1);
        matrix.setColumn(0, values);
        return matrix;
    }

    @Nonnull
    public static RealMatrix makeVectorMatrix(@Nonnull Vector3D values) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(values.getSpace().getDimension(), 1);
        matrix.setColumn(0, values.toArray());
        return matrix;
    }

    @Nonnull
    public static RealMatrix makeVectorMatrix(@Nonnull Vector2D values) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(values.getSpace().getDimension(), 1);
        matrix.setColumn(0, values.toArray());
        return matrix;
    }

    @Nonnull
    public static RealMatrix makeFill(int rows, int columns, double filler) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix.setEntry(i, j, filler);
            }
        }
        return matrix;
    }

    /**
     * @apiNote  CompensatedSingularValueDecompositionはUSV^Tで特異値分解する．
     *           getVで転置前の直交行列が取得できる．
     * @implNote matrixを特異値分解して擬似逆行列を求める
     * @return   matrixの擬似逆行列を取得する
     */
    public static RealMatrix getSVDInverseMatrix(RealMatrix matrix) {
        CompensatedSingularValueDecomposition singular = new CompensatedSingularValueDecomposition(matrix);
        // matrixの特異行列の逆行列を取得する
        RealMatrix SP = MatrixUtils.createRealMatrix(singular.getS().getData()).transpose();
        for (int i = 0; i < FastMath.min(matrix.getRowDimension(), matrix.getColumnDimension()); i++) {
            double sigma = SP.getEntry(i, i);
            SP.setEntry(i, i, sigma == 0.0 ? 0.0 : 1.0 / sigma);
        }
        return (singular.getV().multiply(SP)).multiply(singular.getU().transpose());
    }

    @Nonnull
    public static RealMatrix getProjectionMatrix(@Nonnull RealMatrix matrix) {
        return matrix.multiply(MatrixUtils.inverse(matrix.transpose().multiply(matrix))).multiply(matrix.transpose());
    }

    @Nonnull
    public static RealMatrix getCompensated(@Nonnull RealMatrix Q) {
        if (Q.getColumnDimension() >= Q.getRowDimension()) {
            return Q;
        }
        RealMatrix I = MathHelper.makeIdentity(Q.getRowDimension(), Q.getRowDimension());
        RealMatrix result = MatrixUtils.createRealMatrix(Q.getData());
        for (int i = 0; i < Q.getRowDimension() - Q.getColumnDimension(); i++) {
            List<Pair<RealMatrix, Double>> pairs = new ArrayList<>();
            for (int j = 0; j < I.getColumnDimension(); j++) {
                RealMatrix vec = I.getColumnMatrix(j).subtract(getProjectionMatrix(result).multiply(I.getColumnMatrix(j)));
                double normSq = vec.transpose().multiply(vec).getEntry(0, 0);
                pairs.add(new Pair<>(vec, normSq));
            }
            Optional<Pair<RealMatrix, Double>> optCompensatedVec = pairs.stream().max(InterfaceHelper.getComparator(
                    new IFuncParam1<Double, Pair<RealMatrix, Double>>() {
                @Override
                public Double function(Pair<RealMatrix, Double> realMatrixDoublePair) {
                    return realMatrixDoublePair.getSecond();
                }
            }));
            if (optCompensatedVec.isPresent()) {
                double normSq = optCompensatedVec.get().getSecond();
                RealMatrix lastResult = MatrixUtils.createRealMatrix(result.getData());
                result = MathHelper.makeFill(Q.getRowDimension(), Q.getColumnDimension() + i + 1, 0.0);
                for (int i0 = 0; i0 < lastResult.getColumnDimension(); i0++) {
                    result.setColumn(i0, lastResult.getColumn(i0));
                }
                result.setColumnMatrix(Q.getColumnDimension() + i, optCompensatedVec.get().getFirst().scalarMultiply(1.0 / FastMath.sqrt(normSq)));
            }
        }
        return result;
    }

    public static boolean isEpsilon(double delta) {
        return FastMath.abs(delta) < EPSILON;
    }

    public static boolean isInfinity(double value) {
        return Double.POSITIVE_INFINITY == value || Double.NEGATIVE_INFINITY == value;
    }

    public static boolean isNaN(double value) {
        return Double.isNaN(value);
    }

    public static boolean isNanOrInfinity(double value) {
        return isInfinity(value) || isNaN(value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends FieldObject> T invert(T t, boolean inverse) {
        try {
            if (inverse) {
                FieldObject invert = t.invert();
                if (t.getClass().isInstance(invert)) {
                    return (T) invert;
                } else {
                    throw new ClassCastException(
                            invert.getClass().getName() + " is not instance of " + t.getClass().getName());
                }
            }
            FieldObject copy = t.clone();
            if (t.getClass().isInstance(copy)) {
                return (T) copy;
            } else {
                throw new ClassCastException(
                        copy.getClass().getName() + " is not instance of " + t.getClass().getName());
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static Vector2D position2D(@Nonnull Object o) {
        try {
            Object x = (o.getClass().getMethod("getX").invoke(o));
            Object y = (o.getClass().getMethod("getY").invoke(o));
            double dx = x instanceof Float ? (double) (float) x : (double) x;
            double dy = y instanceof Float ? (double) (float) y : (double) y;
            return new Vector2D(dx, dy);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * zが存在するオブジェクトに対して x, y, z のベクトルを返す
     * @param o
     * @return
     */
    @Nonnull
    public static Vector3D position3D(@Nonnull Object o) {
        try {
            Object x = (o.getClass().getMethod("getX").invoke(o));
            Object y = (o.getClass().getMethod("getY").invoke(o));
            Object z = (o.getClass().getMethod("getZ").invoke(o));
            double dx = x instanceof Float ? (double) (float) x : (double) x;
            double dy = y instanceof Float ? (double) (float) y : (double) y;
            double dz = z instanceof Float ? (double) (float) z : (double) z;
            return new Vector3D(dx, dy, dz);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static Pair<Vector2D, Double> position2DTheta(@Nonnull Object o) {
        try {
            Object x = o.getClass().getMethod("getX").invoke(o);
            Object y = o.getClass().getMethod("getY").invoke(o);
            Object theta = o.getClass().getMethod("getTheta").invoke(o);
            double dx = x instanceof Float ? (double) (float) x : (double) x;
            double dy = y instanceof Float ? (double) (float) y : (double) y;
            double dTheta = theta instanceof Float ? (double) (float) theta : (double) theta;
            return new Pair<>(new Vector2D(dx, dy), dTheta);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static double distance2D(@Nonnull Object o1, @Nonnull Object o2) {
        return position2D(o1).subtract(position2D(o2)).getNorm();
    }

    public static double distance3D(@Nonnull Object o1, @Nonnull Object o2) {
        return position3D(o1).subtract(position3D(o2)).getNorm();
    }

    @Nonnull
    public static Vector2D velocity2D(@Nonnull Object o) {
        try {
            Object x = o.getClass().getMethod("getVx").invoke(o);
            Object y = o.getClass().getMethod("getVy").invoke(o);
            double dx = x instanceof Float ? (double) (float) x : (double) x;
            double dy = y instanceof Float ? (double) (float) y : (double) y;
            return new Vector2D(dx, dy);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * vzが存在するオブジェクトに対して vx, vy, vz のベクトルを返す
     *
     * @param o
     * @return
     */
    @Nonnull
    public static Vector3D velocity3D(@Nonnull Object o) {
        try {
            Object x = (o.getClass().getMethod("getVx").invoke(o));
            Object y = (o.getClass().getMethod("getVy").invoke(o));
            Object z = (o.getClass().getMethod("getVz").invoke(o));
            double dx = x instanceof Float ? (double) (float) x : (double) x;
            double dy = y instanceof Float ? (double) (float) y : (double) y;
            double dz = z instanceof Float ? (double) (float) z : (double) z;
            return new Vector3D(dx, dy, dz);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static Vector2D getHead2(@Nonnull Vector3D vector3D) {
        return new Vector2D(vector3D.getX(), vector3D.getY());
    }

    @Nonnull
    public static Vector3D setHead2(@Nonnull Vector3D vector3D, @Nonnull Vector2D insert) {
        return new Vector3D(insert.getX(), insert.getY(), vector3D.getZ());
    }

    /**
     * 2つの円の交点
     *
     * @param pos1    円1の中心
     * @param radius1 円1の半径
     * @param pos2    円2の中心
     * @param radius2 円2の半径
     * @return 交点
     */
    @Nonnull
    public static Vector2D[] getIntersections(@Nonnull Vector2D pos1, double radius1, @Nonnull Vector2D pos2,
            double radius2) {
        Vector2D pos12 = pos1.subtract(pos2);
        if (pos12.getNormSq() < MathHelper.EPSILON) {
            return new Vector2D[]{};
        }
        Vector2D dPos12 = MathHelper.normalized(pos12).scalarMultiply(radius2);
        double theta = FastMath.atan(radius1 / radius2);
        return new Vector2D[]{MathHelper.applyRotation2D(dPos12, theta).add(pos2),
                MathHelper.applyRotation2D(dPos12, -theta).add(pos2)};
    }

    /**
     * 線分に点から下ろした垂線が引ける場合その交点を返す
     *
     * @param start  線分の開始地点
     * @param end    線分の終了地点
     * @param target 任意の点
     * @return 引ける場合その交点 引けない場合Optional#empty
     */
    public static Optional<Vector2D> getPerpendicularPoint(@Nonnull Vector2D start, @Nonnull Vector2D end,
            @Nonnull Vector2D target) {
        if (end.subtract(start).getNormSq() < MathHelper.EPSILON) {
            return Optional.empty();
        }
        Vector2D p = MathHelper.normalized(end.subtract(start));
        Vector2D result = p.scalarMultiply(target.subtract(start).dotProduct(p)).add(start);
        // 外側にあるときはempty
        if (end.subtract(start).getNormSq() < start.subtract(result).getNormSq() ||
                end.subtract(start).getNormSq() < end.subtract(result).getNormSq()) return Optional.empty();
        return Optional.of(result);
    }

    /**
     * 方向ベクトルから正側負側の単位法線ベクトルを求める
     *
     * @param p 方向ベクトル
     * @return 単位法線ベクトル
     */
    @Nonnull
    public static Vector2D[] getPerpendicularVectors(@Nonnull Vector2D p) {
        if (p.getNormSq() < MathHelper.EPSILON) return new Vector2D[]{};
        return new Vector2D[]{MathHelper.applyRotation2D(MathHelper.normalized(p), -HALF_PI),
                MathHelper.applyRotation2D(MathHelper.normalized(p), HALF_PI)};
    }

    public static boolean isCollidedWithSegment(@Nonnull Vector2D start, @Nonnull Vector2D end,
            @Nonnull Vector2D target, double margin) {
        Optional<Vector2D> pp = getPerpendicularPoint(start, end, target);
        return (pp.isPresent() && pp.get().subtract(target).getNorm() <= margin) ||
                start.subtract(target).getNorm() <= margin || end.subtract(target).getNorm() <= margin;
    }

    @Nonnull
    public static Vector2D[] getContactWithSegment(@Nonnull Vector2D start, @Nonnull Vector2D end,
            @Nonnull Vector2D target, double margin) {
        if (isCollidedWithSegment(start, end, target, margin)) return new Vector2D[]{};
        List<Vector2D> candidates = new ArrayList<>();
        double d1 = FastMath.sqrt(start.subtract(target).getNormSq() - margin * margin);
        Vector2D[] contact1 = getIntersections(start, margin, target, d1);
        if (end.subtract(start).getNormSq() < MathHelper.EPSILON) {
            return contact1;
        }
        double d2 = FastMath.sqrt(end.subtract(target).getNormSq() - margin * margin);
        Vector2D[] contact2 = getIntersections(end, margin, target, d2);
        for (Vector2D vector2D : contact1) {
            if (getPerpendicularPoint(start, end, vector2D).isEmpty()) {
                candidates.add(vector2D);
            }
        }
        for (Vector2D vector2D : contact2) {
            if (getPerpendicularPoint(start, end, vector2D).isEmpty()) {
                candidates.add(vector2D);
            }
        }
        if (candidates.size() < 2) return new Vector2D[]{};
        //throw new RuntimeException(new ArrayIndexOutOfBoundsException("candidates size is less than 2"));
        return new Vector2D[]{candidates.get(0), candidates.get(1)};
    }

    public static boolean isCollidedWithBox(@Nonnull Vector2D min, @Nonnull Vector2D max, @Nonnull Vector2D target,
            double margin) {
        Vector2D minX = new Vector2D(min.getX(), max.getY());
        Vector2D minY = new Vector2D(max.getX(), min.getY());
        boolean isIn = min.getX() <= target.getX() && target.getX() <= max.getX() && min.getY() <= target.getY() &&
                target.getY() <= max.getY();
        boolean isInSide =
                isCollidedWithSegment(min, minX, target, margin) || isCollidedWithSegment(min, minY, target, margin) ||
                        isCollidedWithSegment(minX, max, target, margin) ||
                        isCollidedWithSegment(minY, max, target, margin);
        return isIn || isInSide;
    }

    @Nonnull
    public static Vector2D[] getContactWithBox(@Nonnull Vector2D min, @Nonnull Vector2D max, @Nonnull Vector2D target,
            double margin) {
        if (isCollidedWithBox(min, max, target, margin)) return new Vector2D[]{};
        Vector2D minX = new Vector2D(min.getX(), max.getY());
        Vector2D minY = new Vector2D(max.getX(), min.getY());
        List<Vector2D> candidates = new ArrayList<>();
        Vector2D[] contact0 = getIntersections(min, margin, target,
                FastMath.sqrt(min.subtract(target).getNormSq() - margin * margin));
        Vector2D[] contact1 = getIntersections(minX, margin, target,
                FastMath.sqrt(minX.subtract(target).getNormSq() - margin * margin));
        Vector2D[] contact2 = getIntersections(minY, margin, target,
                FastMath.sqrt(minY.subtract(target).getNormSq() - margin * margin));
        Vector2D[] contact3 = getIntersections(max, margin, target,
                FastMath.sqrt(max.subtract(target).getNormSq() - margin * margin));
        for (Vector2D vector2D : contact0) {
            if (!(min.getX() <= vector2D.getX() && vector2D.getX() <= max.getX() ||
                    min.getY() <= vector2D.getY() && vector2D.getY() <= max.getY())) {
                candidates.add(vector2D);
            }
        }
        for (Vector2D vector2D : contact1) {
            if (!(min.getX() <= vector2D.getX() && vector2D.getX() <= max.getX() ||
                    min.getY() <= vector2D.getY() && vector2D.getY() <= max.getY())) {
                candidates.add(vector2D);
            }
        }
        for (Vector2D vector2D : contact2) {
            if (!(min.getX() <= vector2D.getX() && vector2D.getX() <= max.getX() ||
                    min.getY() <= vector2D.getY() && vector2D.getY() <= max.getY())) {
                candidates.add(vector2D);
            }
        }
        for (Vector2D vector2D : contact3) {
            if (!(min.getX() <= vector2D.getX() && vector2D.getX() <= max.getX() ||
                    min.getY() <= vector2D.getY() && vector2D.getY() <= max.getY())) {
                candidates.add(vector2D);
            }
        }
        if (candidates.size() < 2)
            throw new RuntimeException(new ArrayIndexOutOfBoundsException("candidates size is less than 2"));
        return new Vector2D[]{candidates.get(0), candidates.get(1)};
    }

    /**
     *
     * @param min
     * @param max
     * @param start
     * @param end
     * @return startから [近い方, 遠い方]
     */
    @Nonnull
    @Deprecated
    public static Optional<Pair<Vector2D, Vector2D>> clippingWithBox(@Nonnull Vector2D min, @Nonnull Vector2D max,
            @Nonnull Vector2D start, @Nonnull Vector2D end) {
        Vector2D es = end.subtract(start);
        double theta = FastMath.atan2(es.getY(), es.getX());
        List<Double> kList = new ArrayList<>();
        double minK = es.getNorm();
        double maxK = 0;
        kList.add((min.getX() - start.getX()) / FastMath.cos(theta));
        kList.add((max.getX() - start.getX()) / FastMath.cos(theta));
        kList.add((min.getY() - start.getY()) / FastMath.sin(theta));
        kList.add((max.getY() - start.getY()) / FastMath.sin(theta));
        kList.add(0.0);
        kList.add(es.getNorm());
        boolean flag = false;
        for (double k : kList) {
            if (!isNanOrInfinity(k)) {
                if (0.0 <= k && k <= es.getNorm()) {
                    Vector2D candidate = MathHelper.normalized(es).scalarMultiply(k).add(start);
                    if (min.getX() + EPSILON < candidate.getX() && candidate.getX() < max.getX() - EPSILON ||
                            min.getY() + EPSILON < candidate.getY() && candidate.getY() < max.getY() - EPSILON) {
                        minK = FastMath.min(minK, k);
                        maxK = FastMath.min(maxK, k);
                        flag = true;
                    }
                }
            }
        }
        if (!flag) {
            return Optional.empty();
        }
        return Optional.of(new Pair<>(MathHelper.normalized(es).scalarMultiply(minK).add(start),
                MathHelper.normalized(es).scalarMultiply(maxK).add(start)));
    }

    @Nonnull
    public static Vector2D getFromPolar(double r, double theta) {
        return MathHelper.applyRotation2D(new Vector2D(r, 0.0), theta);
    }

    public static Vector2D normalized(@Nonnull Vector2D vector2D) {
        try {
            return vector2D.normalize();
        } catch (ArithmeticException e) {
            return Vector2D.ZERO;
        }
    }

    public static Vector3D normalized(@Nonnull Vector3D vector3D) {
        try {
            return vector3D.normalize();
        } catch (ArithmeticException e) {
            return Vector3D.ZERO;
        }
    }

    /**
     *
     * @param position
     * @param line1    直線上の点
     * @param line2    直線上の点
     * @return line1とline2を結ぶ直線から、positionまでの距離
     */
    public static double distancePositionToLine(Vector2D position, Vector2D line1, Vector2D line2) {
        return distance2D(MathHelper.getPerpendicularPoint(line2, line1, position), position);
    }

    /**
     *
     * @param position
     * @param robot
     * @return positionからrobotへの距離
     */
    public static double distancePositionToRobot(Vector2D position, FilteredRobot robot) {
        return position.subtract(position2D(robot)).getNorm();
    }

    /**
     *
     * @param position
     * @param robotMap notEmpty
     * @return positionに一番近いロボットの id, robot
     */
    public static Optional<IntegratedRobot> nearestRobotToPosition(@Nonnull Map<Integer, IntegratedRobot> robotMap,
            Vector2D position) {
        Optional<IntegratedRobot> minRobot = robotMap.values().stream()
                .min(InterfaceHelper.getComparator(new IFuncParam1<Double, IntegratedRobot>() {
                    @Override
                    public Double function(IntegratedRobot robot1) {
                        return position2D(robot1.getRobot()).subtract(position).getNormSq();
                    }
                }));
        return minRobot;
    }

    /**
     *
     * @param position
     * @param robotList notEmpty
     * @return positionに一番近いロボットの id, robot
     */
    public static Optional<IntegratedRobot> nearestRobotToPosition(@Nonnull List<IntegratedRobot> robotList,
            Vector2D position) {
        Optional<IntegratedRobot> minRobot = robotList.stream()
                .min(InterfaceHelper.getComparator(new IFuncParam1<Double, IntegratedRobot>() {
                    @Override
                    public Double function(IntegratedRobot robot1) {
                        return position2D(robot1.getRobot()).subtract(position).getNormSq();
                    }
                }));
        return minRobot;
    }

    /**
     *
     * @param positions notEmpty
     * @param position
     * @return positionsのうちpositionに一番近いもの
     */
    public static Optional<Vector2D> nearestPositionToPosition(@Nonnull List<Vector2D> positions, Vector2D position) {
        Optional<Vector2D> nearest =
                positions.stream().min(InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
                    @Override
                    public Double function(Vector2D vector2D) {
                        return vector2D.subtract(position).getNormSq();
                    }
                }));
        return nearest;
    }

    /**
     *
     * @param positions notEmpty
     * @param robot
     * @return positionsのうちrobotに一番近いもの
     */
    public static Optional<Vector2D> nearestPositionToRobot(List<Vector2D> positions, IntegratedRobot robot) {
        return nearestPositionToPosition(positions, position2D(robot.getRobot()));
    }

    /**
     *
     * @param scoreMap [robot, score]
     * @return スコアが一番大きいロボット
     */
    public static Optional<IntegratedRobot> robotWithMaxScore(Map<IntegratedRobot, Double> scoreMap) {
        return scoreMap.keySet().stream()
                .max(InterfaceHelper.getComparator(new IFuncParam1<Double, IntegratedRobot>() {
                    @Override
                    public Double function(IntegratedRobot robot1) {
                        return scoreMap.get(robot1);
                    }
                }));
    }

    /**
     * スコアの大きい順にソート
     *
     * @param scoreMap [robot, score]
     * @return ソート後のロボットのリスト
     */
    public static List<IntegratedRobot> sortInGreaterScore(Map<IntegratedRobot, Double> scoreMap) {
        return scoreMap.keySet().stream()
                .sorted(InterfaceHelper.getComparator(new IFuncParam1<Double, IntegratedRobot>() {
                    @Override
                    public Double function(IntegratedRobot robot1) {
                        return scoreMap.get(robot1);
                    }
                })).toList().reversed();
    }

    /**
     * スコアの大きい順にソート
     *
     * @param scoreMap [position, score]
     * @return ソート後のpositionのリスト
     */
    public static List<Vector2D> sortPositionsInGreaterScore(Map<Vector2D, Double> scoreMap) {
        return scoreMap.keySet().stream()
                .sorted(InterfaceHelper.getComparator(new IFuncParam1<Double, Vector2D>() {
                    @Override
                    public Double function(Vector2D position1) {
                        return scoreMap.get(position1);
                    }
                })).toList().reversed();
    }

    public static List<IntegratedRobot> robotListFromIds(Map<Integer, IntegratedRobot> robotMap, List<Integer> ids) {
        return robotMap.values().stream()
                .filter(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                    @Override
                    public Boolean function(IntegratedRobot robot) {
                        return ids.contains(robot.getId());
                    }
                })).toList();
    }

    @Nonnull
    public static int[] toArray(@Nonnull List<Integer> target) {
        final int[] result = new int[target.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = target.get(i);
        }
        return result;
    }

    @Nonnull
    public static List<Integer> toList(@Nonnull int[] value) {
        List<Integer> result = new ArrayList<>();
        for (int i : value) {
            result.add(i);
        }
        return result;
    }

    @Nonnull
    public static <K, V> Map.Entry<K, V> get(@Nonnull LinkedHashMap<K, V> map, int index) {
        int i = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (i == index) return entry;
            i++;
        }
        throw new IndexOutOfBoundsException();
    }

    public static double getCos(Vector2D v1, Vector2D v2) {
        return MathHelper.normalized(v1).dotProduct(MathHelper.normalized(v2));
    }

    public static double getSin(Vector2D v1, Vector2D v2) {
        double cos = getCos(v1, v2);
        return FastMath.sqrt(1.0 - cos * cos);
    }

    public static double clamp(double value, double min, double max) {
        return FastMath.min(FastMath.max(value, min), max);
    }

    public static int clamp(int value, int min, int max) {
        return FastMath.min(FastMath.max(value, min), max);
    }

    public static double getTrapezoidalTime(double v0, double r, double accel, double brake, double maxVel) {
        double baseR = v0 > 0.0 ? r + 0.5 * v0 * v0 / accel : r + 0.5 * v0 * v0 / brake;
        double baseT = getTrapezoidalTime(baseR, accel, brake, maxVel);
        double finalT = baseT + ((v0 > 0.0) ? -v0 / accel : v0 / brake);
        if (finalT < 0.0) {
            if (r <= 0.0) {
                return 0.0;
            }
            return getTrapezoidalTime(-v0, -r, accel, brake, maxVel);
        }
        return finalT;
    }

    public static double getTrapezoidalTime(double r, double accel, double brake, double maxVel) {
        double maxTriangle = 0.5 * maxVel * maxVel / accel + 0.5 * maxVel * maxVel / brake;
        if (r <= maxTriangle) {
            return Math.sqrt(2.0 * r / (accel + brake));
        }
        return maxVel / accel + maxVel / brake + (r - maxTriangle) / maxVel;
    }

    public static int[] getColorId(Color color) {
        return new int[]{color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()};
    }

    public static Color getColor(int[] color) {
        return new Color(color[0], color[1], color[2], color[3]);
    }

    /**
     *
     * @param world    world
     * @param position cafeでの座標
     * @return starLabでの座標
     * @see <a href="https://openstarlab.readthedocs.io/en/latest/Pre_Processing/Sports/Event_data/Data_Format/Football/UEID.html">...</a>
     */
    public static Vector2D getStarLabCoordinate(World world, Vector2D position) {
        final double width = world.getField().getGameWidth();
        final double height = world.getField().getGameHeight();
        final double rawX = position.getX();
        final double rawY = position.getY();
        return new Vector2D(52.5 + rawX * 105 / width, 34 - rawY * 68 / height);
    }

    /**
     *
     * @param world world
     * @param x     starLabでのx座標
     * @param y     starLabでのy座標
     * @return cafeでの座標
     * @see <a href="https://openstarlab.readthedocs.io/en/latest/Pre_Processing/Sports/Event_data/Data_Format/Football/UEID.html">...</a>
     */
    public static Vector2D getCafeCoordinate(World world, double x, double y) {
        final double width = world.getField().getGameWidth();
        final double height = world.getField().getGameHeight();
        return new Vector2D((x / 105 - 0.5) * width, (y / 68 + 0.5) * height);
    }

    /**
     * @return F(Sin^(-1)(x), -1)
     */
    public static double ellipticFArcsinApprox(double x) {
        double a = 6.7527803204762;
        return -FastMath.log(FastMath.exp(a * (1.0 - x)) - 1.0) / a + FastMath.log(FastMath.exp(a) - 1.0) / a;
    }

    public static double invertFArcsinApprox(double x) {
        double a = 6.7527803204762;
        return FastMath.log((FastMath.exp(a) - 1.0) * FastMath.exp(-a * x) + 1.0) / a + 1.0;
    }

    public static double sinc(double x) {
        double result = FastMath.sin(x) / x;
        return Double.isNaN(result) ? 1.0 : result;
    }

    public static double getPhi(double target, double theOther, double theta) {
        double rawPhi = FastMath.atan(theOther / target);
        if (FastMath.abs(theta) <= MathHelper.HALF_PI) {
            return theta * rawPhi / MathHelper.HALF_PI;
        } else {
            return FastMath.copySign((MathHelper.PI - FastMath.abs(theta)) * rawPhi / MathHelper.HALF_PI +
                    (FastMath.abs(theta) - MathHelper.HALF_PI), theta);
        }
    }

    public static double sigmoid(double alpha, double value) {
        return 1.0 / (1.0 + FastMath.exp(-alpha * value));
    }

    public static double sigmoidInverse(double alpha, double value) {
        return FastMath.log(value / (1.0 - value)) / alpha;
    }
}
