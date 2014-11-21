package ua.dp.skillsup;

import java.util.concurrent.atomic.AtomicLong;

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
            writerLock.tryParkReader();
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
