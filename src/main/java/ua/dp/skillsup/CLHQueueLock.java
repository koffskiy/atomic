package ua.dp.skillsup;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by User on 21.11.2014.
 */
public class CLHQueueLock {
    private final AtomicReference<Qnode> tail = new AtomicReference<Qnode>();

    private final ThreadLocal<Qnode> myNode = new ThreadLocal<Qnode>() {
        @Override
        protected Qnode initialValue() {
            return new Qnode();
        }
    };

    private final ThreadLocal<Qnode> myPred = new ThreadLocal<Qnode>();

    public CLHQueueLock() {
        final Qnode qnode = new Qnode();
        qnode.locked = false;

        tail.set(qnode);
    }

    public void lock() {
        final Qnode localNode = myNode.get();
        localNode.locked = true;

        final Qnode pred = tail.getAndSet(localNode);
        myPred.set(pred);

        if (pred.locked) {
            pred.parkedWriter = Thread.currentThread();
            LockSupport.park(this);
        }
    }

    public void unlock() {
        Qnode localNode = myNode.get();
        localNode.locked = false;
        Thread parkedReader;
        do {
            parkedReader = localNode.parkedReaders.poll();
            LockSupport.unpark(parkedReader);
        } while (parkedReader != null);
        LockSupport.unpark(localNode.parkedWriter);
        myNode.set(myPred.get());
    }

    public boolean isLocked() {
        return tail.get().locked;
    }

    public AtomicReference<Qnode> getTail() {
        return tail;
    }

    public static final class Qnode {
        volatile boolean locked = true;
        Queue<Thread> parkedReaders = new ConcurrentLinkedQueue<Thread>();
        Thread parkedWriter;
    }
}
