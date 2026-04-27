package ai_server_cafe.records;

import ai_server_cafe.game.stats.StatsMaker;
import ai_server_cafe.game.strategy.ExpectedGoals;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterThreadStatus;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;

import java.util.Optional;

public final class StrategyThread extends AbstractLoopThreadCafe {
    private static StrategyThread instance = null;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<StatsMaker> statsMaker;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<ExpectedGoals> expectedGoals;
    private double lastDate = 0.0;
    private long commonCycles = 0L;

    private StrategyThread() {
        super("strategy-runner");
        this.statsMaker = Optional.empty();
        this.expectedGoals = Optional.empty();
    }

    public static StrategyThread getInstance() {
        if (instance == null) {
            instance = new StrategyThread();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    @Override
    protected void loop() {
        double nowDate = TimeHelper.now();
        ConfigManager configManager = ConfigManager.getInstance();
        if (nowDate - this.lastDate >= configManager.getCycleTime()) {
            this.lastDate = nowDate;
            UpdaterThreadStatus.getInstance().addTPSData(this.name, nowDate);
            this.commonCycles++;

            try {
                if (configManager.isNeedResetRecord()) {
                    this.statsMaker = Optional.of(new StatsMaker());
                    this.expectedGoals = Optional.of(new ExpectedGoals());
                    configManager.setNeedResetRecord(false);
                }

                if (this.statsMaker.isEmpty()) {
                    this.statsMaker = Optional.of(new StatsMaker());
                }
                if (this.expectedGoals.isEmpty()) {
                    this.expectedGoals = Optional.of(new ExpectedGoals());
                }

                if (configManager.isStart()) {
                    if (this.statsMaker.isPresent()) {
                        this.statsMaker.get().update();
                    }
                    if (this.expectedGoals.isPresent()) {
                        this.expectedGoals.get().update();
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                this.logger.error("Unknown error occurred : {}", (Object[]) e.getStackTrace());
            }
            UpdaterWorld.getInstance().updateCommonCycle(this.commonCycles);
        }
    }

    @Override
    protected void init() {
        this.lastDate = TimeHelper.now();
        this.commonCycles = 0L;
    }

    @Override
    protected void onTerminate() {

    }
}
