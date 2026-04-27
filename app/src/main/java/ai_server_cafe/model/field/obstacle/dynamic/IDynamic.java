package ai_server_cafe.model.field.obstacle.dynamic;

public interface IDynamic {
    IDynamic setDt(double dt);
    IDynamic clone();
    double getDt();
}
