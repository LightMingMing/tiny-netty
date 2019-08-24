package tiny.netty.channel;

import tiny.netty.util.concurrent.MultiThreadEventExecutorGroup;

import java.util.concurrent.ThreadFactory;

/**
 * 抽象事件循环组
 *
 * @author zhaomingming
 */
public abstract class AbstractEventLoopGroup extends MultiThreadEventExecutorGroup implements EventLoopGroup {

    private static final int DEFAULT_EVENT_LOOP_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    protected AbstractEventLoopGroup(ThreadFactory threadFactory, Object... args) {
        this(DEFAULT_EVENT_LOOP_THREADS, threadFactory, args);
    }

    protected AbstractEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        super(nThreads, threadFactory, args);
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
    public ChannelFuture<?> register(ChannelFuture<?> promise) {
        return next().register(promise);
    }
}
