package ua.dp.skillsup;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class OReadersWriterSpinLock {

    AtomicLong readerCount = new AtomicLong();
    AtomicBoolean writer = new AtomicBoolean();

    Queue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

    public void acquireReadLock() {
        while (true) {
            readerCount.incrementAndGet();
            if (writer.get()) {
                readerCount.decrementAndGet();

                threads.offer(Thread.currentThread());
                LockSupport.park(this);
                continue;
            }
            return;
        }
    }

    public void releaseReadLock() {
        readerCount.decrementAndGet();
    }

    public void acquireWriteLock() {
        while (true) {
            if (writer.compareAndSet(false, true)) {
                while (readerCount.get() > 0);
                return;
            }
        }
    }

    public void releaseWriteLock() {
        writer.set(false);
        Thread reader;
        do {
            reader = threads.poll();
            LockSupport.unpark(reader);
        } while (reader != null);
    }
}
