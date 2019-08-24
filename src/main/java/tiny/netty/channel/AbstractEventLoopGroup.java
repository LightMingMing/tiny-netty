package tiny.netty.channel;

import tiny.netty.util.concurrent.MultiThreadEventExecutorGroup;

import java.util.concurrent.ThreadFactory;

/**
 * 抽象事件循环组
 *
 * @author zhaomingming
 */
public abstract class AbstractEventLoopGroup extends MultiThreadEventExecutorGroup implements EventLoopGroup {

    protected AbstractEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    public ChannelFuture<?> register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture<?> register(Channel channel, ChannelFuture<?> promise) {
        return next().register(channel, promise);
    }
}
