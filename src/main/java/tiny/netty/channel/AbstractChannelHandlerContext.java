package tiny.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tiny.netty.util.concurrent.EventExecutor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.SocketAddress;

/**
 * 通道处理器上下文实现
 *
 * @author zhaomingming
 */
public abstract class AbstractChannelHandlerContext implements ChannelHandlerContext {

    private static final int INIT = 0;
    private static final int ADD_PENDING = 1;
    private static final int ADD_COMPLETE = 2;
    private static final int REMOVE_COMPLETE = 3;
    private static final VarHandle STATE_HANDLE;

    static {
        MethodHandles.Lookup l = MethodHandles.lookup();
        try {
            STATE_HANDLE = l.findVarHandle(AbstractChannelHandlerContext.class, "handlerState", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EventExecutor executor;
    private final ChannelPipeline pipeline;
    private final String name;
    private final boolean inbound;
    private final boolean outbound;
    volatile AbstractChannelHandlerContext prev;
    volatile AbstractChannelHandlerContext next;
    private volatile int handlerState = INIT;

    protected AbstractChannelHandlerContext(ChannelPipeline pipeline, EventExecutor executor, String name, boolean inbound, boolean outbound) {
        if (pipeline == null) {
            throw new IllegalArgumentException("pipeline is null");
        }
        this.pipeline = pipeline;
        this.executor = executor;
        this.name = name;
        this.inbound = inbound;
        this.outbound = outbound;
    }

    static void invokeChannelRegistered(AbstractChannelHandlerContext ctx) {
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            ctx.invokeChannelRegistered();
        } else {
            executor.execute(ctx::invokeChannelRegistered);
        }
    }

    static void invokeChannelUnregistered(AbstractChannelHandlerContext ctx) {
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            ctx.invokeChannelUnregistered();
        } else {
            executor.execute(ctx::invokeChannelUnregistered);
        }
    }

    static void invokeChannelActive(AbstractChannelHandlerContext ctx) {
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            ctx.invokeChannelActive();
        } else {
            executor.execute(ctx::invokeChannelActive);
        }
    }

    static void invokeChannelInactive(AbstractChannelHandlerContext ctx) {
        EventExecutor executor = ctx.executor();
        if (executor.inEventLoop()) {
            ctx.invokeChannelInactive();
        } else {
            executor.execute(ctx::invokeChannelInactive);
        }
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
    public boolean isRemoved() {
        return handlerState == REMOVE_COMPLETE;
    }

    @Override
    public String name() {
        return name;
    }

    final void setAddPending() {
        if (!STATE_HANDLE.compareAndSet(this, INIT, ADD_PENDING)) {
            logger.warn("Failed to setAddPending, the state is {}.", handlerState);
        }
    }

    final boolean setAddComplete() {
        for (; ; ) {
            int oldState = handlerState;
            if (oldState == REMOVE_COMPLETE) {
                return false;
            }
            if (STATE_HANDLE.compareAndSet(this, oldState, ADD_COMPLETE)) {
                return true;
            }
        }
    }

    final void setRemoved() {
        handlerState = REMOVE_COMPLETE;
    }

    final void callHandlerAdded() throws Exception {
        if (setAddComplete()) {
            logger.debug("[{}] handlerAdded()... ", name());
            handler().handlerAdded(this);
        }
    }

    final void callHandlerRemoved() throws Exception {
        try {
            if (handlerState == ADD_COMPLETE) {
                logger.debug("[{}] handlerRemoved()... ", name());
                handler().handlerRemoved(this);
            }
        } finally {
            setRemoved();
        }
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
        invokeChannelRegistered(findContextInbound());
        return this;
    }

    private void invokeChannelRegistered() {
        logger.debug("[{}] invokeChannelRegistered()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelRegistered(this);
            } catch (Throwable cause) {
                invokeExceptionCaught(cause);
            }
        } else {
            this.fireChannelRegistered();
        }
    }

    @Override
    public AbstractChannelHandlerContext fireChannelUnregistered() {
        invokeChannelUnregistered(findContextInbound());
        return this;
    }

    private void invokeChannelUnregistered() {
        logger.debug("[{}] invokeChannelUnregistered()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelUnregistered(this);
            } catch (Throwable cause) {
                invokeExceptionCaught(cause);
            }
        } else {
            this.fireChannelUnregistered();
        }
    }

    @Override
    public AbstractChannelHandlerContext fireChannelActive() {
        invokeChannelActive(findContextInbound());
        return this;
    }

    private void invokeChannelActive() {
        logger.debug("[{}] invokeChannelActive()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelActive(this);
            } catch (Throwable cause) {
                invokeExceptionCaught(cause);
            }
        } else {
            this.fireChannelActive();
        }
    }

    @Override
    public AbstractChannelHandlerContext fireChannelInactive() {
        invokeChannelInactive(findContextInbound());
        return this;
    }

    private void invokeChannelInactive() {
        logger.debug("[{}] invokeChannelInactive()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelInactive(this);
            } catch (Throwable cause) {
                invokeExceptionCaught(cause);
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
    public ChannelFuture<?> bind(SocketAddress localAddress) {
        return bind(localAddress, newPromise());
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
                ((ChannelOutboundHandler) handler()).bind(this, localAddress, promise);
            } catch (Throwable cause) {
                promise.completeExceptionally(cause);
            }
        } else {
            bind(localAddress, promise);
        }
    }

    @Override
    public ChannelFuture<?> deregister() {
        return deregister(newPromise());
    }

    @Override
    public ChannelFuture<?> deregister(ChannelFuture<?> promise) {
        AbstractChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeDeregister(promise);
        } else {
            executor.execute(() -> next.invokeDeregister(promise));
        }
        return promise;
    }

    private void invokeDeregister(ChannelFuture<?> promise) {
        logger.debug("[{}] invokeDeregister()...", name);
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).deregister(this, promise);
            } catch (Throwable cause) {
                promise.completeExceptionally(cause);
            }
        } else {
            deregister(promise);
        }
    }

    @Override
    public ChannelFuture<?> close() {
        return close(newPromise());
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
                ((ChannelOutboundHandler) handler()).close(this, promise);
            } catch (Throwable cause) {
                promise.completeExceptionally(cause);
            }
        } else {
            close(promise);
        }
    }

    @Override
    public ChannelFuture<?> newPromise() {
        return new CompletableChannelFuture<>(channel());
    }
}
