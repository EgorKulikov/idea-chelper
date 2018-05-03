package net.egork.chelper.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class StreamConfiguration {
    public static final StreamConfiguration STANDARD = new StreamConfiguration(StreamType.STANDARD);
    public static final StreamConfiguration TASK_ID = new StreamConfiguration(StreamType.TASK_ID);

    public final StreamType type;
    public final String fileName;

    @JsonCreator
    public StreamConfiguration(@JsonProperty("type") StreamType type) {
        this(type, null);
    }

    public StreamConfiguration(StreamType type, String fileName) {
        this.type = type;
        this.fileName = fileName;
    }

    public String getFileName(String taskId, String extension) {
        if (type == StreamType.CUSTOM) {
            return fileName;
        }
        if (type == StreamType.TASK_ID) {
            return taskId.toLowerCase() + extension;
        }
        return null;
    }

    public static final StreamType[] OUTPUT_TYPES = {
            StreamType.STANDARD, StreamType.TASK_ID, StreamType.CUSTOM
    };

    public static enum StreamType {
        STANDARD("Standard stream", false),
        TASK_ID("Name.in/.out", false),
        CUSTOM("Custom filename", true),
        LOCAL_REGEXP("Local regular expression", true);
        private final String uiDescription;
        public final boolean hasStringParameter;

        private StreamType(String uiDescription, boolean hasStringParameter) {
            this.uiDescription = uiDescription;
            this.hasStringParameter = hasStringParameter;
        }

        @Override
        public String toString() {
            return uiDescription;
        }
    }
}
