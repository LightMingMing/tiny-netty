package tiny.netty.util.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程工厂
 *
 * @author zhaomingming
 */
public class DefaultThreadFactory implements ThreadFactory {

    private final static AtomicInteger factoryNumber = new AtomicInteger();

    private AtomicInteger index = new AtomicInteger();
    private String prefixName;

    public DefaultThreadFactory(String prefix) {
        this.prefixName = prefix + "-" + factoryNumber.incrementAndGet() + "-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(prefixName + index.incrementAndGet());
        return thread;
    }
}
