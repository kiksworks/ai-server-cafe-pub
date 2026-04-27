package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.CircleCafe;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.StringCafe;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.updater.UpdaterRefBox;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

public class DrawReferee implements IItemDraw {

    // TODO
    private final Logger LOGGER = Logger.getLogger("drawReferee");

    @Override
    public void init(Class<? extends IContainerCafe> area,
            @Nonnull LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        if (area == VisionArea.class) {
            // penalty kick用
            graphicalComponents.putLast("yellow_penalty_circle", new NoneCafe());
            graphicalComponents.putLast("blue_penalty_circle", new NoneCafe());
            // ball placement用
            graphicalComponents.putLast("blue_ball_target", new NoneCafe());
            graphicalComponents.putLast("yellow_ball_target", new NoneCafe());
            graphicalComponents.putLast("blue_ball_label", new NoneCafe());
            graphicalComponents.putLast("yellow_ball_label", new NoneCafe());
        }
    }

    @Override
    public void update(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents, @Nonnull Config config) {
        if (area != VisionArea.class) {
            return;
        }
        GcRefereeMessage.Referee.Command command = UpdaterRefBox.getInstance().getCurrentCommand();
        boolean worldInvert = config.visibility.invertVisible;
        Field field = UpdaterWorld.getInstance().getField();
        //ペナルティーマーク
        if (command == GcRefereeMessage.Referee.Command.PREPARE_PENALTY_YELLOW) {
            Vector2D pkPos =
                    isInvertView(TeamColor.YELLOW, config) ? field.getFrontPenaltyMark() : field.getBackPenaltyMark();
            // イエローサークルの表示
            graphicalComponents.put("yellow_penalty_circle", new CircleCafe(Color.YELLOW, pkPos.getX(),    // X座標
                    pkPos.getY(),       // Y座標
                    100.0,     // 半径
                    false,     // 塗りつぶさない
                    10.0F      // 線の太さ
            ));
        } else if (command == GcRefereeMessage.Referee.Command.PREPARE_PENALTY_BLUE) {
            // ブルーサークルの表示
            Vector2D pkPos =
                    isInvertView(TeamColor.BLUE, config) ? field.getFrontPenaltyMark() : field.getBackPenaltyMark();
            graphicalComponents.put("blue_penalty_circle", new CircleCafe(Color.BLUE, pkPos.getX(),   // X座標
                    pkPos.getY(),       // Y座標
                    100.0,     // 半径
                    false,     // 塗りつぶさない
                    10.0F      // 線の太さ
            ));
        }

        // ball placement position
        Vector2D targetPosition = UpdaterRefBox.getInstance().getBallPlacePos().get();
        if (worldInvert) {
            targetPosition = targetPosition.negate();
        }
        //青チームのボール目標位置の可視化
        if (command == GcRefereeMessage.Referee.Command.BALL_PLACEMENT_BLUE) {
            graphicalComponents.put("blue_ball_target",
                    new CircleCafe(ColorHelper.ROBOT_BLUE, targetPosition.getX(), targetPosition.getY(), 100, false,
                            50.0F));
            graphicalComponents.put("blue_ball_label",
                    new StringCafe(ColorHelper.ROBOT_BLUE, (int) targetPosition.getX() - -100,
                            (int) targetPosition.getY(), "Blue Target", 150)); // 目標位置のラベル表示
        }

        // 黄チームのボール目標位置の可視化
        if (command == GcRefereeMessage.Referee.Command.BALL_PLACEMENT_YELLOW) {
            graphicalComponents.put("yellow_ball_target",
                    new CircleCafe(ColorHelper.ROBOT_YELLOW, targetPosition.getX(), targetPosition.getY(), 100, false,
                            50.0F));
            graphicalComponents.put("yellow_ball_label",
                    new StringCafe(ColorHelper.ROBOT_YELLOW, (int) targetPosition.getX() - -100,
                            (int) targetPosition.getY(), "Yellow Target", 150)); // 目標位置のラベル表
        }
    }

    @Override
    public RecordData1 putRecord(@NotNull RecordData1 layer) {
        final UpdaterRefBox refBox = UpdaterRefBox.getInstance();
        layer.setRefereeData(refBox.getMessagesAndPop().get());
        return layer;
    }
}
