package queue;

import junit.framework.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentMostRecentlyInsertedQueueTest extends JSR166TestCase {

    public static class Bounded extends BaseMostRecentlyInsertedQueueTest {
        @Override
        protected Queue emptyCollection(int capacity) {
            return new ConcurrentMostRecentlyInsertedQueue(capacity);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() {
                return ConcurrentMostRecentlyInsertedQueue.class;
            }

            public Collection emptyCollection() {
                return new ConcurrentMostRecentlyInsertedQueue(Integer.MAX_VALUE);
            }

            public Object makeElement(int i) {
                return i;
            }

            public boolean isConcurrent() {
                return true;
            }

            public boolean permitsNulls() {
                return false;
            }
        }
        return newTestSuite(ConcurrentMostRecentlyInsertedQueueTest.class,
                new Bounded().testSuite(),
                CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Integers 0 ... n - 1.
     */
    private ConcurrentMostRecentlyInsertedQueue<Integer> populatedQueue(int n) {
        ConcurrentMostRecentlyInsertedQueue<Integer> q = new ConcurrentMostRecentlyInsertedQueue<>(n);
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; ++i)
            assertTrue(q.offer(new Integer(i)));
        assertFalse(q.isEmpty());
        assertEquals(n, q.size());
        assertEquals((Integer) 0, q.peek());
        return q;
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

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        ConcurrentMostRecentlyInsertedQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        ConcurrentMostRecentlyInsertedQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.peek());
            assertEquals(i, q.poll());
            assertTrue(q.peek() == null ||
                    !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * remove removes next element, or throws NSEE if empty
     */
    public void testRemove() {
        ConcurrentMostRecentlyInsertedQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {
        }
    }

    /**
     * remove(x) removes x and returns true if present
     */
    public void testRemoveElement() {
        ConcurrentMostRecentlyInsertedQueue q = populatedQueue(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.contains(i));
            assertTrue(q.remove(i));
            assertFalse(q.contains(i));
            assertTrue(q.contains(i - 1));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.contains(i));
            assertTrue(q.remove(i));
            assertFalse(q.contains(i));
            assertFalse(q.remove(i + 1));
            assertFalse(q.contains(i + 1));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * clear removes all elements
     */
    public void testClear() {
        ConcurrentMostRecentlyInsertedQueue q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
        q.add(one);
        assertFalse(q.isEmpty());
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * removeAll(c) removes only those elements of c and reports true if changed
     */
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            ConcurrentMostRecentlyInsertedQueue q = populatedQueue(SIZE);
            ConcurrentMostRecentlyInsertedQueue p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            assertEquals(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                Integer x = (Integer) (p.remove());
                assertFalse(q.contains(x));
            }
        }
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(new ConcurrentMostRecentlyInsertedQueue(1).iterator());
    }

    /**
     * iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final ConcurrentMostRecentlyInsertedQueue q = new ConcurrentMostRecentlyInsertedQueue(3);
        q.add(one);
        q.add(two);
        q.add(three);

        int k = 0;
        for (Iterator it = q.iterator(); it.hasNext(); ) {
            assertEquals(++k, it.next());
        }

        assertEquals(3, k);
    }

    /**
     * Modifications do not cause iterators to fail
     */
    public void testWeaklyConsistentIteration() {
        final ConcurrentMostRecentlyInsertedQueue q = new ConcurrentMostRecentlyInsertedQueue(3);
        q.offer(one);
        q.offer(two);
        q.offer(three);

        for (Iterator it = q.iterator(); it.hasNext(); ) {
            q.remove();
            it.next();
        }

        assertEquals("queue should be empty again", 0, q.size());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final ConcurrentMostRecentlyInsertedQueue q = new ConcurrentMostRecentlyInsertedQueue(3);
        q.add(one);
        q.add(two);
        q.add(three);
        Iterator it = q.iterator();
        it.next();
        it.remove();
        it = q.iterator();
        assertSame(it.next(), two);
        assertSame(it.next(), three);
        assertFalse(it.hasNext());
    }

    public void testOfferInExecutor() throws InterruptedException {
        final ConcurrentMostRecentlyInsertedQueue<Integer> q = new ConcurrentMostRecentlyInsertedQueue<>(SIZE);
        final CheckedBarrier threadsStarted = new CheckedBarrier(SIZE);
        final CountDownLatch aboutToWait = new CountDownLatch(SIZE);
        final ExecutorService executor = Executors.newFixedThreadPool(SIZE);

        try (PoolCleaner cleaner = cleaner(executor)) {
            for (int i = 0; i < SIZE; i++) {
                final int value = i;
                executor.execute(new CheckedRunnable() {
                    public void realRun() {
                        threadsStarted.await();
                        assertTrue(q.offer(value));
                        aboutToWait.countDown();
                    }
                });
            }
        }

        aboutToWait.await();
        assertEquals(SIZE, q.size());
    }

    public void testPollInExecutor() throws InterruptedException {
        final ConcurrentMostRecentlyInsertedQueue<Integer> q = populatedQueue(SIZE);
        final CheckedBarrier threadsStarted = new CheckedBarrier(SIZE);
        final CountDownLatch pollDone = new CountDownLatch(SIZE);
        final ExecutorService executor = Executors.newFixedThreadPool(SIZE);
        final List<Integer> pollResults = Collections.synchronizedList(new ArrayList<Integer>());
        try (PoolCleaner cleaner = cleaner(executor)) {
            for (int i = 0; i < SIZE; i++) {
                executor.execute(new CheckedRunnable() {
                    public void realRun() {
                        threadsStarted.await();
                        Integer poll = q.poll();
                        assertNotNull(poll);
                        pollResults.add(poll);
                        pollDone.countDown();
                    }
                });
            }
        }

        pollDone.await();
        assertEquals(0, q.size());
        for (int i = 0; i < SIZE; i++) {
            assertTrue(pollResults.contains(i));
        }
    }

    public void testPeekInExecutor() throws InterruptedException {
        final ConcurrentMostRecentlyInsertedQueue<Integer> q = populatedQueue(SIZE);
        final CheckedBarrier threadsStarted = new CheckedBarrier(SIZE);
        final CountDownLatch peekDone = new CountDownLatch(SIZE);
        final ExecutorService executor = Executors.newFixedThreadPool(SIZE);

        try (PoolCleaner cleaner = cleaner(executor)) {
            for (int i = 0; i < SIZE; i++) {
                executor.execute(new CheckedRunnable() {
                    public void realRun() {
                        threadsStarted.await();
                        assertEquals((Integer) 0, q.peek());
                        peekDone.countDown();
                    }
                });
            }
        }

        peekDone.await();
        assertEquals(SIZE, q.size());
    }
}