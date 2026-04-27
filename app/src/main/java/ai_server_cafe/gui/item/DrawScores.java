package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.game.strategy.XGType;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.PassScoreCafe;
import ai_server_cafe.records.ver1.PassTargetWrapper;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.records.ver1.StrategyWrapper;
import ai_server_cafe.updater.UpdaterPassTarget;
import ai_server_cafe.updater.UpdaterStrategy;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DrawScores implements IItemDraw {
    @Override
    public void init(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        // PassScore
        graphicalComponents.putLast("blue_pass_score", new NoneCafe());
        graphicalComponents.putLast("yellow_pass_score", new NoneCafe());
        // XGScore
        graphicalComponents.putLast("blue_XG_score", new NoneCafe());
        graphicalComponents.putLast("yellow_XG_score", new NoneCafe());
    }

    @Override
    public void update(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents, @Nonnull Config config) {
        // パス候補のスコアの描画
        if (config.visibility.bluePassScoreVisible) {
            graphicalComponents.put("blue_pass_score", new PassScoreCafe(ColorHelper.ROLE_ARCHIVE_SKY,
                    UpdaterPassTarget.getInstance().getScoreMap(TeamColor.BLUE).get(),
                    isInvertView(TeamColor.BLUE, config)));
        }
        if (config.visibility.yellowPassScoreVisible) {
            graphicalComponents.put("yellow_pass_score", new PassScoreCafe(ColorHelper.ROLE_PURPLE,
                    UpdaterPassTarget.getInstance().getScoreMap(TeamColor.YELLOW).get(),
                    isInvertView(TeamColor.YELLOW, config)));
        }
        // ゴール期待値のスコアの描画//XGTypeで可視化するゴール期待値の種類を変更可。GUIで切り替える用にしたい
        if (config.visibility.blueXGScoreVisible) {
            graphicalComponents.put("blue_XG_score", new PassScoreCafe(ColorHelper.ROLE_BLUE,
                    UpdaterStrategy.getInstance().getXGMap(TeamColor.BLUE, XGType.TYPEC).get(),
                    isInvertView(TeamColor.BLUE, config)));
        }
        if (config.visibility.yellowXGScoreVisible) {
            graphicalComponents.put("yellow_XG_score", new PassScoreCafe(ColorHelper.ROLE_YELLOW,
                    UpdaterStrategy.getInstance().getXGMap(TeamColor.YELLOW, XGType.TYPEC).get(),
                    isInvertView(TeamColor.YELLOW, config)));
        }
    }

    @Override
    public RecordData1 putRecord(@NotNull RecordData1 layer) {
        for(TeamColor color : TeamColor.values()) {
            // ========================= 以下 passTarget =========================
            final UpdaterPassTarget passTarget = UpdaterPassTarget.getInstance();
            final Map<Vector2D, Double> scoreMap = passTarget.getScoreMap(color).get();
            final List<Vector2D> passTargets = passTarget.getPassTargets(color).get();
            final Vector2D kickerPos = passTarget.getKickerPos(color).get();
            layer.setPassScoreData(color, new PassTargetWrapper(scoreMap, passTargets, kickerPos));

            // ========================= 以下 xgScore =========================
            final UpdaterStrategy strategy = UpdaterStrategy.getInstance();
            final Map<Vector2D, Double> xgMapA = strategy.getXGMap(color, XGType.TYPEA).get();
            final Map<Vector2D, Double> xgMapB = strategy.getXGMap(color, XGType.TYPEB).get();
            final Map<Vector2D, Double> xgMapC = strategy.getXGMap(color, XGType.TYPEC).get();
            layer.setXGData(color, new StrategyWrapper(strategy.getAttackDire(color), xgMapA, xgMapB, xgMapC));
        }
        return layer;
    }
}
