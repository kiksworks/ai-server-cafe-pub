package ai_server_cafe.model.field;

public abstract class AbstractFilteredObject<T> extends FieldObject {
    public AbstractFilteredObject() {
        this.lost = true;
    }

    public abstract T getRaw();
}
