package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutor;

/**
 * 通道处理器上下文
 *
 * @author zhaomingming
 */
public interface ChannelHandlerContext extends ChannelInboundInvoker, ChannelOutboundInvoker {

    ChannelHandler handler();

    Channel channel();

    ChannelPipeline pipeline();

    EventExecutor executor();

    boolean isRemoved();

    String name();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);
}
