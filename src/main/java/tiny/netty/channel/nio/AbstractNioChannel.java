package tiny.netty.channel.nio;

import tiny.netty.channel.AbstractChannel;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * Nio通道抽象实现
 *
 * @author zhaomingming
 */
public abstract class AbstractNioChannel extends AbstractChannel implements NioChannel {

    private final SelectableChannel ch;
    private SelectionKey selectionKey;

    protected AbstractNioChannel(SelectableChannel ch) {
        this.ch = ch;
    }

    @Override
    public boolean isOpen() {
        return javaChannel().isOpen();
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
        selectionKey = ch.register(eventLoop().selector(), 0, this);
    }

    @Override
    protected void doDeregister() {
        logger.debug("(nio) cancel the registration of this channel with its selector");
        selectionKey.cancel();
    }

    @Override
    protected void doClose() throws Exception {
        logger.debug("(nio) close the channel");
        javaChannel().close();
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }

    abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe {

    }
}
