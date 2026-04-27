package ai_server_cafe.util.gui;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

public class PatternLayoutCafe extends AbstractStringLayout {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");

    public PatternLayoutCafe() {
        super(Charset.defaultCharset());
    }

    @Override
    public String toSerializable(LogEvent event) {
        return DATE_FORMAT.format(event.getTimeMillis()) + " [" + event.getLevel() + "] " + event.getLoggerName() + " - " + event.getMessage().getFormattedMessage() + "\n\r";
    }
}
