package ua.dp.skillsup;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class OReadersWriterSpinLock {

    AtomicLong readerCount = new AtomicLong();
    AtomicLong writerCount = new AtomicLong();

    AtomicReference<Thread> exclusiveThread = new AtomicReference<Thread>();

    public void acquireReadLock() {
//        Thread currentThread = Thread.currentThread();
        while (writerCount.get() > 0/* && currentThread != exclusiveThread.get()*/);

        readerCount.incrementAndGet();
    }

    public void releaseReadLock() {
        readerCount.decrementAndGet();
    }

    public void acquireWriteLock() {
        Thread currentThread = Thread.currentThread();
        while (writerCount.get() > 0  && currentThread != exclusiveThread.get());

        writerCount.incrementAndGet();
        exclusiveThread.set(currentThread);

        while (readerCount.get() > 0);
    }

    public void releaseWriteLock() {
        writerCount.decrementAndGet();
        exclusiveThread.set(null);
    }
}
