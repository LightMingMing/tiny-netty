package tiny.netty.channel.nio;

import org.junit.Test;
import tiny.netty.channel.Channel;
import tiny.netty.channel.EventLoop;

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
            eventLoop.register(channel);
        } finally {
            eventLoop.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            eventLoop.awaitTermination(2, TimeUnit.SECONDS);
            assertThat(channel.isRegistered()).isTrue();
            assertThat(eventLoop.isTerminated());
        }
    }
}