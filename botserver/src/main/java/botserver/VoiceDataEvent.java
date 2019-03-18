package botserver;

public class VoiceDataEvent extends StreamEvent {
    private byte[] data = null;

    public VoiceDataEvent(String caller, byte[] data) {
        super(StreamEventType.VOICE_DATA, caller);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
