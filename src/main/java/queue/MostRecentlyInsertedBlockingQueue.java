package queue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by VitaliiRiabtsev on 11/17/2016.
 */
public class MostRecentlyInsertedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private static class Node<E> {
        E element;
        Node<E> next;

        Node(E element) {
            this.element = element;
        }
    }

    private final int capacity;
    private final AtomicInteger count = new AtomicInteger();

    private Node<E> head;
    private Node<E> tail;

    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();

    private final ReentrantLock putLock = new ReentrantLock();

    public MostRecentlyInsertedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        tail = head = new Node<>(null);
    }

    private void enqueue(Node<E> node) {
        tail = tail.next = node;
    }

    private E dequeue() {
        Node<E> first = head.next; //hold first node, to make it as head of queue

        head.next = head; // remove head node from queue
        head = first; // make first node as head of queue
        E removedElement = first.element; //hold first element for return it
        head.element = null; // remove first element from head

        return removedElement;
    }

    private void signalNotEmpty() {
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    private class IteratorImpl implements Iterator<E> {
        private Node<E> current;

        private IteratorImpl() {
            current = head.next;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public E next() {
            if (current == null) {
                throw new NoSuchElementException();
            }

            E element = current.element;
            current = current.next;
            return element;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new IteratorImpl();
    }

    @Override
    public int size() {
        return count.get();
    }

    @Override
    public void put(E element) throws InterruptedException {
        if (element == null) throw new NullPointerException();

        Node<E> node = new Node<>(element);
        int currentCount = -1;

        putLock.lockInterruptibly();
        try {
            currentCount = count.get();

            if (currentCount == capacity) {
                takeLock.lockInterruptibly();
                try {
                    dequeue();
                } finally {
                    takeLock.unlock();
                }
                enqueue(node);
            } else {
                enqueue(node);
                currentCount = count.incrementAndGet();
            }

        } finally {
            putLock.unlock();
        }

        if (currentCount == 1) {
            signalNotEmpty();
        }
    }

    @Override
    public boolean offer(E element, long timeout, TimeUnit unit) throws InterruptedException {
        if (element == null) throw new NullPointerException();

        Node<E> node = new Node<>(element);
        int currentCount = -1;

        if (!putLock.tryLock(timeout, unit)) {
            return false;
        }
        try {
            currentCount = count.get();

            if (currentCount == capacity) {
                takeLock.lockInterruptibly();
                try {
                    dequeue();
                } finally {
                    takeLock.unlock();
                }
                enqueue(node);
            } else {
                enqueue(node);
                currentCount = count.incrementAndGet();
            }

        } finally {
            putLock.unlock();
        }

        if (currentCount == 1) {
            signalNotEmpty();
        }

        return true;
    }


    @Override
    public E take() throws InterruptedException {
        E removedElement;

        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            removedElement = dequeue();
            int currentCount = count.decrementAndGet();
            if (currentCount > 0) {
                notEmpty.signalAll();
            }
        } finally {
            takeLock.unlock();
        }

        return removedElement;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E removedElement = null;
        long nanos = unit.toNanos(timeout);
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            removedElement = dequeue();
            int currentCount = count.decrementAndGet();
            if (currentCount > 0) {
                notEmpty.signalAll();
            }
        } finally {
            takeLock.unlock();
        }

        return removedElement;
    }

    @Override
    public int remainingCapacity() {
        return capacity - count.get();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> collection, int maxElements) {
        if (collection == null)
            throw new NullPointerException();
        if (collection == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int numberOfElements = Math.min(maxElements, count.get());
            int drainCount = 0;

            while (drainCount < numberOfElements) {
                Node<E> currentNode = head.next;
                collection.add(currentNode.element);
                currentNode.element = null;
                head.next = head;
                head = currentNode;
                drainCount += 1;
            }

            if (count.addAndGet(-drainCount) > 0) {
                notEmpty.signalAll();
            }
            return numberOfElements;
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public boolean offer(E element) {
        if (element == null) throw new NullPointerException();

        int currentCount = -1;
        Node<E> node = new Node<>(element);
        putLock.lock();
        try {
            currentCount = count.get();

            if (currentCount == capacity) {
                takeLock.lock();
                try {
                    dequeue();
                } finally {
                    takeLock.unlock();
                }
                enqueue(node);
            } else {
                enqueue(node);
                currentCount = count.incrementAndGet();
            }

        } finally {
            putLock.unlock();
        }

        if (currentCount == 1) {
            signalNotEmpty();
        }

        return true;
    }

    @Override
    public E poll() {
        if (count.get() == 0)
            return null;
        E element = null;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                element = dequeue();
                int currentCount = count.decrementAndGet();
                if (currentCount > 0) {
                    notEmpty.signalAll();
                }
            }
        } finally {
            takeLock.unlock();
        }

        return element;
    }

    @Override
    public E peek() {
        if (count.get() == 0)
            return null;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null)
                return null;
            else
                return first.element;
        } finally {
            takeLock.unlock();
        }
    }

}
