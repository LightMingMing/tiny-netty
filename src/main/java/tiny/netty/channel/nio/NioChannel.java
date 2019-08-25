package tiny.netty.channel.nio;

import tiny.netty.channel.Channel;

import java.nio.channels.SelectableChannel;

/**
 * Nio通道
 *
 * @author zhaomingming
 */
public interface NioChannel extends Channel {

    SelectableChannel javaChannel();

    @Override
    NioEventLoop eventLoop();

    @Override
    NioUnsafe unsafe();

    interface NioUnsafe extends Unsafe {
    }
}
