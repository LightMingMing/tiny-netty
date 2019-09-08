package tiny.netty.channel;

import org.junit.Test;
import tiny.netty.channel.nio.NioEventLoopGroup;
import tiny.netty.channel.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 通道管道测试
 *
 * @author zhaomingming
 */
public class ChannelPipelineTest {

    @Test
    public void testAddAndRemoveHandlerBeforeRegistration() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            TraceableChannelHandler h1 = new TraceableChannelHandler();

            channel.pipeline().addFirst("h1", h1);
            ChannelHandlerContext ctx = channel.pipeline().context("h1");
            channel.pipeline().remove("h1");

            eventLoop.register(channel).get();
            assertThat(channel.isRegistered()).isTrue();

            assertThat(h1.awaitAdded()).isTrue();
            assertThat(h1.awaitRemoved()).isTrue();
            assertThat(h1.awaitRegistered()).isFalse();
            assertThat(ctx.isRemoved()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testAddAndRemoveHandlerAfterRegistration() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            TraceableChannelHandler h1 = new TraceableChannelHandler();

            eventLoop.register(channel).get();
            assertThat(channel.isRegistered());

            channel.pipeline().addFirst("h1", h1);
            ChannelHandlerContext ctx = channel.pipeline().context("h1");
            assertThat(h1.awaitAdded()).isTrue();

            channel.pipeline().remove("h1");
            assertThat(h1.awaitRemoved());
            assertThat(ctx.isRemoved()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testRegister() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            TraceableChannelHandler h1 = new TraceableChannelHandler();
            TraceableChannelHandler h2 = new TraceableChannelHandler();

            channel.pipeline().addFirst("h1", h1).addLast("h2", h2);

            eventLoop.register(channel).get();
            assertThat(channel.isRegistered()).isTrue();

            assertThat(h1.awaitAdded()).isTrue();
            assertThat(h2.awaitAdded()).isTrue();
            assertThat(h1.awaitRegistered()).isTrue();
            assertThat(h2.awaitRegistered()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testDeregister() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            TraceableChannelHandler h1 = new TraceableChannelHandler();
            TraceableChannelHandler h2 = new TraceableChannelHandler();

            channel.pipeline().addFirst("h1", h1).addLast("h2", h2);

            eventLoop.register(channel).get();
            assertThat(channel.isRegistered()).isTrue();

            channel.deregister().get();
            assertThat(channel.isRegistered()).isFalse();
            assertThat(h1.awaitUnregistered()).isTrue();
            assertThat(h2.awaitUnregistered()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testChannelActive() throws Exception {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            TraceableChannelHandler h1 = new TraceableChannelHandler();
            channel.pipeline().addFirst("h1", h1);

            ChannelFuture<?> regFuture = eventLoop.register(channel);
            ChannelFuture<?> actFuture = channel.bind(new InetSocketAddress(80));

            regFuture.get();
            assertThat(channel.isRegistered()).isTrue();

            actFuture.get();
            assertThat(channel.isActive()).isTrue();
            assertThat(h1.awaitActive()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testChannelInactiveAfterClose() throws ExecutionException, InterruptedException {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            TraceableChannelHandler h1 = new TraceableChannelHandler();
            channel.pipeline().addFirst("h1", h1);

            eventLoop.register(channel);
            channel.bind(new InetSocketAddress(80)).get();
            assertThat(channel.isOpen()).isTrue();
            assertThat(channel.isActive()).isTrue();

            channel.close().get();
            assertThat(channel.isOpen()).isFalse();
            assertThat(channel.isActive()).isFalse();
            assertThat(h1.awaitInactive()).isTrue();
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    static class TraceableChannelHandler extends ChannelInboundHandlerAdapter {

        private CountDownLatch added = new CountDownLatch(1);
        private CountDownLatch removed = new CountDownLatch(1);
        private CountDownLatch registered = new CountDownLatch(1);
        private CountDownLatch unregistered = new CountDownLatch(1);
        private CountDownLatch active = new CountDownLatch(1);
        private CountDownLatch inactive = new CountDownLatch(1);

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            added.countDown();
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            removed.countDown();
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            registered.countDown();
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            unregistered.countDown();
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            active.countDown();
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            inactive.countDown();
            super.channelInactive(ctx);
        }

        boolean awaitAdded() throws InterruptedException {
            return added.await(1, TimeUnit.SECONDS);
        }

        boolean awaitRemoved() throws InterruptedException {
            return removed.await(1, TimeUnit.SECONDS);
        }

        boolean awaitRegistered() throws InterruptedException {
            return registered.await(1, TimeUnit.SECONDS);
        }

        boolean awaitUnregistered() throws InterruptedException {
            return unregistered.await(1, TimeUnit.SECONDS);
        }

        boolean awaitActive() throws InterruptedException {
            return active.await(1, TimeUnit.SECONDS);
        }

        boolean awaitInactive() throws InterruptedException {
            return inactive.await(1, TimeUnit.SECONDS);
        }
    }
}