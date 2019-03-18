package botserver;

public class StreamEvent {
    public enum StreamEventType {
        INVALID,
        STREAM_START,
        STREAM_END,
        VOICE_START,
        VOICE_DATA,
    }

    private StreamEventType eventType = StreamEventType.INVALID;
    private String caller = "";

    protected StreamEvent(StreamEventType type, String caller) {
        this.eventType = type;
        this.caller = caller;
    }

    public StreamEventType getType() {
        return eventType;
    }

    public String getCaller() {
        return caller;
    }
}
