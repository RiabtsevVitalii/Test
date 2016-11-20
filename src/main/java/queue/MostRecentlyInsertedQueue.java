package queue;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class MostRecentlyInsertedQueue<E> extends AbstractQueue<E> implements Queue<E> {

    private static class Node<E> {
        E element;
        Node<E> next;

        Node(E element) {
            this.element = element;
        }
    }

    private Node<E> head;
    private Node<E> tail;

    private final int capacity;
    private int count;

    public MostRecentlyInsertedQueue(int capacity) {
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

    void unlink(Node<E> nextNode, Node<E> currentNode) {
        nextNode.element = null;
        currentNode.next = nextNode.next;
        if (tail == nextNode) {
            tail = currentNode;
        }
        count -= 1;
    }

    private class IteratorImpl implements Iterator<E> {
        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        private IteratorImpl() {
            current = head.next;
            if (current != null) {
                currentElement = current.element;
            }
        }

        private Node<E> nextNode(Node<E> currentNode) {
            for (; ; ) {
                Node<E> nextNode = currentNode.next;
                if (nextNode == currentNode) {
                    return head.next;
                }
                if (nextNode == null || nextNode.element != null) {
                    return nextNode;
                }
                currentNode = nextNode;
            }
        }

        public boolean hasNext() {
            return current != null;
        }

        public E next() {
            if (current == null) {
                throw new NoSuchElementException();
            }
            E element = currentElement;
            lastRet = current;
            current = nextNode(current);
            currentElement = (current == null) ? null : current.element;
            return element;
        }

        public void remove() {
            if (lastRet == null) {
                throw new IllegalStateException();
            }

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
        enqueue(node);
        if (count < capacity) {
            count += 1;
        } else {
            dequeue();
        }

        return true;
    }

    @Override
    public E poll() {
        if (count == 0) {
            return null;
        }
        E removedElement = dequeue();
        count -= 1;

        return removedElement;
    }

    @Override
    public E peek() {
        if (count == 0) {
            return null;
        }

        Node<E> first = head.next;
        return first.element;
    }
}
