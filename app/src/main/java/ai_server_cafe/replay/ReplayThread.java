package ai_server_cafe.replay;

import ai_server_cafe.game.strategy.XGType;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.records.AbstractRecordData;
import ai_server_cafe.records.ver0.RecordData0;
import ai_server_cafe.records.ver1.PassTargetWrapper;
import ai_server_cafe.records.ver1.PathPlannerWrapper;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.records.ver1.StrategyWrapper;
import ai_server_cafe.replay.clip.ClipState;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterCurrentObstacle;
import ai_server_cafe.updater.UpdaterPassTarget;
import ai_server_cafe.updater.UpdaterPathPlanner;
import ai_server_cafe.updater.UpdaterRefBox;
import ai_server_cafe.updater.UpdaterReplay;
import ai_server_cafe.updater.UpdaterStrategy;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.compressor.CompressHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.reader.ReaderExternal;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * ReplayCafeの処理を書くクラス
 */
public class ReplayThread extends AbstractLoopThreadCafe {
    private static ReplayThread instance = null;
    private double lastDate;
    private ReplayState replayState;
    private ClipState clipState;
    private long start = 0L;

    public static ReplayThread getInstance() {
        if (instance == null) {
            instance = new ReplayThread();
        }
        return instance;
    }

    private ReplayThread() {
        super("replay-thread");
        this.replayState = ReplayState.STOP;
        this.clipState = ClipState.WAIT;
        this.lastDate = 0.0;
    }

    @Override
    protected void loop() {
        this.loadRecordFile();
        // fileの読み込みが終わっていないならここでreturn
        if (UpdaterReplay.getInstance().getExternalFile().get().isEmpty()) return;
        double nowDate = TimeHelper.now();
        if (nowDate - this.lastDate >= ConfigManager.getInstance().getCycleTime()) {
            this.lastDate = nowDate;
            // 再生処理
            // RecordData0とRecordData1で再生するコードの記述が違うのでフレームに関する処理だけ記述
            this.replayState = UpdaterReplay.getInstance().getReplayState();
            switch (this.replayState) {
                case REPLAY: {
                    final long commonFrame = UpdaterReplay.getInstance().getReplayFrame();
                    if (commonFrame < UpdaterReplay.getInstance().getReplayLastFrame()) {
                        // 通常再生
                        UpdaterReplay.getInstance().setReplayFrame(commonFrame + 1L);
                        this.applyLoadedRecordData(UpdaterReplay.getInstance().getReplayFrameData(RecordData1.class).get());
                    } else {
                        // 再生終了したら最終コマで一時停止
                        this.replayState = ReplayState.PAUSE;
                        UpdaterReplay.getInstance().setReplayState(this.replayState);
                        UpdaterReplay.getInstance().setReplayFrame(UpdaterReplay.getInstance().getReplayLastFrame());
                        this.applyLoadedRecordData(UpdaterReplay.getInstance().getReplayFrameData(RecordData1.class).get());
                    }
                    break;
                }
                case STOP: {
                    // close read file
                    UpdaterReplay.getInstance().setReplayFrame(0L);
                    UpdaterReplay.getInstance().replayClose();
                    this.applyLoadedRecordData(Optional.empty());
                    break;
                }
                case PAUSE: {
                    // 現在のフレームで停止
                    this.applyLoadedRecordData(UpdaterReplay.getInstance().getReplayFrameData(RecordData1.class).get());
                    break;
                }
                default: {
                    this.logger.warn("ReplayState is default.");
                    break;
                }
            }

            // 切り取り処理
            this.clipState = UpdaterReplay.getInstance().getClipState();
            if(UpdaterReplay.getInstance().getClipInitialFrame().get().isEmpty()
                    || UpdaterReplay.getInstance().getClipLastFrame().get().isEmpty()) return;
            switch (this.clipState) {
                case START : {
                    this.start = System.currentTimeMillis();
                    this.logger.info("Clipping start");
                    UpdaterReplay.getInstance().setClipState(ClipState.CLIP);
                    UpdaterReplay.getInstance().setClipFrame(
                            UpdaterReplay.getInstance().getClipInitialFrame().get().get());
                    break;
                }
                case CLIP: {
                    final long commonFrame = UpdaterReplay.getInstance().getClipFrame();
                    if(commonFrame >= UpdaterReplay.getInstance().getClipLastFrame().get().get()) {
                        UpdaterReplay.getInstance().setClipState(ClipState.FINISH);
                        UpdaterReplay.getInstance().setClipFrame(UpdaterReplay.getInstance().getClipLastFrame().get().get());
                    }
                    switch (UpdaterReplay.getInstance().getExtension()) {
                        case BIN : {
                            UpdaterReplay.getInstance().getClipWriterExternal().get().append(
                                    UpdaterReplay.getInstance().getClipFrameCompressedData().get());
                            break;
                        }
                        case CSV : {
                            switch (UpdaterReplay.getInstance().getFormat()) {
                                case 0 : {
                                    this.applyLoadedClipCSVData(UpdaterReplay.getInstance().getClipFrameRecordData(RecordData0.class).get());
                                    break;
                                }
                                case 1 :{
                                    this.applyLoadedClipCSVData(UpdaterReplay.getInstance().getClipFrameRecordData(RecordData1.class).get());
                                    break;
                                }
                                default : {
                                    break;
                                }
                            }
                            break;
                        }
                        default : {
                            UpdaterReplay.getInstance().setClipState(ClipState.FINISH);
                            break;
                        }
                    }
                    UpdaterReplay.getInstance().setClipFrame(
                            commonFrame + UpdaterReplay.getInstance().getWindowSize());
                    break;
                }
                case FINISH : {
                    this.logger.info("Clipping:{}-{} is done",
                            UpdaterReplay.getInstance().getClipInitialFrame().get().get(),
                            UpdaterReplay.getInstance().getClipLastFrame().get().get());
                    UpdaterReplay.getInstance().clipClose();
                    this.logger.info("Processing time is {}[ms]", (System.currentTimeMillis() - this.start));
                    break;
                }
                case WAIT : {
                    break;
                }
                default : {
                    UpdaterReplay.getInstance().setClipState(ClipState.WAIT);
                    break;
                }
            }
        }
    }

