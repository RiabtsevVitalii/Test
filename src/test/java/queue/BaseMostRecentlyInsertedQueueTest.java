package queue;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public abstract class BaseMostRecentlyInsertedQueueTest extends JSR166TestCase {

    public Test testSuite() {
        return new TestSuite(this.getClass());
    }

    protected abstract Queue emptyCollection(int capacity);

    protected Object makeElement(int i) {
        return Integer.valueOf(i);
    }

    public void testOfferNullValue() {
        int capacity = 1;
        Queue queue = emptyCollection(capacity);

        try {
            queue.offer(null);
            shouldThrow();
        } catch (NullPointerException success) {
        }
    }

    public void testPollFromEmptyQueue() {
        Queue queue = emptyCollection(1);
        assertNull(queue.poll());
    }

    public void testPeekFromEmptyQueue() {
        Queue queue = emptyCollection(1);
        assertNull(queue.peek());
    }

    public void testOfferPollPeek() {
        int capacity = 2;
        Queue queue = emptyCollection(capacity);

        //offer first element with value=1. expecting size=1, expecting return value="first" by peek()
        int expectSize = 1;
        Object item1 = makeElement(1);
        assertTrue(queue.offer(item1));
        assertEquals(expectSize, queue.size());
        assertSame(item1, queue.peek());

        //offer second element with value=2. expecting size=2, expecting return value="first" by peek()
        expectSize = 2;
        Object item2 = makeElement(2);
        assertTrue(queue.offer(item2));
        assertEquals(expectSize, queue.size());
        assertSame(item1, queue.peek());

        //offer third element with value=3. expecting size=2, expecting return value="second" by peek()
        expectSize = 2;
        Object item3 = makeElement(3);
        assertTrue(queue.offer(item3));
        assertEquals(expectSize, queue.size());
        assertSame(item2, queue.peek());

        //expecting return value="second" by poll(), expecting return value="third" by peek(), expecting size=1
        expectSize = 1;
        assertSame(item2, queue.poll());
        assertSame(item3, queue.peek());
        assertEquals(expectSize, queue.size());

        //clear queue, expecting return value=NULL by peek(), expecting return value=NULL by poll(),expecting size=0
        expectSize = 0;
        queue.clear();
        assertEquals(expectSize, queue.size());
        assertNull(queue.peek());
        assertNull(queue.poll());
    }

    public void testEmptyQueueIterator() {
        int capacity = 1;
        Queue<String> queue = emptyCollection(capacity);

        Iterator<String> iterator = queue.iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    public void testQueueIterator() {
        int capacity = 2;
        Queue queue = emptyCollection(capacity);
        Object item1 = makeElement(1);
        queue.offer(item1);
        Object item2 = makeElement(2);
        queue.offer(item2);

        Iterator<String> iterator = queue.iterator();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        assertSame(item1, iterator.next());

        assertTrue(iterator.hasNext());
        assertSame(item2, iterator.next());

        assertFalse(iterator.hasNext());
    }
}
