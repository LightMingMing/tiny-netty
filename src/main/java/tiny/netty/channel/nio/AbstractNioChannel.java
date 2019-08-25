package tiny.netty.channel.nio;

import tiny.netty.channel.AbstractChannel;

import java.nio.channels.SelectableChannel;

/**
 * Nio通道抽象实现
 *
 * @author zhaomingming
 */
public abstract class AbstractNioChannel extends AbstractChannel implements NioChannel {

    private final SelectableChannel ch;

    protected AbstractNioChannel(SelectableChannel ch) {
        this.ch = ch;
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    @Override
    public SelectableChannel javaChannel() {
        return ch;
    }

    @Override
    protected void doRegister() throws Exception {
        logger.debug("(nio) registers the channel to selector");
        // TODO 不要在这里配置
        ch.configureBlocking(false);
        ch.register(eventLoop().selector(), 0);
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }

    abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe {

    }
}
