package ai_server_cafe.util.natives;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class NativeRegistry {
    private static final List<NativeExtractor> extractors = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger("native-loader");
    public static void register(String targetDir, String targetFile, String destinationDir) {
        extractors.add(new NativeExtractor(targetDir, targetFile, destinationDir));
    }

    public static void loadAll() {
        for (NativeExtractor extractor : extractors) {
            extractor.load(LOGGER);
        }
    }
}
