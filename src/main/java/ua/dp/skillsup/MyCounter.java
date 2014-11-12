package ua.dp.skillsup;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MyCounter implements Counter {

	private volatile AtomicLong[] counters;
	private AtomicLong size = new AtomicLong(0);
	private AtomicLong threshold = new AtomicLong(0);
	private AtomicBoolean expanding = new AtomicBoolean(false);

	private static final int HASH_INCREMENT = 0x61c88647;
	private static AtomicInteger nextHashCode = new AtomicInteger();
	private ThreadLocal<Integer> threadHash = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return nextHashCode.getAndAdd(HASH_INCREMENT);
		}
	};

	public MyCounter(int capacity) {
		int adjustedLength = findNextPositivePowerOfTwo(capacity);
		counters = new AtomicLong[adjustedLength];
		setThreshold(adjustedLength);
		for (int i = 0 ; i < adjustedLength; i++) {
			counters[i] = new AtomicLong();
		}
	}

	@Override
	public void inc() {
		AtomicLong[] temp = counters;
		int length = temp.length;
		int index = threadHash.get() & (length - 1);
		AtomicLong counter = temp[index];
		if (counter.getAndIncrement() == 0) {
			long currentSize = size.getAndIncrement();
			if (currentSize >= threshold.get() && expanding.compareAndSet(false, true)) {
				int newLength = 2 * length;
				AtomicLong[] tempCounters = Arrays.copyOf(counters, newLength);
				for (int i = length; i < newLength; i++) {
					tempCounters[i] = new AtomicLong();
				}
				setThreshold(newLength);
				counters = tempCounters;
				expanding.set(false);
			}
		}
	}

	private void setThreshold(int length) {
		threshold.set(length * 2 / 3);
	}

	@Override
	public long get() {
		long sum = 0;
		AtomicLong[] temp = counters;
		for (AtomicLong counter : temp) {
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