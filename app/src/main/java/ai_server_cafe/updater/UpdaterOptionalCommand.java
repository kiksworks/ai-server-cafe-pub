package ai_server_cafe.updater;

import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.util.interfaces.WrapperCloneableList;

import java.util.ArrayList;
import java.util.List;

public final class UpdaterOptionalCommand {
    private static UpdaterOptionalCommand instance = null;
    private final List<OptionalCommand> ocList;

    private UpdaterOptionalCommand() {
        this.ocList = new ArrayList<>();
    }

    public synchronized static UpdaterOptionalCommand getInstance() {
        if(instance == null) {
            instance = new UpdaterOptionalCommand();
        }
        return instance;
    }

    public synchronized WrapperCloneableList<OptionalCommand> getCommands() {
        return new WrapperCloneableList<>(this.ocList);
    }

    public synchronized void update(OptionalCommand command) {
        if(this.ocList.isEmpty()){
            this.ocList.add(command);
        } else {
            this.ocList.getFirst().combine(command);
        }
    }

    public synchronized void addCommand(OptionalCommand command) {
        this.ocList.add(command);
    }

    public synchronized void remove(int index) {
        this.ocList.remove(index);
    }

    public synchronized void clear() {
        this.ocList.clear();
    }
}
