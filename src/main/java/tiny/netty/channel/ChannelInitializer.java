package tiny.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通道初始化器
 *
 * @author zhaomingming
 */
public abstract class ChannelInitializer extends ChannelInboundHandlerAdapter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public final void handlerAdded(ChannelHandlerContext ctx) {
        initChannel(ctx);
    }

    private void initChannel(ChannelHandlerContext ctx) {
        try {
            initChannel(ctx.channel());
        } catch (Throwable cause) {
            exceptionCaught(ctx, cause);
        } finally {
            ctx.pipeline().remove(this);
        }
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Failed to initialize a channel. closing: {}", ctx, cause);
        ctx.close();
    }

    protected abstract void initChannel(Channel channel) throws Exception;
}
