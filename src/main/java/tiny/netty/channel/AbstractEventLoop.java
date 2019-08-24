package tiny.netty.channel;

import tiny.netty.util.concurrent.EventExecutorGroup;
import tiny.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.concurrent.ThreadFactory;

/**
 * 抽象事件循环器
 *
 * @author zhaomingming
 */
public abstract class AbstractEventLoop extends SingleThreadEventExecutor implements EventLoop {

    protected AbstractEventLoop(EventExecutorGroup parent, ThreadFactory factory) {
        super(parent, factory);
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    public EventLoopGroup parent() {
        return (EventLoopGroup) super.parent();
    }

    @Override
    public ChannelFuture<?> register(Channel channel) {
        return register(channel, channel.newPromise());
    }
}
