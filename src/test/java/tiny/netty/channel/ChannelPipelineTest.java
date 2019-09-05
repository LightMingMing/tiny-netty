package tiny.netty.channel;

import org.junit.Test;
import tiny.netty.channel.nio.NioEventLoopGroup;
import tiny.netty.channel.nio.NioServerSocketChannel;

import java.util.concurrent.CountDownLatch;
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

    static class TraceableChannelHandler extends ChannelInboundHandlerAdapter {

        private CountDownLatch added = new CountDownLatch(1);
        private CountDownLatch removed = new CountDownLatch(1);
        private CountDownLatch registered = new CountDownLatch(1);

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

        boolean awaitAdded() throws InterruptedException {
            return added.await(1, TimeUnit.SECONDS);
        }

        boolean awaitRemoved() throws InterruptedException {
            return removed.await(1, TimeUnit.SECONDS);
        }

        boolean awaitRegistered() throws InterruptedException {
            return registered.await(1, TimeUnit.SECONDS);
        }
    }
}