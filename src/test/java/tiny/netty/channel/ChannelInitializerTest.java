package tiny.netty.channel;

import org.junit.Test;
import tiny.netty.channel.nio.NioEventLoopGroup;
import tiny.netty.channel.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 测试
 *
 * @author zhaomingming
 */
public class ChannelInitializerTest {

    @Test
    public void test() throws InterruptedException, ExecutionException {
        EventLoop eventLoop = new NioEventLoopGroup(1).next();
        try {
            Channel channel = new NioServerSocketChannel();
            channel.pipeline().addFirst("init", new ChannelInitializer() {
                @Override
                protected void initChannel(Channel channel) {
                    channel.pipeline().addFirst("h1", new ChannelInboundHandlerAdapter());
                    channel.pipeline().addFirst("h2", new ChannelInboundHandlerAdapter());
                }
            });
            eventLoop.register(channel);
            channel.bind(new InetSocketAddress(8080)).get();

            assertThat(channel.pipeline().get("init")).isNull();
            assertThat(channel.pipeline().get("h1")).isNotNull();
            assertThat(channel.pipeline().get("h2")).isNotNull();

        } finally {
            eventLoop.shutdownGracefully();
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
        }
    }
}