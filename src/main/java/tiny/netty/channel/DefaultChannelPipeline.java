package tiny.netty.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tiny.netty.util.concurrent.EventExecutor;

import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;

/**
 * 通道管道默认实现
 *
 * @author zhaomingming
 */
public class DefaultChannelPipeline implements ChannelPipeline {

    private final Channel channel;
    private final AbstractChannelHandlerContext head;
    private final AbstractChannelHandlerContext tail;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private boolean registered = false;
    private volatile PendingHandlerCallback pendingHandlerCallbackHead;

    public DefaultChannelPipeline(Channel channel) {
        this.channel = channel;
        this.head = new HeadContext("head");
        this.tail = new TailContext("tail");
        this.head.next = tail;
        this.tail.prev = head;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        AbstractChannelHandlerContext ctx;
        synchronized (this) {
            // TODO 检查 name 是否重复
            ctx = newContext(null, name, handler);
            addFirst0(ctx);
            // 通道还未注册至事件循环中, handlerAdded()回调方法需要后置处理
            if (!registered) {
                ctx.setAddPending();
                callHandlerCallbackLater(ctx, true);
                return this;
            }
        }
        // 已注册
        EventExecutor executor = ctx.executor();
        if (!executor.inEventLoop()) {
            ctx.setAddPending();
            executor.execute(() -> callHandlerAdded0(ctx));
        } else {
            callHandlerAdded0(ctx);
        }
        return this;
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
        AbstractChannelHandlerContext ctx;
        synchronized (this) {
            // TODO 检查 name 是否重复
            ctx = newContext(null, name, handler);
            addLast0(ctx);
            // 通道还未注册至事件循环中
            if (!registered) {
                ctx.setAddPending();
                callHandlerCallbackLater(ctx, true);
                return this;
            }
        }
        // 已注册
        EventExecutor executor = ctx.executor();
        if (!executor.inEventLoop()) {
            ctx.setAddPending();
            executor.execute(() -> callHandlerAdded0(ctx));
        } else {
            callHandlerAdded0(ctx);
        }
        return this;
    }

    @Override
    public ChannelPipeline remove(String name) {
        remove(context(name));
        return this;
    }

    @Override
    public ChannelPipeline remove(ChannelHandler handler) {
        remove(context(handler));
        return this;
    }

    private void remove(AbstractChannelHandlerContext ctx) {
        if (ctx == null) {
            return;
        }
        synchronized (this) {
            remove0(ctx);
            if (!registered) {
                callHandlerCallbackLater(ctx, false);
                return;
            }
        }
        // 已注册
        EventExecutor executor = ctx.executor();
        if (!executor.inEventLoop()) {
            executor.execute(() -> callHandlerRemoved0(ctx));
        } else {
            callHandlerRemoved0(ctx);
        }
    }

    private void callHandlerAdded0(AbstractChannelHandlerContext ctx) {
        try {
            ctx.callHandlerAdded();
        } catch (Throwable cause) {
            boolean removed = false;
            try {
                // TODO
                synchronized (this) {
                    remove0(ctx);
                }
                ctx.callHandlerRemoved();
                removed = true;
            } catch (Throwable e) {
                // ignore
            }
            if (removed) {
                // TODO
                fireExceptionCaught(new Exception(ctx.handler().getClass().getName() +
                        ".handlerAdded() has thrown an exception; removed., removed.", cause));
            } else {
                fireExceptionCaught(new Exception(ctx.handler().getClass().getName() +
                        ".handlerAdded() has thrown an exception; also failed to remove.", cause));
            }
            ctx.setRemoved();
        }
    }

    private void callHandlerRemoved0(AbstractChannelHandlerContext ctx) {
        try {
            ctx.callHandlerRemoved();
        } catch (Throwable cause) {
            // TODO
            fireExceptionCaught(new Exception(new Exception(ctx.handler().getClass().getName() + ".handlerRemoved() has thrown an exception", cause)));
        }
    }

    private void callHandlerCallbackLater(AbstractChannelHandlerContext ctx, boolean added) {
        PendingHandlerCallback newTask = added ? new PendingHandlerAddedTask(ctx) : new PendingHandlerRemovedTask(ctx);
        PendingHandlerCallback prev = pendingHandlerCallbackHead;
        if (prev == null) {
            pendingHandlerCallbackHead = newTask;
        } else {
            while (prev.next != null) {
                prev = prev.next;
            }
            prev.next = newTask;
        }
    }

    private AbstractChannelHandlerContext newContext(EventExecutor executor, String name, ChannelHandler handler) {
        return new DefaultChannelHandlerContext(this, executor, handler, name);
    }

    private void addFirst0(AbstractChannelHandlerContext ctx) {
        AbstractChannelHandlerContext next = head.next;
        head.next = ctx;
        next.prev = ctx;
        ctx.prev = head;
        ctx.next = next;
    }

    private void addLast0(AbstractChannelHandlerContext ctx) {
        AbstractChannelHandlerContext prev = tail.prev;
        prev.next = ctx;
        tail.prev = ctx;
        ctx.prev = prev;
        ctx.next = tail;
    }

