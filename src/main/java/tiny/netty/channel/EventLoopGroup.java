package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutorGroup;

/**
 * 事件循环组接口
 *
 * @author zhaomingming
 */
public interface EventLoopGroup extends EventExecutorGroup {

    @Override
    EventLoop next();

    // 异步将通道注册到事件循环组中
    ChannelFuture<?> register(Channel channel);

    // 异步将通道注册到事件循环组中
    ChannelFuture<?> register(ChannelFuture<?> promise);
}
