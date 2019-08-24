package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutor;

/**
 * 事件循环器
 *
 * @author zhaomingming
 */
public interface EventLoop extends EventExecutor, EventLoopGroup {
    @Override
    EventLoopGroup parent();

    @Override
    EventLoop next();

    // 异步将通道注册到事件循环器中
    @Override
    ChannelFuture<?> register(Channel channel);

    // 异步将通道注册到事件循环器中
    @Override
    ChannelFuture<?> register(Channel channel, ChannelFuture<?> promise);
}
