package ua.dp.skillsup;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FastCounter implements Counter {

	public static final int TRY_COUNT = 8;
	private volatile AtomicLong[] counters;

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
		add(1);
	}

    @Override
    public void dec() {
        add(-1);
    }

    private void add(long inc) {
        while (true) {
            AtomicLong[] localCounters = counters;
            int index = threadLocalHash.get() & (localCounters.length - 1);
            AtomicLong counter = localCounters[index];
            if (counter == null) {
                if(!isExpanding.get() && isExpanding.compareAndSet(false, true) && localCounters == counters) {
                    counters[index] = new AtomicLong(inc);
                    return;
                }
                continue;
            }

            for (int tryIndex = 0; tryIndex < TRY_COUNT; tryIndex++) {
                long current = counter.get();
                long next = current + inc;
                if (counter.compareAndSet(current, next)) {
                    return;
                }
            }
            if (!isExpanding.get() && isExpanding.compareAndSet(false, true)) {
                if (counters == localCounters) {
                    AtomicLong[] newCounters = Arrays.copyOf(localCounters, localCounters.length * 2);
                    for (int newIndex = localCounters.length; newIndex < newCounters.length; newIndex++) {
                        newCounters[newIndex] = new AtomicLong();
                    }
                    counters = newCounters;
                    isExpanding.lazySet(false);
                }
            }
        }
    }

	@Override
	public long get() {
		long sum = 0;
		for (AtomicLong counter : counters) {
            if (counter != null) {
                sum += counter.get();
            }
		}
		return sum;
	}

	private static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}
}