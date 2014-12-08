package ua.dp.skillsup.readlock;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class OReadersWriterSpinLock {

    //ToDo re-entrancy

    AtomicLong readerCount = new AtomicLong();
    //FastCounter readerCount = new FastCounter();

    CLHQueueLock writerLock = new CLHQueueLock();

    public void acquireReadLock() {
        while (true) {
            readerCount.incrementAndGet();

            if (!writerLock.isLocked()) {
                return;
            }

            readerCount.decrementAndGet();

            CLHQueueLock.Qnode lastTail = writerLock.getTail().get();
            if (lastTail.locked && lastTail == writerLock.getTail().get()) {
                lastTail.parkedReaders.offer(Thread.currentThread());
                LockSupport.park(this);
            }
        }
    }

    public void releaseReadLock() {
        readerCount.decrementAndGet();
    }

    public void acquireWriteLock() {
        writerLock.lock();
        while (readerCount.get() > 0);
    }

    public void releaseWriteLock() {
        writerLock.unlock();
    }
}
