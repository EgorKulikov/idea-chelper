package net.egork.chelper.task;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class StreamConfiguration {
	public static final StreamConfiguration STANDARD = new StreamConfiguration(StreamType.STANDARD);
	public static final StreamConfiguration TASK_ID = new StreamConfiguration(StreamType.TASK_ID);

	public final StreamType type;
	public final String fileName;

	public StreamConfiguration(StreamType type) {
		this(type, null);
	}

	public StreamConfiguration(StreamType type, String fileName) {
		this.type = type;
		this.fileName = fileName;
	}

	public String getFileName(String taskId, String extension) {
		if (type == StreamType.CUSTOM)
			return fileName;
		if (type == StreamType.TASK_ID)
			return taskId.toLowerCase() + extension;
		return null;
	}

	public static enum StreamType {
		STANDARD, TASK_ID, CUSTOM
	}
}
