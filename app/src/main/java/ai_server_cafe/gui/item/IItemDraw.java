package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;

public interface IItemDraw {
    public void update(Class<? extends IContainerCafe> area, LinkedHashMap<String, IGraphicalComponent> graphicalComponents, Config config);

    public default void init(Class<? extends IContainerCafe> area, LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        LinkedHashMap<String, IGraphicalComponent> map = new LinkedHashMap<>();
        this.update(area, map, ConfigManager.getInstance().getConfig());
        for (String key : map.keySet()) {
            graphicalComponents.putLast(key, new NoneCafe());
        }
    }

    /**
     *
     * @param layer 保存するRecordData1のインスタンス
     * @return 保存後のRecordData1のインスタンス
     */
    public RecordData1 putRecord(@Nonnull RecordData1 layer);

    /**
     * 戦略部(PathPlannerなど)の表示用
     * NegativeSideが自分になるように調整されているItemを表示するときに最終的な反転bool値を取得する
     *
     * @param color   自陣カラー
     * @param config config manager
     * @return 最終的な反転bool値
     */
    public default boolean isInvertView(@Nonnull TeamColor color, @Nonnull Config config) {
        return !config.visibility.invertVisible && ConfigManager.getInstance().isInvert(color) ||
                config.visibility.invertVisible && !ConfigManager.getInstance().isInvert(color);
    }
}