    /**
     *
     * @param recordData 復元した現在のRecordDataから、Updaterを更新したりする
     */
    private <T extends AbstractRecordData> void applyLoadedRecordData(@Nonnull Optional<T> recordData) {
        if (recordData.isEmpty()) {
            UpdaterWorld.getInstance().replayStop();
            UpdaterCurrentObstacle.getInstance().replayStop();
            UpdaterPathPlanner.getInstance().replayStop();
            UpdaterPassTarget.getInstance().replayStop();
            UpdaterRefBox.getInstance().replayStop();
            UpdaterStrategy.getInstance().replayStop();
            ConfigManager.getInstance().replayStop();
            return;
        }
        UpdaterReplay.getInstance().setCycleTime(recordData.get().cycleTime);
        ConfigManager.getInstance().setIsReplayGoalYellowOfPositive(recordData.get().isGoalOfYellowPositive);
        switch (recordData.get().format) {
            case 0: {
                // do nothing
                // GuiThreadのloop内で処理している
                break;
            }
            case 1: {
                RecordData1 recordData1 = (RecordData1) recordData.get();

                // set world
                final World replayWorld = recordData1.getWorld();
                UpdaterWorld.getInstance().setReplayWorld(replayWorld);

                // set referee
                for (GcRefereeMessage.Referee referee : recordData1.getRefereeData()) {
                    UpdaterRefBox.getInstance().updateRefBoxByReplay(referee);
                }

                for (TeamColor color : TeamColor.values()) {
                    // set passTarget
                    final PassTargetWrapper passTargetWrapper = recordData1.getPassTargetData(color);
                    UpdaterPassTarget.getInstance().setScoreMap(passTargetWrapper.getScoreMap(), color);
                    UpdaterPassTarget.getInstance().setPassTargets(passTargetWrapper.getPassTargets(), color);
                    UpdaterPassTarget.getInstance().setKickerPos(passTargetWrapper.getKickerPos(), color);

                    // set strategy
                    final StrategyWrapper strategyWrapper = recordData1.getXGData(color);
                    UpdaterStrategy.getInstance().setAttackDire(strategyWrapper.getAttackSide(), color);
                    for (XGType type : XGType.values()) {
                        UpdaterStrategy.getInstance().setReplayXGMap(strategyWrapper.getXGMap(type), color, type);
                    }

                    for (int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
                        // set obstacles
                        final List<AbstractObstacle> obstacles = recordData1.getObstacles(color, id);
                        UpdaterCurrentObstacle.getInstance().putReplay(color, id, obstacles);

                        // set pathPlanner
                        final PathPlannerWrapper pathWrapper = recordData1.getPlannerData(color, id);
                        if (pathWrapper != null) {
                            UpdaterPathPlanner.getInstance().setReplayResult(color, pathWrapper.getPath(id));
                            UpdaterPathPlanner.getInstance().setStep(color, id, pathWrapper.getStep());
                        }
                    }
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     *
     * ファイルが何パーセント読み込めたかを設定する
     */
    private void loadRecordFile() {
        Pair<File, Boolean> loadData = UpdaterReplay.getInstance().getFile().get();
        if (loadData.getFirst() != null && !loadData.getSecond()) {
            File file = loadData.getFirst();
            final Gson gson = new GsonBuilder().create();
            // 最初の行を読み込んで、initialFrameを取得する
            Optional<AbstractRecordData> firstData = Optional.empty();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1));
                String line = reader.readLine();
                reader.close();
                if (line != null) {
                    firstData = Optional.of(gson.fromJson(CompressHelper.getDeCompressedString(line), RecordData1.class));
                    UpdaterReplay.getInstance().setReplayInitialFrame(firstData.get().frame);
                    UpdaterReplay.getInstance().setFormat(firstData.get().format);
                }
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
            // 最後の行を読み込んで、lastFrameを取得する
            Optional<AbstractRecordData> lastData = Optional.empty();
            try {
                if (firstData.isEmpty()) {
                    this.logger.error("firstData is empty");
                    throw new IOException();
                }
                this.logger.info("try to open {} in format version {}",
                        loadData.getFirst().getName(), UpdaterReplay.getInstance().getFormat());
                ReversedLinesFileReader reversedLinesFileReader = ReversedLinesFileReader.builder().setCharset(
                        StandardCharsets.ISO_8859_1).setFile(file).get();
                String line = reversedLinesFileReader.readLine();
                if (line != null) {
                    switch (UpdaterReplay.getInstance().getFormat()) {
                        case 0: {
                            lastData = Optional.of(gson.fromJson(
                                    CompressHelper.getDeCompressedString(line), RecordData0.class));
                            break;
                        }
                        case 1: {
                            lastData = Optional.of(gson.fromJson(
                                    CompressHelper.getDeCompressedString(line), RecordData1.class));
                            break;
                        }
                        default: {
                            this.logger.error("All RecordCafe formats don't match {}",
                                    loadData.getFirst().getName());
                            throw new IOException();
                        }
                    }
                }
                if (lastData.isEmpty()) {
                    this.logger.error("lastData is Empty");
                    throw new IOException();
                }
                reversedLinesFileReader.close();
                UpdaterReplay.getInstance().setReplayLastFrame(lastData.get().frame);
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
            ReaderExternal external = new ReaderExternal(file, this.logger, StandardCharsets.ISO_8859_1, new IFuncParam1<Void, Integer>() {
                @Override
                public Void function(Integer integer) {
                    UpdaterReplay.getInstance().setPercent(integer);
                    return null;
                }
            }, (firstData.isEmpty() || lastData.isEmpty()) ? -1 : lastData.get().frame - firstData.get().frame);
            UpdaterReplay.getInstance().setLoadedFile(external);
        }
    }

    /**
     *
     * データ解析(切り取る内容を書く)
     */
    private <T extends AbstractRecordData> void applyLoadedClipCSVData(@Nonnull Optional<T> recordData) {
        if(recordData.isEmpty()) {
            UpdaterReplay.getInstance().clipClose();
            return;
        }
        switch (recordData.get().format) {
            case 0 : {
                // Graphics2Dのため、解析しようがない
                break;
            }
            case 1 : {
                final RecordData1 recordData1 = (RecordData1) recordData.get();
                // ここより下に切り取る内容を書く

                // ここより上で切り取る内容を書く
                break;
            }
            default : {
                break;
            }
        }
    }

    @Override
    protected void init() {
        this.lastDate = TimeHelper.now();
        this.replayState = ReplayState.STOP;
        UpdaterReplay.getInstance().setReplayState(this.replayState);
        UpdaterReplay.getInstance().setReplayFrame(0L);
    }

    public static void reset() {
        instance = null;
    }

    public void onTerminate() {
        UpdaterReplay.getInstance().replayClose();
    }
}
