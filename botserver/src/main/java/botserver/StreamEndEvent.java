package botserver;

public class StreamEndEvent extends StreamEvent {
    public StreamEndEvent(String caller) {
        super(StreamEventType.STREAM_END, caller);
    }
}
