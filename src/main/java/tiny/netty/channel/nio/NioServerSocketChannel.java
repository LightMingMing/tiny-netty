package tiny.netty.channel.nio;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * NioServerSocketChannel 服务端Socket通道
 *
 * @author zhaomingming
 */
public class NioServerSocketChannel extends AbstractNioChannel {

    protected NioServerSocketChannel() {
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
    protected Unsafe newUnsafe() {
        return new NioMessageUnsafe();
    }

    private class NioMessageUnsafe extends AbstractNioUnsafe implements NioUnsafe {
    }
}
