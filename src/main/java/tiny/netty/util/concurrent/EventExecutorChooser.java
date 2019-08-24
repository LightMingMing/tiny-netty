package tiny.netty.util.concurrent;

/**
 * 事件执行选择器
 *
 * @author zhaomingming
 */
public interface EventExecutorChooser {

    EventExecutor next();
}
