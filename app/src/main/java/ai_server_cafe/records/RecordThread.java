package ai_server_cafe.records;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.IItemDraw;
import ai_server_cafe.gui.registry.RegistryGraphicalComponents;
import ai_server_cafe.records.ver0.GraphicsRecord;
import ai_server_cafe.records.ver0.RecordData0;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.replay.ReplayState;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterRecord;
import ai_server_cafe.updater.UpdaterReplay;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.compressor.CompressHelper;
import ai_server_cafe.util.math.MathHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * RecordCafeの処理を書くクラス
 */
public final class RecordThread extends AbstractLoopThreadCafe {
    private static RecordThread instance = null;
    private double lastDate = 0.0;
    private long commonCycles = 0L;
    // 前tickのisStartの値
    private boolean isLastStart;
    // 現在tickのisStartの値
    private boolean isCurrentStart;

    private RecordThread() {
        super("record-runner");
        this.isLastStart = false;
        this.isCurrentStart = false;
    }

    public static RecordThread getInstance() {
        if(instance == null) {
            instance = new RecordThread();
        }
        return instance;
    }

    @Override
    protected void loop() {
        if (UpdaterReplay.getInstance().getReplayState() != ReplayState.STOP) return;
        double nowDate = TimeHelper.now();
        ConfigManager configManager = ConfigManager.getInstance();
        if (nowDate - this.lastDate >= configManager.getCycleTime()) {
            this.lastDate = nowDate;
            // config.jsonでRecordCafeを使うか設定できる
            if (!ConfigManager.getInstance().getConfig().enableRecord) {
                return;
            }
            this.commonCycles++;
            this.isCurrentStart = ConfigManager.getInstance().isStart();
            try {
                if(this.isLastStart && !this.isCurrentStart) {
                    UpdaterRecord.reset();
                } else if (this.isCurrentStart) {
                    // Record Cafe
                    switch (ConfigManager.getInstance().getConfig().saveFormat) {
                        case 0 : {
                            LinkedHashMap<String, IGraphicalComponent> map = new LinkedHashMap<>();
                            RegistryGraphicalComponents.init(VisionArea.class, map);
                            // 現在のConfigを書き込む
                            Config recordConfig = new Config();
                            for (Field field : recordConfig.visibility.getClass().getFields()) {
                                if (field.getType() == boolean.class) {
                                    field.setBoolean(recordConfig.visibility, true);
                                }
                            }
                            recordConfig.visibility.invertVisible = false;
                            recordConfig.isGoalOfYellowPositive = ConfigManager.getInstance().getConfig().isGoalOfYellowPositive;
                            List<Integer> obstacles = new ArrayList<>();
                            for (int i = 0; i < ConfigManager.MAX_ROBOTS; i++) {
                                obstacles.add(i);
                            }
                            recordConfig.visibility.yellowObstacle = MathHelper.toArray(obstacles);
                            recordConfig.visibility.blueObstacle = MathHelper.toArray(obstacles);
                            // 現在のGuiThreadのGraphics2Dを取得
                            RegistryGraphicalComponents.update(VisionArea.class, map, recordConfig);
                            RecordData0 recordData = new RecordData0();
                            final ai_server_cafe.model.field.Field field = UpdaterWorld.getInstance().getField();
                            // これより下でRecordData0に上記で取得したデータを書き込む
                            recordData.drawWidth = field.getCarpetWidth();
                            recordData.drawHeight = field.getCarpetHeight();
                            recordData.frame = this.commonCycles;
                            if (!map.isEmpty()) {
                                // RecordData0にGraphics2Dを保存する
                                for (String key : map.keySet()) {
                                    map.get(key).paint(new GraphicsRecord(key, recordData));
                                }
                            }
                            // RecordData0を圧縮する
                            Gson gson = new GsonBuilder().create();
                            String rawJson = gson.toJson(recordData);
                            String compressedData = CompressHelper.getCompressedString(rawJson);
                            UpdaterRecord.getInstance().getRecordWriter().get().append(compressedData + "\n");
                            break;
                        }
                        case 1 : {
                            RecordData1 recordData1 = new RecordData1();
                            recordData1.frame = this.commonCycles;
                            recordData1.cycleTime = configManager.getCycleTime();
                            recordData1.isGoalOfYellowPositive = configManager.isInvert(TeamColor.YELLOW);
                            // 現在のWorld，ObstacleなどをRecordData1に保存する
                            for (IItemDraw draw : RegistryGraphicalComponents.REGISTRY) {
                                recordData1 = draw.putRecord(recordData1);
                            }
                            // RecordData1を圧縮する
                            Gson gson = new GsonBuilder().create();
                            String rawJson = gson.toJson(recordData1);
                            String compressedData = CompressHelper.getCompressedString(rawJson);
                            UpdaterRecord.getInstance().getRecordWriter().get().append(compressedData + "\n");
                            break;
                        }
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.logger.error("Unknown error occurred : {}", (Object[]) e.getStackTrace());
            }
            this.isLastStart = this.isCurrentStart;
        }
    }

    @Override
    protected void init() {
        this.lastDate = TimeHelper.now();
        this.commonCycles = 0L;
    }

    public static void reset() {
        instance = null;
    }
}
