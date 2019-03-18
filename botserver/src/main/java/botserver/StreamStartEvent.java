package botserver;

public class StreamStartEvent extends StreamEvent {
    public StreamStartEvent(String caller) {
        super(StreamEventType.STREAM_START, caller);
    }
}