    private void remove0(AbstractChannelHandlerContext ctx) {
        AbstractChannelHandlerContext prev = ctx.prev;
        AbstractChannelHandlerContext next = ctx.next;
        prev.next = next;
        next.prev = prev;
    }

    @Override
    public AbstractChannelHandlerContext context(String name) {
        return context0(name);
    }

    @Override
    public AbstractChannelHandlerContext context(ChannelHandler handler) {
        return context0(handler);
    }

    @Override
    public ChannelHandler get(String name) {
        ChannelHandlerContext ctx = context0(name);
        return ctx == null ? null : ctx.handler();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ChannelHandler> T get(Class<T> handlerType) {
        ChannelHandlerContext ctx = context0(handlerType);
        return ctx == null ? null : (T) ctx.handler();
    }

    private AbstractChannelHandlerContext context0(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        AbstractChannelHandlerContext ctx = head;
        while ((ctx = ctx.next) != tail) {
            if (ctx.name().equals(name)) {
                return ctx;
            }
        }
        return null;
    }

    private AbstractChannelHandlerContext context0(ChannelHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        AbstractChannelHandlerContext ctx = head;
        while ((ctx = ctx.next) != tail) {
            if (ctx.handler() == handler) {
                return ctx;
            }
        }
        return null;
    }

    private <T extends ChannelHandler> ChannelHandlerContext context0(Class<T> handlerType) {
        if (handlerType == null) {
            throw new IllegalArgumentException("handlerType is null");
        }
        AbstractChannelHandlerContext ctx = head;
        while ((ctx = ctx.next) != tail) {
            if (handlerType.isAssignableFrom(ctx.handler().getClass())) {
                return ctx;
            }
        }
        return null;
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        AbstractChannelHandlerContext.invokeChannelRegistered(head);
        return this;
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        AbstractChannelHandlerContext.invokeChannelUnregistered(head);
        return this;
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        return this;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        return this;
    }

    @Override
    public ChannelPipeline fireExceptionCaught(Throwable cause) {
        return this;
    }

    @Override
    public ChannelFuture<?> bind(SocketAddress localAddress, ChannelFuture<?> promise) {
        return promise;
    }

    @Override
    public ChannelFuture<?> close(ChannelFuture<?> promise) {
        return promise;
    }

    protected void callHandlerAddedForAllHandlers() {
        PendingHandlerCallback task;
        synchronized (this) {
            assert !registered;
            registered = true;
            task = this.pendingHandlerCallbackHead;
            this.pendingHandlerCallbackHead = null;
        }
        while (task != null) {
            task.execute();
            task = task.next;
        }
    }

    private abstract static class PendingHandlerCallback implements Runnable {
        protected final AbstractChannelHandlerContext ctx;
        private PendingHandlerCallback next;

        PendingHandlerCallback(AbstractChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        abstract void execute();
    }

    class PendingHandlerAddedTask extends PendingHandlerCallback {

        PendingHandlerAddedTask(AbstractChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        void execute() {
            EventExecutor executor = ctx.executor();
            if (executor.inEventLoop()) {
                callHandlerAdded0(ctx);
            } else {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException e) {
                    logger.warn(
                            "Can't invoke handlerAdded() as the EventExecutor {} rejected it, removing handler {}.",
                            executor, ctx.name(), e);
                    synchronized (this) {
                        remove0(ctx);
                    }
                    ctx.setRemoved();
                }
            }
        }

        @Override
        public void run() {
            callHandlerAdded0(ctx);
        }
    }

    class PendingHandlerRemovedTask extends PendingHandlerCallback {
        PendingHandlerRemovedTask(AbstractChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        void execute() {
            EventExecutor executor = ctx.executor();
            if (executor.inEventLoop()) {
                callHandlerRemoved0(ctx);
            } else {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException e) {
                    logger.warn(
                            "Can't invoke handlerRemoved() as the EventExecutor {} rejected it, removing handler {}.",
                            executor, ctx.name(), e);
                }
            }
        }

        @Override
        public void run() {
            callHandlerRemoved0(ctx);
        }
    }

    class HeadContext extends AbstractChannelHandlerContext implements ChannelInboundHandler, ChannelOutboundHandler {

        HeadContext(String name) {
            super(DefaultChannelPipeline.this, channel.eventLoop(), name, true, true);
        }

        @Override
        public ChannelHandler handler() {
            return this;
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            ctx.fireChannelRegistered();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            ctx.fireChannelUnregistered();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {

        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {

        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelFuture<?> promise) throws Exception {

        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelFuture<?> promise) throws Exception {

        }
    }

    class TailContext extends AbstractChannelHandlerContext implements ChannelInboundHandler {

        TailContext(String name) {
            super(DefaultChannelPipeline.this, channel.eventLoop(), name, true, false);
        }

        @Override
        public ChannelHandler handler() {
            return this;
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {

        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {

        }
    }
}
