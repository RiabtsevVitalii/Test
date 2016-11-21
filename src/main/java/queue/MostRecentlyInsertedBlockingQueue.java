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
        Node<E> firstNode = head.next;

        head.next = head;
        head = firstNode;
        E removedElement = firstNode.element;
        firstNode.element = null;

        return removedElement;
    }

    public boolean remove(Object object) {
        if (object == null) return false;
        putLock.lock();
        takeLock.lock();
        try {
            for (Node<E> currentNode = head, nextNode = currentNode.next;
                 nextNode != null;
                 currentNode = nextNode, nextNode = nextNode.next) {
                if (object.equals(nextNode.element)) {
                    unlink(nextNode, currentNode);
                    return true;
                }
            }
            return false;
        } finally {
            putLock.unlock();
            takeLock.unlock();
        }
    }

    private void signalNotEmpty() {
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    void unlink(Node<E> nextNode, Node<E> currentNode) {
        nextNode.element = null;
        currentNode.next = nextNode.next;
        if (tail == nextNode) {
            tail = currentNode;
        }
        count.getAndDecrement();
    }

    private class IteratorImpl implements Iterator<E> {
        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        private IteratorImpl() {
            putLock.lock();
            takeLock.lock();
            try {
                current = head.next;
                if (current != null)
                    currentElement = current.element;
            } finally {
                putLock.unlock();
                takeLock.unlock();
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        private Node<E> nextNode(Node<E> currentNode) {
            while (true) {
                Node<E> nextNode = currentNode.next;
                if (nextNode == currentNode)
                    return head.next;
                if (nextNode == null || nextNode.element != null)
                    return nextNode;
                currentNode = nextNode;
            }
        }

        public E next() {
            putLock.lock();
            takeLock.lock();
            try {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                E element = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.element;
                return element;
            } finally {
                putLock.unlock();
                takeLock.unlock();
            }
        }

        public void remove() {
            if (lastRet == null)
                throw new IllegalStateException();
            putLock.lock();
            takeLock.lock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> currentNode = head, nextNode = currentNode.next;
                     nextNode != null;
                     currentNode = nextNode, nextNode = nextNode.next) {
                    if (nextNode == node) {
                        unlink(nextNode, currentNode);
                        break;
                    }
                }
            } finally {
                putLock.unlock();
                takeLock.unlock();
            }
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
            if (count.get() == capacity) {
                takeLock.lockInterruptibly();
                try {
                    if (count.get() == capacity) {
                        dequeue();
                        count.decrementAndGet();
                    }
                } finally {
                    takeLock.unlock();
                }
            }

            enqueue(node);
            currentCount = count.incrementAndGet();
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
            if (count.get() == capacity) {
                takeLock.lockInterruptibly();
                try {
                    if (count.get() == capacity) {
                        dequeue();
                        count.decrementAndGet();
                    }
                } finally {
                    takeLock.unlock();
                }
            }
            enqueue(node);
            currentCount = count.incrementAndGet();
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
            if (count.get() == capacity) {
                takeLock.lock();
                try {
                    if (count.get() == capacity) {
                        dequeue();
                        count.decrementAndGet();
                    }
                } finally {
                    takeLock.unlock();
                }
            }
            enqueue(node);
            currentCount = count.incrementAndGet();
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
