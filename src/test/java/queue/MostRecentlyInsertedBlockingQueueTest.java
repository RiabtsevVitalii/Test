package queue;

import junit.framework.Test;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MostRecentlyInsertedBlockingQueueTest extends JSR166TestCase {

    public static class Bounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new MostRecentlyInsertedBlockingQueue(SIZE);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() {
                return MostRecentlyInsertedBlockingQueue.class;
            }

            public Collection emptyCollection() {
                return new MostRecentlyInsertedBlockingQueue(Integer.MAX_VALUE);
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

        return newTestSuite(MostRecentlyInsertedBlockingQueueTest.class,
                new Bounded().testSuite(),
                CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Integers 0 ... n - 1.
     */
    private MostRecentlyInsertedBlockingQueue<Integer> populatedQueue(int n) {
        MostRecentlyInsertedBlockingQueue<Integer> q =
                new MostRecentlyInsertedBlockingQueue<>(n);
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; i++)
            assertTrue(q.offer(new Integer(i)));
        assertFalse(q.isEmpty());
        assertEquals(0, q.remainingCapacity());
        assertEquals(n, q.size());
        assertEquals((Integer) 0, q.peek());
        return q;
    }

    /**
     * A new queue has the indicated capacity
     */
    public void testConstructor1() {
        assertEquals(SIZE, new MostRecentlyInsertedBlockingQueue(SIZE).remainingCapacity());
    }

    /**
     * Constructor throws IllegalArgumentException if capacity argument nonpositive
     */
    public void testConstructor2() {
        try {
            new MostRecentlyInsertedBlockingQueue(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {
        }
    }

    /**
     * Queue transitions from empty to full when elements added
     */
    public void testEmptyFull() {
        MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(2);
        assertTrue(q.isEmpty());
        assertEquals("should have room for 2", 2, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        q.add(two);
        assertFalse(q.isEmpty());
        assertEquals(0, q.remainingCapacity());
        assertTrue(q.offer(three));
        assertFalse(q.isEmpty());
        assertEquals(0, q.remainingCapacity());

    }

    /**
     * remainingCapacity decreases on add, increases on remove
     */
    public void testRemainingCapacity() {
        BlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.remainingCapacity());
            assertEquals(SIZE, q.size() + q.remainingCapacity());
            assertEquals(i, q.remove());
        }
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE - i, q.remainingCapacity());
            assertEquals(SIZE, q.size() + q.remainingCapacity());
            assertTrue(q.add(i));
        }
    }

    /**
     * Offer succeeds anyway
     */
    public void testOffer() {
        MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(1);
        assertTrue(q.offer(zero));
        assertTrue(q.offer(one));
    }

    /**
     * all elements successfully put are contained
     */
    public void testPut() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            Integer x = new Integer(i);
            q.put(x);
            assertTrue(q.contains(x));
        }
        assertEquals(0, q.remainingCapacity());
    }

    /**
     * put blocks interruptibly waiting for take when full
     */
    public void testTakeWithPut() throws InterruptedException {
        final int capacity = 2;
        final MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(1);
        final CountDownLatch pleaseTake = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                await(pleaseTake);
                assertEquals(0, q.remainingCapacity());
                assertEquals(1, q.take());

                pleaseInterrupt.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                }
                assertFalse(Thread.interrupted());
            }
        });


        q.put(1);
        pleaseTake.countDown();

        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
        assertEquals(1, q.remainingCapacity());
    }

    /**
     * take retrieves elements in FIFO order
     */
    public void testTake() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.take());
        }
    }

    /**
     * Take removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTake() throws InterruptedException {
        final BlockingQueue q = populatedQueue(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, q.take());
                }

                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                }
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                }
                assertFalse(Thread.interrupted());
            }
        });

        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue<Integer> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
        long startTime = System.nanoTime();
        assertNull(q.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        checkEmpty(q);
    }

    /**
     * Interrupted timed poll throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPoll() throws InterruptedException {
        final BlockingQueue<Integer> q = populatedQueue(SIZE);
        final CountDownLatch aboutToWait = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
                }
                aboutToWait.countDown();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
                }
            }
        });

        await(aboutToWait);
        waitForThreadToEnterWaitState(t);
        t.interrupt();
        awaitTermination(t);
        checkEmpty(q);
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
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
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
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
     * An add following remove(x) succeeds
     */
    public void testRemoveElementAndOffer() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(Integer.MAX_VALUE);
        assertTrue(q.offer(new Integer(1)));
        assertTrue(q.offer(new Integer(2)));
        assertTrue(q.remove(new Integer(1)));
        assertTrue(q.remove(new Integer(2)));
        assertTrue(q.offer(new Integer(3)));
        assertNotNull(q.take());
    }

    /**
     * clear removes all elements
     */
    public void testClear() {
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
        assertEquals(SIZE, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        assertTrue(q.contains(one));
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue q = populatedQueue(SIZE);
        Iterator it = q.iterator();
        int i;
        for (i = 0; it.hasNext(); i++)
            assertTrue(q.contains(it.next()));
        assertEquals(i, SIZE);
        assertIteratorExhausted(it);

        it = q.iterator();
        for (i = 0; it.hasNext(); i++)
            assertEquals(it.next(), q.take());
        assertEquals(i, SIZE);
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(new MostRecentlyInsertedBlockingQueue(Integer.MAX_VALUE).iterator());
    }

    /**
     * iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(3);
        q.add(one);
        q.add(two);
        q.add(three);
        assertEquals(0, q.remainingCapacity());
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
        final MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(3);
        q.add(one);
        q.add(two);
        q.add(three);
        for (Iterator it = q.iterator(); it.hasNext(); ) {
            q.remove();
            it.next();
        }
        assertEquals(0, q.size());
    }

    public void testPollAndOfferInExecutor() {
        final MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(2);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final CountDownLatch aboutToWait = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {
            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertNull(q.poll(SHORT_DELAY_MS, MILLISECONDS));
                    assertEquals(2, q.remainingCapacity());
                    threadsStarted.await();
                    aboutToWait.await();
                    assertSame(one, q.poll());
                }
            });

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    assertTrue(q.offer(one));
                    aboutToWait.countDown();
                }
            });
        }
    }

    public void testPollAntPullInExecutor() {
        final MostRecentlyInsertedBlockingQueue q = new MostRecentlyInsertedBlockingQueue(2);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {
            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertNull(q.poll());
                    threadsStarted.await();
                    assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                    checkEmpty(q);
                }
            });

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    q.put(one);
                }
            });
        }
    }
}