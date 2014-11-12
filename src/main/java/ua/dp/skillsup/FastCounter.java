package ua.dp.skillsup;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FastCounter implements Counter {

    private volatile AtomicLong[] counters;

    private AtomicLong singleCounter = new AtomicLong();

    private AtomicBoolean isExpanding = new AtomicBoolean(false);

    private static AtomicInteger nextHashCode = new AtomicInteger();
    private static final int HASH_INCREMENT = 0x61c88647;

    private final ThreadLocal<Integer> threadLocalHash = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return nextHashCode();
        }
    };
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    public FastCounter(int capacity) {
        int adjustedCapacity = findNextPositivePowerOfTwo(capacity);
        counters = new AtomicLong[adjustedCapacity];
        for (int index = 0; index < adjustedCapacity; index++) {
            counters[index] = new AtomicLong();
        }
    }

	@Override
	public void inc() {
        while (true) {
            AtomicLong[] localCounters = counters;
            int index = threadLocalHash.get() & (localCounters.length - 1);
            AtomicLong counter = localCounters[index];

            for (int tryIndex = 0; tryIndex < 4; tryIndex++) {
                long current = counter.get();
                long next = current + 1;
                if (counter.compareAndSet(current, next)) {
                    return;
                }
            }
            if (isExpanding.compareAndSet(false, true)) {
                AtomicLong[] newCounters = Arrays.copyOf(localCounters, localCounters.length * 2);
                for (int newIndex = 0; newIndex < newCounters.length; newIndex++) {
                    if (newCounters[newIndex] == null) {
                        newCounters[newIndex] = new AtomicLong();
                    }
                }
                counters = newCounters;
                isExpanding.set(false);
            }
            if (isExpanding.compareAndSet(false, true)) {
                singleCounter.incrementAndGet();
            }
        }

	}

	@Override
	public long get() {
        long sum = 0;
		for (AtomicLong counter : counters) {
            sum += counter.get();
        }
        return sum + singleCounter.get();
	}

    private static int findNextPositivePowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}