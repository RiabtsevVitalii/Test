package queue;

import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

/**
 * Created by VitaliiRiabtsev on 11/16/2016.
 */
public class MostRecentlyInsertedQueueTest {

    @Test(expected = IllegalArgumentException.class)
    public void instantQueueWithIllegalCapacity() {
        new MostRecentlyInsertedQueue(0);
    }

    @Test
    public void instantQueueWithNormalCapacity() {
        assertNotNull(new MostRecentlyInsertedQueue(1));
    }

    @Test
    public void sizeOfEmptyQueue() {
        MostRecentlyInsertedQueue<String> queue = new MostRecentlyInsertedQueue<>(1);
        int expectSize = 0;
        assertEquals(expectSize, queue.size());
    }

    @Test(expected = NullPointerException.class)
    public void offerNullValue() {
        int capacity = 1;

        MostRecentlyInsertedQueue<String> queue = new MostRecentlyInsertedQueue<>(capacity);
        queue.offer(null);
    }

    @Test    public void offer() {
        int capacity = 2;
        MostRecentlyInsertedQueue<String> queue = new MostRecentlyInsertedQueue<>(capacity);

        //offer first element with value="first". expecting size=1, expecting return value="first" by peek()
        int expectSize = 1;
        String item1 = "first";
        assertTrue(queue.offer(item1));
        assertEquals(expectSize, queue.size());
        assertEquals(item1, queue.peek());

        //offer second element with value="second". expecting size=2, expecting return value="first" by peek()
        expectSize = 2;
        String item2 = "second";
        assertTrue(queue.offer(item2));
        assertEquals(expectSize, queue.size());
        assertEquals(item1, queue.peek());

        //offer third element with value="third". expecting size=2, expecting return value="second" by peek()
        expectSize = 2;
        String item3 = "third";
        assertTrue(queue.offer(item3));
        assertEquals(expectSize, queue.size());
        assertEquals(item2, queue.peek());

        //expecting return value="second" by poll(), expecting return value="third" by peek(), expecting size=1
        expectSize = 1;
        assertEquals(item2, queue.poll());
        assertEquals(item3, queue.peek());
        assertEquals(expectSize, queue.size());

        //clear queue, expecting return value=NULL by peek(), expecting return value=NULL by poll(),expecting size=0
        expectSize = 0;
        queue.clear();
        assertEquals(expectSize, queue.size());
        assertNull(queue.peek());
        assertNull(queue.poll());
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyQueueIterator() {
        int capacity = 1;
        MostRecentlyInsertedQueue<String> queue = new MostRecentlyInsertedQueue<>(capacity);

        Iterator<String> iterator = queue.iterator();
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());

        iterator.next(); //must throw NoSuchElementException
    }

    @Test
    public void queueIterator() {
        int capacity = 2;
        MostRecentlyInsertedQueue<String> queue = new MostRecentlyInsertedQueue<>(capacity);
        String item1 = "first";
        queue.offer(item1);
        String item2 = "second";
        queue.offer(item2);

        Iterator<String> iterator = queue.iterator();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        assertEquals(item1, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(item2, iterator.next());

        assertFalse(iterator.hasNext());
    }
}