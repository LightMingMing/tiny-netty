package tiny.netty.channel.nio;

import tiny.netty.channel.AbstractEventLoopGroup;
import tiny.netty.channel.EventLoopGroup;
import tiny.netty.util.concurrent.DefaultThreadFactory;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

/**
 * Nio事件循环组
 *
 * @author zhaomingming
 */
public class NioEventLoopGroup extends AbstractEventLoopGroup implements EventLoopGroup {

    public NioEventLoopGroup() {
        super(new DefaultThreadFactory("nioEventLoop"), SelectorProvider.provider());
    }

    public NioEventLoopGroup(int nThreads) {
        super(nThreads, new DefaultThreadFactory("nioEventLoop"), SelectorProvider.provider());
    }

    @Override
    protected NioEventLoop newChild(Executor executor, Object... args) {
        return new NioEventLoop(this, executor, (SelectorProvider) args[0]);
    }
}
