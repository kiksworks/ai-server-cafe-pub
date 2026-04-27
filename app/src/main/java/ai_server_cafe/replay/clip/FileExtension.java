package ai_server_cafe.replay.clip;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum FileExtension {
    BIN(".bin", StandardCharsets.ISO_8859_1),
    CSV(".csv", StandardCharsets.UTF_8);

    private final String ext;
    private final Charset charsets;

    FileExtension(String ext, Charset charsets) {
        this.ext = ext;
        this.charsets = charsets;
    }

    public String getExtension() {
        return this.ext;
    }

    public Charset getCharsets() {
        return this.charsets;
    }
}
