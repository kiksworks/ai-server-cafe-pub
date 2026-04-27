package ai_server_cafe.network.transmitter;

import ai_server_cafe.Main;
import ai_server_cafe.model.network.SendCommand;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.TransmitterManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public final class RobotDriver extends AbstractLoopThreadCafe {
    private static RobotDriver instance = null;
    private double lastTime;

    private RobotDriver() {
        super("robot driver");
    }

    @Override
    protected void loop() {
        double nowDate = TimeHelper.now();
        if (nowDate - this.lastTime >= ConfigManager.getInstance().getSendCycleTime()) {
                // WorldUpdater -> IntegratedRobot && !isLost && containsActiveRobots
                // Controller
                // SendCommand
            try {
                UpdaterWorld updaterWorld = UpdaterWorld.getInstance();
                List<SendCommand> sendCommands = updaterWorld.makeSendCommands().get();
                for (AbstractTransmitter at : TransmitterManager.getInstance().getTransmitters().get()) {
                    for (SendCommand sendCommand : sendCommands) {
                        at.sendCommand(sendCommand);
                        updaterWorld.updateSendCommand(sendCommand.getColor(), sendCommand.getId(), sendCommand);
                    }
                    at.sendBuffer();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Main.exit(1, true);
            }

            this.lastTime = TimeHelper.now();
        }
    }

    @Override
    protected void init() {
        this.lastTime = TimeHelper.now();
    }

    public static RobotDriver getInstance() {
        if (instance == null) {
            instance = new RobotDriver();
        }
        return instance;
    }

    @Nonnull
    public static SendCommand makeHaltCommand(int id, @Nonnull TeamColor color) {
        return new SendCommand(id, color, new Pair<EnumKickType, Integer>(EnumKickType.NONE, 0), 0, 0, 0,0, false);
    }

    public static void reset() {
        instance = null;
    }
}
