package botserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class StreamHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(StreamHandler.class);

    private static final AttributeKey<String> CALLER = AttributeKey.valueOf("caller");

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        // fire stream end event
        String caller = ctx.channel().attr(CALLER).get();
        StreamEvent event = new StreamEndEvent(caller);
        LinkedBlockingQueue<StreamEvent> stream = StreamFactory.getInstance().getStream(caller);
        if (stream == null) {
            logger.warn("discard event because listener dead: caller={}, event={}", caller, event.getType());
            return;
        }
        stream.offer(event);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof StreamEvent)) {
            return;
        }

        // fire stream event
        String caller = ctx.channel().attr(CALLER).get();
        StreamEvent event = (StreamEvent) msg;
        LinkedBlockingQueue<StreamEvent> stream = StreamFactory.getInstance().getStream(caller);
        if (stream == null) {
            logger.warn("discard event because listener dead: caller={}, event={}", caller, event.getType());
            return;
        }
        stream.offer(event);
    }

}
