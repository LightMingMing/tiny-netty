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

    static class TraceableChannelHandler extends ChannelHandlerAdapter {

        private CountDownLatch added = new CountDownLatch(1);
        private CountDownLatch removed = new CountDownLatch(1);

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            added.countDown();
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            removed.countDown();
        }

        boolean awaitAdded() throws InterruptedException {
            return added.await(1, TimeUnit.SECONDS);
        }

        boolean awaitRemoved() throws InterruptedException {
            return removed.await(1, TimeUnit.SECONDS);
        }
    }
}