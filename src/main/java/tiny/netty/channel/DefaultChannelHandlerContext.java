package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutor;

/**
 * 默认通道处理器上下文实现
 *
 * @author zhaomingming
 */
public class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

    private final ChannelHandler handler;

    public DefaultChannelHandlerContext(Channel channel, EventExecutor executor, ChannelHandler handler, String name) {
        super(channel, executor, name, isInbound(handler), isOutbound(handler));
        this.handler = handler;
    }

    private static boolean isInbound(ChannelHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        return handler instanceof ChannelInboundHandler;
    }

    private static boolean isOutbound(ChannelHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        return handler instanceof ChannelOutboundHandler;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }
}
