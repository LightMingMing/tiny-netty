package tiny.netty.channel.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * NioServerSocketChannel 服务端Socket通道
 *
 * @author zhaomingming
 */
public class NioServerSocketChannel extends AbstractNioChannel {

    public NioServerSocketChannel() {
        super(openServerSocketChannel());
    }

    private static ServerSocketChannel openServerSocketChannel() {
        try {
            return SelectorProvider.provider().openServerSocketChannel();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open a serverSocketChannel");
        }
    }

    @Override
    public ServerSocketChannel javaChannel() {
        return (ServerSocketChannel) super.javaChannel();
    }

    @Override
    protected Unsafe newUnsafe() {
        return new NioMessageUnsafe();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        logger.debug("(nio) bind the socketAddress: {}", localAddress);
        // TODO 配置backlog
        javaChannel().bind(localAddress);
    }

    @Override
    public boolean isActive() {
        return javaChannel().socket().isBound();
    }

    private class NioMessageUnsafe extends AbstractNioUnsafe implements NioUnsafe {
    }
}
