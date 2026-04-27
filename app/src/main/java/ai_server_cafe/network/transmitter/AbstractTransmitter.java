package ai_server_cafe.network.transmitter;

import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.model.network.SendCommand;
import ai_server_cafe.network.radio.EnumRadioType;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractTransmitter extends AbstractLoopThreadCafe {
    protected AbstractTransmitter(String name) {
        super(name);
    }

    @Override
    @Deprecated
    public void start() {}

    public void startWith(int port, String hostAddress, String interfaceAddress) {
        this.start();
    }

    public abstract void sendCommands(@Nonnull List<SendCommand> commandList);

    public abstract void sendCommand(@Nonnull SendCommand command);

    public void sendBuffer() {}

    public void sendOptionalCommand(OptionalCommand optionalCommand) {}

    public abstract EnumRadioType getType();
}
