package ai_server_cafe.records.ver1;

import ai_server_cafe.game.strategy.XGType;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
/**
 * ReplayとRecordDataのsetterおよびgetterとして使用するクラス
 */
public class StrategyWrapper {
    private final int attackSide;
    private final Map<Vector2D, Double> typeMapA;
    private final Map<Vector2D, Double> typeMapB;
    private final Map<Vector2D, Double> typeMapC;

    public StrategyWrapper(int attackSide, Map<Vector2D, Double> typeMapA,
                           Map<Vector2D, Double> typeMapB, Map<Vector2D, Double> typeMapC) {
        this.attackSide = attackSide;
        this.typeMapA = typeMapA;
        this.typeMapB = typeMapB;
        this.typeMapC = typeMapC;
    }

    public int getAttackSide() {
        return this.attackSide;
    }

    public Map<Vector2D, Double> getXGMap(@Nonnull XGType type) {
        switch (type) {
            case TYPEA -> {
                return this.typeMapA;
            }
            case TYPEB -> {
                return this.typeMapB;
            }
            case TYPEC -> {
                return this.typeMapC;
            }
            default -> {
                return new HashMap<>();
            }
        }
    }

}
