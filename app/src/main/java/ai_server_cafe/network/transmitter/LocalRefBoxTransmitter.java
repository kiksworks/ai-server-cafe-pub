package ai_server_cafe.network.transmitter;

import ai_server_cafe.config.Config;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterRefBox;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;

public final class LocalRefBoxTransmitter extends AbstractLoopThreadCafe {
    private static LocalRefBoxTransmitter instance = null;
    private double lastTime;
    private int totalSend;

    private LocalRefBoxTransmitter() {
        super("local-ref-box-transmitter");
        this.lastTime = 0.0;
        this.totalSend = 0;
    }

    @Override
    synchronized protected void loop() {
        if (TimeHelper.now() - this.lastTime > ConfigManager.getInstance().getSendCycleTime()) {
            Config.LocalRefBox localRefBox = ConfigManager.getInstance().getConfig().localRefBoxConfig;
            this.totalSend++;
            this.lastTime = TimeHelper.now();
            GcRefereeMessage.Referee referee = GcRefereeMessage.Referee.newBuilder()
                    .setCommand(localRefBox.command)
                    .setCommandTimestamp((long)(TimeHelper.now() * 1000000))
                    .setPacketTimestamp((long)(TimeHelper.now() * 1000000))
                    .setCommandCounter(this.totalSend)
                    .setStage(localRefBox.stage)
                    .setDesignatedPosition(GcRefereeMessage.Referee.Point.newBuilder()
                            .setX((float)localRefBox.ballPlacePosX)
                            .setY((float)localRefBox.ballPlacePosY)
                            .build())
                    .setBlue(GcRefereeMessage.Referee.TeamInfo.newBuilder()
                            .setName(localRefBox.blueTeamName)
                            .setScore(localRefBox.blueScore)
                            .setRedCards(localRefBox.blueRedCards)
                            .setYellowCards(localRefBox.blueYellowCards)
                            .setTimeouts(localRefBox.blueTimeouts)
                            .setTimeoutTime(localRefBox.blueTimeoutTime)
                            .setGoalkeeper(localRefBox.blueGoalKeeper)
                            .setMaxAllowedBots(localRefBox.blueMaxAllowRobots)
                            .build())
                    .setYellow(GcRefereeMessage.Referee.TeamInfo.newBuilder()
                            .setName(localRefBox.yellowTeamName)
                            .setScore(localRefBox.yellowScore)
                            .setRedCards(localRefBox.yellowRedCards)
                            .setYellowCards(localRefBox.yellowYellowCards)
                            .setTimeouts(localRefBox.yellowTimeouts)
                            .setTimeoutTime(localRefBox.yellowTimeoutTime)
                            .setGoalkeeper(localRefBox.yellowGoalKeeper)
                            .setMaxAllowedBots(localRefBox.yellowMaxAllowRobots)
                            .build())
                    .build();
            UpdaterRefBox.getInstance().updateLocalRefBox(referee);
        }
    }

    @Override
    synchronized protected void init() {
    }

    public static LocalRefBoxTransmitter getInstance() {
        if (instance == null) {
            instance = new LocalRefBoxTransmitter();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }
}
