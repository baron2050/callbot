package botserver;

public class VoiceStartEvent extends StreamEvent {
    public VoiceStartEvent(String caller) {
        super(StreamEventType.VOICE_START, caller);
    }
}
