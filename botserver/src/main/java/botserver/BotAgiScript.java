package botserver;

import org.asteriskjava.fastagi.*;
import org.asteriskjava.fastagi.reply.AgiReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BotAgiScript extends BaseAgiScript {
    private static final Logger logger = LoggerFactory.getLogger(BotAgiScript.class);
    public static long streamPollTimeout = 5;
    private static String wavOutDir = "/home/k/audios/";

    private String streamHost = "127.0.0.1";
    private int streamPort = 29000;

    public BotAgiScript(String streamHost, int streamPort) {
        super();
        this.streamHost = streamHost;
        this.streamPort = streamPort;
    }

    @Override
    public void service(AgiRequest request, AgiChannel channel) throws AgiException {
        String caller = request.getCallerIdNumber();
        logger.info("get new call: caller={}, uniqueid={}", caller, request.getUniqueId());


        try {
            answer();

            LinkedBlockingQueue<StreamEvent> stream = StreamFactory.getInstance().createStream(caller);
            int result = getAudioStream(streamHost, streamPort);
            if (result != 0) {
                logger.error("get audio stream failed: caller={}, code={}", caller, result);
                hangup();
                return;
            }
            logger.info("get audio stream success: caller={}", caller);

            boolean hangup = false;

            while (!hangup) {
                if (channel.getLastReply().getResultCode() == AgiReply.SC_DEAD_CHANNEL) {
                    logger.info("AGI[{}] asterisk channel closed", caller);
                    break;
                }

                StreamEvent event = null;
                try {
                    event = stream.poll(streamPollTimeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.warn("AGI[{}] stream event poll interruptted", caller);
                    break;
                }

                if (event == null) {
                    continue;
                }

                logger.info("AGI[{}] got stream event: caller={}, event={}", caller, event.getCaller(), event.getType());
                switch (event.getType()) {
                    case STREAM_START:
                        logger.info("AGI[{}] play intro to caller", caller);
                        // 阻塞调用，非打断播放语音
                        streamFile("hello-world");
                        break;
                    case STREAM_END:
                        hangup = true;
                        break;
                    case VOICE_START:
                        logger.info("AGI[{}] stopstream because user start talking", caller);
                        // 停止播放语音
                        stopStream();
                        break;
                    case VOICE_DATA:
                        byte[] voiceData = ((VoiceDataEvent) event).getData();
                        logger.info("AGI[{}] got voice data: caller={}, length={}", caller, event.getCaller(), voiceData.length);
                        // 获得一句用户语音，将声音写入wav文件
                        handleVoiceData(caller, request.getUniqueId(), voiceData);
                        // 异步调用播放语音，可继续检测VOICE_START事件
                        asyncStreamFile("demo-abouttotry");
                        break;
                    default:
                        logger.error("AGI[{}] got unknown stream event: caller={}, event_type={}", caller, event.getCaller(), event.getType());
                }
            }
        } catch (AgiHangupException he) {
            // ignore
        } catch (AgiException e) {
            logger.error("AGI[{}] got agi exception", e);
        } finally {
            StreamFactory.getInstance().destroyStream(caller);
        }

    }

    private void handleVoiceData(String caller, String callId, byte[] data) throws AgiException {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
        String wavFileName = wavOutDir + caller + "_" + callId.replace(".", "_") +  timestamp;
        InputStream input = new ByteArrayInputStream(data);
        try {
            AudioFormat format = new AudioFormat(8000f, 16, 1, true, false);
            AudioInputStream stream = new AudioInputStream(input, format, data.length);
            File wavFile = new File(wavFileName);
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavFile);
            logger.info("AGI[{}]wav file saved: {}", caller, wavFileName);
        } catch (IOException e) {
            logger.error("failed to write wav file: {}, error={}", wavFileName, e);
        }
    }
}
