package queue;

import java.util.Collection;
import java.util.Queue;

/**
 * Created by VitaliiRiabtsev on 11/16/2016.
 */
public class MostRecentlyInsertedQueueTest extends JSR166TestCase {

    public static class Bounded extends BaseMostRecentlyInsertedQueueTest {
        @Override
        protected Queue emptyCollection(int capacity) {
            return new MostRecentlyInsertedQueue(capacity);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static junit.framework.Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() {
                return MostRecentlyInsertedQueue.class;
            }

            public Collection emptyCollection() {
                return new MostRecentlyInsertedQueue(Integer.MAX_VALUE);
            }

            public Object makeElement(int i) {
                return i;
            }

            public boolean isConcurrent() {
                return false;
            }

            public boolean permitsNulls() {
                return false;
            }
        }
        return newTestSuite(MostRecentlyInsertedQueueTest.class,
                new Bounded().testSuite(),
                CollectionTest.testSuite(new Implementation()));
    }

    public void testConstructor1() {
        try {
            new MostRecentlyInsertedQueue(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {

        }
    }

    public void testConstructor2() {
        assertNotNull(new MostRecentlyInsertedQueue(1));
    }

    public void testSizeOfEmptyQueue() {
        MostRecentlyInsertedQueue queue = new MostRecentlyInsertedQueue(1);
        int expectSize = 0;
        assertEquals(expectSize, queue.size());
    }
}