package tiny.netty.util.concurrent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 测试
 *
 * @author zhaomingming
 */
public class EventExecutorChooserTest {

    @Test
    public void test() {
        for (int i = 1; i < 16; i++)
            test(i);
    }

    private void test(int length) {
        EventExecutor[] executors = new EventExecutor[length];
        for (int i = 0; i < length; i++) {
            executors[i] = mock(EventExecutor.class);
        }
        EventExecutorChooser chooser = EventExecutorChooserFactory.INSTANCE.newChooser(executors);
        for (int i = 0; i < (length << 2); i++) {
            assertThat(chooser.next()).isEqualTo(executors[i % length]);
        }
    }
}