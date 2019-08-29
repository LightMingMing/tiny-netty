package tiny.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tiny.netty.util.concurrent.EventExecutor;

import java.net.SocketAddress;

/**
 * 通道处理器上下文实现
 *
 * @author zhaomingming
 */
public abstract class AbstractChannelHandlerContext implements ChannelHandlerContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EventExecutor executor;
    private final ChannelPipeline pipeline;
    private final String name;
    private final boolean inbound;
    private final boolean outbound;
    private AbstractChannelHandlerContext prev;
    private AbstractChannelHandlerContext next;

    protected AbstractChannelHandlerContext(ChannelPipeline pipeline, EventExecutor executor, String name, boolean inbound, boolean outbound) {
        if (pipeline == null) {
            throw new IllegalArgumentException("channel is null");
        }
        this.pipeline = pipeline;
        this.executor = executor;
        this.name = name;
        this.inbound = inbound;
        this.outbound = outbound;
    }

    @Override
    public Channel channel() {
        return pipeline.channel();
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public EventExecutor executor() {
        return executor != null ? executor : channel().eventLoop();
    }

    @Override
    public String name() {
        return name;
    }

    private AbstractChannelHandlerContext findContextInbound() {
        AbstractChannelHandlerContext ctx = this;
        for (; ; ) {
            ctx = ctx.next;
            // DefaultChannelPipeline自带尾节点inbound=true, 不用担心空指针
            if (ctx.inbound) return ctx;
        }
    }

    private AbstractChannelHandlerContext findContextOutbound() {
        AbstractChannelHandlerContext ctx = this;
        for (; ; ) {
            ctx = ctx.prev;
            // DefaultChannelPipeline自带的头节点outbound=true, 不用担心空指针
            if (ctx.outbound) return ctx;
        }
    }

    private boolean invokeHandler() {
        // TODO 是否能够调用处理器
        return true;
    }

    @Override
    public AbstractChannelHandlerContext fireChannelRegistered() {
        AbstractChannelHandlerContext next = findContextInbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelRegistered();
        } else {
            executor.execute(next::invokeChannelRegistered);
        }
        return this;
    }

    private void invokeChannelRegistered() {
        logger.debug("[{}] invokeChannelRegistered()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelRegistered(this);
            } catch (Throwable cause) {
                fireExceptionCaught(cause);
            }
        } else {
            this.fireChannelRegistered();
        }
    }

    @Override
    public AbstractChannelHandlerContext fireChannelUnregistered() {
        AbstractChannelHandlerContext next = findContextInbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelUnregistered();
        } else {
            executor.execute(next::invokeChannelUnregistered);
        }
        return this;
    }

    private void invokeChannelUnregistered() {
        logger.debug("[{}] invokeChannelUnregistered()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelUnregistered(this);
            } catch (Throwable cause) {
                fireExceptionCaught(cause);
            }
        } else {
            this.fireChannelUnregistered();
        }
    }

    @Override
    public AbstractChannelHandlerContext fireChannelActive() {
        AbstractChannelHandlerContext next = findContextInbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelActive();
        } else {
            executor.execute(next::invokeChannelActive);
        }
        return this;
    }

    private void invokeChannelActive() {
        logger.debug("[{}] invokeChannelActive()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelActive(this);
            } catch (Throwable cause) {
                fireExceptionCaught(cause);
            }
        } else {
            this.fireChannelActive();
        }
    }

    @Override
    public AbstractChannelHandlerContext fireChannelInactive() {
        AbstractChannelHandlerContext next = findContextInbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelInactive();
        } else {
            executor.execute(next::invokeChannelInactive);
        }
        return this;
    }

    private void invokeChannelInactive() {
        logger.debug("[{}] invokeChannelInactive()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelInactive(this);
            } catch (Throwable cause) {
                fireExceptionCaught(cause);
            }
        } else {
            this.fireChannelInactive();
        }
    }


    @Override
    public AbstractChannelHandlerContext fireExceptionCaught(Throwable cause) {
        AbstractChannelHandlerContext next = findContextInbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeExceptionCaught(cause);
        } else {
            executor.execute(() -> next.invokeExceptionCaught(cause));
        }
        return this;
    }

    private void invokeExceptionCaught(Throwable cause) {
        logger.debug("[{}] invokeExceptionCaught({})...", name, cause.getMessage());
        if (invokeHandler()) {
            ((ChannelInboundHandler) handler()).exceptionCaught(this, cause);
        } else {
            fireExceptionCaught(cause);
        }
    }

    @Override
    public ChannelFuture<?> bind(SocketAddress localAddress, ChannelFuture<?> promise) {
        AbstractChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeBind(localAddress, promise);
        } else {
            executor.execute(() -> next.invokeBind(localAddress, promise));
        }
        return promise;
    }

    private void invokeBind(SocketAddress localAddress, ChannelFuture<?> promise) {
        logger.debug("[{}] invokeBind({})", name, localAddress);
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).bind(localAddress, promise);
            } catch (Throwable cause) {
                promise.completeExceptionally(cause);
            }
        } else {
            bind(localAddress, promise);
        }
    }

    @Override
    public ChannelFuture<?> close(ChannelFuture<?> promise) {
        AbstractChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeClose(promise);
        } else {
            executor.execute(() -> next.invokeClose(promise));
        }
        return promise;
    }

    private void invokeClose(ChannelFuture<?> promise) {
        logger.debug("[{}] invokeClose()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).close(promise);
            } catch (Throwable cause) {
                promise.completeExceptionally(cause);
            }
        } else {
            close(promise);
        }
    }
}
