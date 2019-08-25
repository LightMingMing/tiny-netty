package tiny.netty.channel.nio;

import org.junit.Test;
import tiny.netty.channel.Channel;
import tiny.netty.channel.ChannelFuture;
import tiny.netty.channel.EventLoop;
import tiny.netty.channel.EventLoopGroup;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 测试
 *
 * @author zhaomingming
 */
public class NioEventLoopTest {

    @Test
    public void testRegister() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        Channel channel = new NioServerSocketChannel();
        try {
            eventLoop.register(channel).get();
            assertThat(channel.isRegistered());
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
            assertThat(channel.isRegistered()).isFalse();
            assertThat(eventLoop.isTerminated());
        }
    }

    @Test
    public void testReRegister() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        Channel channel = new NioServerSocketChannel();
        try {
            eventLoop.register(channel).get();
            assertThat(channel.isRegistered());

            ChannelFuture<?> deregisterFuture = channel.newPromise();
            channel.unsafe().deregister(deregisterFuture);
            deregisterFuture.get();
            assertThat(channel.isRegistered()).isFalse();

            eventLoop.register(channel).get();
            assertThat(channel.isRegistered());
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
            assertThat(channel.isRegistered()).isFalse();
            assertThat(eventLoop.isTerminated());
        }
    }

    @Test
    public void testReRegisterAnotherEventLoop() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(2);
        Channel channel = new NioServerSocketChannel();
        try {
            group.register(channel).get();
            assertThat(channel.isRegistered());

            ChannelFuture<?> deregisterFuture = channel.newPromise();
            channel.unsafe().deregister(deregisterFuture);
            deregisterFuture.get();
            assertThat(channel.isRegistered()).isFalse();

            group.register(channel).get();
            assertThat(channel.isRegistered());
        } finally {
            group.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            group.awaitTermination(2, TimeUnit.SECONDS);
            assertThat(channel.isRegistered()).isFalse();
            assertThat(group.isTerminated());
        }
    }

    @Test
    public void testActive() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        Channel channel = new NioServerSocketChannel();
        try {
            eventLoop.register(channel).get();
            assertThat(channel.isRegistered()).isTrue();

            assertThat(channel.isActive()).isFalse();
            ChannelFuture<?> bindFuture = channel.newPromise();
            channel.unsafe().bind(new InetSocketAddress(8080), bindFuture);
            bindFuture.get();
            assertThat(channel.isActive()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }
}