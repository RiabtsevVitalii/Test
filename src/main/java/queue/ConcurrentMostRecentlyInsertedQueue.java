package queue;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Created by VitaliiRiabtsev on 11/17/2016.
 */
public class ConcurrentMostRecentlyInsertedQueue<E> extends AbstractQueue<E> implements Queue<E> {
    private volatile Node<E> head;
    private volatile Node<E> tail;

    private final int capacity;
    private volatile int count;

    static class Node<E> {
        volatile E element;
        volatile Node<E> next;

        Node(E element) {
            this.element = element;
        }
    }

    public ConcurrentMostRecentlyInsertedQueue(int capacity) {
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
        return count;
    }

    @Override
    public boolean offer(E element) {
        if (element == null) {
            throw new NullPointerException();
        }

        Node<E> node = new Node<>(element);

        synchronized (this) {
            enqueue(node);
            if (count < capacity) {
                count += 1;
            } else {
                dequeue();
            }
        }
        return true;
    }

    @Override
    public synchronized E poll() {
        if (count == 0) {
            return null;
        }
        E removedElement = dequeue();
        count -= 1;

        return removedElement;
    }

    @Override
    public synchronized E peek() {
        if (count == 0) {
            return null;
        }

        Node<E> first = head.next;
        return first.element;
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }
}
