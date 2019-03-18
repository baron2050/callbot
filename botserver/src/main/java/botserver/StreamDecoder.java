package botserver;

import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;



public class StreamDecoder extends ByteToMessageDecoder {
    private enum StreamDecoderState {
        READ_HEADER,
        READ_DATA,
    }

    private static final Logger logger = LoggerFactory.getLogger(StreamDecoder.class);
    private static final AttributeKey<String> CALLER = AttributeKey.valueOf("caller");
    private static final TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(8000, 16, 1, true, false);
    private static final TarsosDSPAudioFloatConverter audioFloatConverter = TarsosDSPAudioFloatConverter.getConverter(audioFormat);
    private static final double DEFAULT_SILENCE_THRESHOLD = -70.0D;
    private static final int DEFAULT_SILENCE_FRAME_THRESHOLD = 8000;

    private double silenceThreshold = DEFAULT_SILENCE_THRESHOLD;
    private int silenceFrameThreshold = DEFAULT_SILENCE_FRAME_THRESHOLD;
    private SilenceDetector silenceDetector;

    private String caller = null;
    private int silenceFrameCount = DEFAULT_SILENCE_FRAME_THRESHOLD;
    private ByteBuf voiceBuffer;

    private StreamDecoderState state = StreamDecoderState.READ_HEADER;

    public StreamDecoder() {
        this(DEFAULT_SILENCE_THRESHOLD, DEFAULT_SILENCE_FRAME_THRESHOLD);
    }

    public StreamDecoder(double silenceThreadHold, int silenceFrameThreshold) {
        this.silenceThreshold = silenceThreadHold;
        this.silenceFrameThreshold = silenceFrameThreshold;
        this.silenceDetector = new SilenceDetector(silenceThreshold, false);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        voiceBuffer = ctx.alloc().buffer();
    }

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved0(ctx);
        ReferenceCountUtil.release(voiceBuffer);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (voiceBuffer.isReadable()) {
            byte[] voiceData = new byte[voiceBuffer.readableBytes()];
            voiceBuffer.readBytes(voiceData);
            ctx.fireChannelRead(new VoiceDataEvent(caller, voiceData));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        switch (state) {
            case READ_HEADER:
                if (in.readableBytes() < 2) {
                    return;
                }
                short callerLen = in.getShort(0);
                if (in.readableBytes() < 2 + callerLen) {
                    return;
                }
                in.skipBytes(2);
                ByteBuf callerBuf = in.readBytes(callerLen);
                caller = callerBuf.toString(Charset.forName("US-ASCII"));
                ReferenceCountUtil.release(callerBuf);

                ctx.channel().attr(CALLER).set(caller);
                out.add(new StreamStartEvent(caller));
                state = StreamDecoderState.READ_DATA;
                //logger.debug("got stream header: caller={}", caller);
                // intentionally omitted break
            case READ_DATA:
                while (in.isReadable()) {
                    int readBufferSize = in.readableBytes();
                    byte[] soundBytes = new byte[readBufferSize];
                    in.readBytes(soundBytes);

                    float[] soundFloats = new float[readBufferSize/audioFormat.getFrameSize()];
                    audioFloatConverter.toFloatArray(soundBytes, soundFloats);

                    boolean beforeSilence = isSilence();
                    if (silenceDetector.isSilence(soundFloats)) {
                        incSilence(soundFloats.length);
                    } else {
                        clearSilence();
                    }

                    //logger.debug("got stream data: caller={}, length={}", caller, readBufferSize);

                    if (isSilence()) {
                        if (voiceBuffer.isReadable()) {
                            byte[] voiceData = new byte[voiceBuffer.readableBytes()];
                            voiceBuffer.readBytes(voiceData);
                            out.add(new VoiceDataEvent(caller, voiceData));
                            voiceBuffer.clear();
                        }
                    } else {
                        voiceBuffer.writeBytes(soundBytes);

                        if (beforeSilence) {
                            out.add(new VoiceStartEvent(caller));
                        }
                    }
                }
                break;
            default:
                throw new Error("invaid stream decoder state");
        }
    }

    private boolean isSilence() {
        return silenceFrameCount >= silenceFrameThreshold;
    }

    private void incSilence(int frameCount) {
        silenceFrameCount += frameCount;
    }

    private void clearSilence() {
        silenceFrameCount = 0;
    }
}
