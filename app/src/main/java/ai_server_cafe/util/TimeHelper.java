package ai_server_cafe.util;

public class TimeHelper {
    public static double now() {
        return (double)(System.nanoTime()) * 1e-9;
    }
}
