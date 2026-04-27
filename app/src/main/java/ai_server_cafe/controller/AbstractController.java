package ai_server_cafe.controller;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.game.Command;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;

/**
 * 制御器の基底クラス
 */
public abstract class AbstractController {
    /**
     * ルール規定の移動速度
     */
    protected double velocityLimit;

    /**
     * 制御器のコンストラクタ
     * @param maxVel 基礎的な最大速度
     */
    public AbstractController(double maxVel) {
        this.velocityLimit = maxVel;
    }

    /**
     * ゲームルールによる移動速度制限の設定
     * @param limit ルール規定の移動速度
     */
    public void setVelocityLimit(double limit) {
        this.velocityLimit = limit;
    }

    /**
     * @param command コマンド (フィールド基準)
     * @return vx, vy, omega (ロボット基準)
     */
    public Vector3D update(FilteredRobot robot, Field field, @Nonnull Command command) {
        boolean hasPos = command.getTargetPos().isPresent();
        boolean hasTheta = command.getTargetTheta().isPresent();
        if (hasPos && hasTheta) {
            return this.updatePT(robot, field, command.getTargetPos().get(), command.getTargetTheta().get());
        } else if (!hasPos && hasTheta) {
            return this.updateVT(robot, field, command.getTargetVel(), command.getTargetTheta().get());
        } else if (hasPos && !hasTheta) {
            return this.updatePO(robot, field, command.getTargetPos().get(), command.getTargetOmega());
        }
        return this.updateVO(robot, field, command.getTargetVel(), command.getTargetOmega());
    }

    protected abstract Vector3D updatePT(FilteredRobot robot, Field field, Vector2D position, double theta);

    protected abstract Vector3D updateVT(FilteredRobot robot, Field field, Vector2D velocity, double theta);

    protected abstract Vector3D updatePO(FilteredRobot robot, Field field, Vector2D position, double omega);

    protected abstract Vector3D updateVO(FilteredRobot robot, Field field, Vector2D velocity, double omega);
}
