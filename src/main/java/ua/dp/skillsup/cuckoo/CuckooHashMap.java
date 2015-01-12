package ua.dp.skillsup.cuckoo;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CuckooHashMap<K, V> extends AbstractMap<K, V> {

	private static final int INITIAL_CAPACITY = 16;
	private static final double INITIAL_LOAD_FACTOR = 0.75;

	private volatile AtomicReferenceArray<EntryCounter>[] entries;
	private final int capacity;
	private final int threshold;

	public CuckooHashMap() {
		this(INITIAL_CAPACITY, INITIAL_LOAD_FACTOR);
	}

	public CuckooHashMap(int capacity, double loadFactor) {
		this.capacity = findNextPositivePowerOfTwo(capacity);
		entries = new AtomicReferenceArray[] {
				new AtomicReferenceArray<EntryCounter>(this.capacity),
				new AtomicReferenceArray<EntryCounter>(this.capacity)
		};
		threshold = (int) (loadFactor * capacity);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	/**
	 * Size is non locking, so it is very approximate value
	 * @return approximate size
	 */
	public int size() {
		int size = 0;
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < capacity; j++) {
				EntryCounter counter = entries[i].get(j);
				if (counter != null && counter.entry != null) {
					size++;
				}
			}
		}
		return size;
	}

	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	public V get(Object key) {
		int h = key.hashCode();
		int h1 = hash1(h);
		int h2 = hash2(h);

		while (true) {
			EntryCounter<K, V> e1r1 = entries[0].get(h1);
			if (e1r1 != null && h == e1r1.entry.hash && e1r1.entry.key.equals(key)) {
				return e1r1.entry.value;
			}

			EntryCounter<K, V> e2r1 = entries[1].get(h2);
			if (e2r1 != null && h == e2r1.entry.hash && e2r1.entry.key.equals(key)) {
				return e2r1.entry.value;
			}

			//Second try
			EntryCounter<K, V> e1r2 = entries[0].get(h1);
			if (e1r2 != null && h == e1r2.entry.hash && e1r2.entry.key.equals(key)) {
				return e1r2.entry.value;
			}

			EntryCounter<K, V> e2r2 = entries[1].get(h2);
			if (e2r2 != null && h == e2r2.entry.hash && e2r2.entry.key.equals(key)) {
				return e2r2.entry.value;
			}

			if (!checkCounters(
					e1r1 == null ? 0 : e1r1.counter,
					e2r1 == null ? 0 : e2r1.counter,
					e1r2 == null ? 0 : e1r2.counter,
					e2r2 == null ? 0 : e2r2.counter)) {
				return null;
			}
		}
	}

	public V put(K key, V value) {
		if (value == null) throw new NullPointerException();

		int h = key.hashCode();
		int h1 = hash1(h);
		int h2 = hash2(h);

		while (true) {
			FindResult<K, V> findResult = find(key);
			if (findResult.exists) {
				//If find return true, then either first not null or the second
				return findResult.first != null
						? findResult.first.entry.setValue(value)
						: findResult.second.entry.setValue(value);
			}

			if (findResult.first == null) {
				EntryCounter<K, V> counter = new EntryCounter<K, V>(new MyEntry<K, V>(key, value, h), 0);
				if (entries[0].compareAndSet(h1, null, counter)) return null;
				continue;
			} else if (findResult.first.entry == null) {
				EntryCounter<K, V> counter = new EntryCounter<K, V>(new MyEntry<K, V>(key, value, h),
						findResult.first.counter);
				if (entries[0].compareAndSet(h1, findResult.first, counter)) return null;
				continue;
			}

			if (findResult.second == null) {
				EntryCounter<K, V> counter = new EntryCounter<K, V>(new MyEntry<K, V>(key, value, h), 0);
				if (entries[1].compareAndSet(h2, null, counter)) return null;
				continue;
			} else if (findResult.second.entry == null) {
				EntryCounter<K, V> counter = new EntryCounter<K, V>(new MyEntry<K, V>(key, value, h),
						findResult.second.counter);
				if (entries[1].compareAndSet(h2, findResult.second, counter)) return null;
				continue;
			}

			if (relocate(0, h1)) continue;
			throw new IllegalStateException("Rehashing is needed to proceed");
		}
	}

	public V remove(Object key) {
		int h = key.hashCode();
		int h1 = hash1(h);
		int h2 = hash2(h);
		while (true) {
			FindResult<K, V> findResult = find(key);
			if (!findResult.exists) return null;
			if (findResult.first != null) {
				if (entries[0].compareAndSet(h1, findResult.first, new EntryCounter(null, findResult.first.counter)))
					return findResult.first.entry.getValue();
			} else {
				if (entries[0].get(h1) != null) continue;
				if (entries[1].compareAndSet(h2, findResult.second, new EntryCounter(null, findResult.second.counter)))
					return findResult.second.entry.getValue();
			}
		}
	}

	public void clear() {
		entries = new AtomicReferenceArray[] {
				new AtomicReferenceArray<EntryCounter>(this.capacity),
				new AtomicReferenceArray<EntryCounter>(this.capacity)
		};
	}

	private FindResult<K, V> find(Object key) {
		int h = key.hashCode();
		int h1 = hash1(h);
		int h2 = hash2(h);
		boolean exists;

		while (true) {
			exists = false;
			EntryCounter<K, V> e1r1 = entries[0].get(h1);
			if (e1r1 != null && e1r1.entry != null) {
				if (e1r1.entry.marked) {
					helpRelocate(0, h1, false);
					continue;
				}
				exists = h == e1r1.entry.hash && e1r1.entry.key.equals(key);
			}

			EntryCounter<K, V> e2r1 = entries[1].get(h2);
			if (e2r1 != null && e2r1.entry != null) {
				if (e2r1.entry.marked) {
					helpRelocate(1, h2, false);
					continue;
				}
				if (h == e2r1.entry.hash && e2r1.entry.key.equals(key)) {
					if (exists) {
						deleteDuplicate(h1, e1r1.entry, h2, e2r1.entry);
					} else {
						exists = true;
					}
				}
			}

			if (exists) return new FindResult<K, V>(true, e1r1, e2r1);

			EntryCounter<K, V> e1r2 = entries[0].get(h1);
			if (e1r2 != null && e1r2.entry != null) {
				if (e1r2.entry.marked) {
					helpRelocate(0, h1, false);
					continue;
				}
				exists = h == e1r2.entry.hash && e1r2.entry.key.equals(key);
			}

			EntryCounter<K, V> e2r2 = entries[1].get(h2);
			if (e2r2 != null && e2r2.entry != null) {
				if (e2r2.entry.marked) {
					helpRelocate(1, h2, false);
					continue;
				}
				if (h == e2r2.entry.hash && e2r2.entry.key.equals(key)) {
					if (exists) {
						deleteDuplicate(h1, e1r2.entry, h2, e2r2.entry);
					} else {
						exists = true;
					}
				}
			}

			if (exists) return new FindResult<K, V>(true, e1r2, e2r2);
			if (checkCounters(
					e1r1 == null ? 0 : e1r1.counter,
					e2r1 == null ? 0 : e2r1.counter,
					e1r2 == null ? 0 : e1r2.counter,
					e2r2 == null ? 0 : e2r2.counter)) continue;
			return new FindResult<K, V>(false, e1r2, e2r2);
		}
	}

	private boolean relocate(int startTableIndex, int index) {
		int[] route = new int[threshold];
		int startLevel = 0;
		int tableIndex = startTableIndex;
		int idx = index;
		int previousIndex = -1;
		MyEntry previous = null;
		boolean found = false;
		boolean notFinished = true;

		while (notFinished) {
			notFinished = false;
			int depth = startLevel;
			do {
				MyEntry entry = entries[tableIndex].get(idx).entry;
				if (entry == null) {
					found = true;
					break;
				}
				while (entry.marked) {
					helpRelocate(tableIndex, idx, false);
					entry = entries[tableIndex].get(idx).entry;
				}

				if (previous == entry || (previous != null && entry.key.equals(previous.key))) {
					if (tableIndex == 0) {
						deleteDuplicate(idx, entry, previousIndex, previous);
					} else {
						deleteDuplicate(previousIndex, previous, index, entry);
					}
				}

				route[depth] = index;
				previous = entry;
				previousIndex = index;
				tableIndex ^= 1;
				index = tableIndex == 0
						? hash1(entry.hash)
						: hash2(entry.hash);
			} while (++depth < threshold);

			if (found) {
				tableIndex ^= 1;
				for (int i = depth - 1; i >= 0; --i, tableIndex ^= 1) {
					index = route[i];
					MyEntry entry = entries[tableIndex].get(index).entry;
					if (entry.marked) {
						helpRelocate(tableIndex, index, false);
						entry = entries[tableIndex].get(index).entry;
					}
					if (entry == null) {
						continue;
					}
					int destinationIndex = tableIndex == 0
							? hash2(entry.hash)
							: hash1(entry.hash);
					MyEntry destinationEntry = entries[tableIndex ^ 1].get(destinationIndex).entry;
					if (destinationEntry != null) {
						notFinished = true;
						startLevel = i + 1;
						index = destinationIndex;
						tableIndex ^= 1;
						break;
					}
					helpRelocate(tableIndex, index, true);
				}
			}
		}

		return found;
	}

	private void deleteDuplicate(int idx, MyEntry entry, int secondIdx, MyEntry secondEntry) {
		EntryCounter first = entries[0].get(idx);
		EntryCounter second = entries[1].get(secondIdx);
		if (entry != first.entry && secondEntry != second.entry) return;
		if (!entry.key.equals(secondEntry.key)) return;
		entries[1].compareAndSet(secondIdx, second, new EntryCounter(null, second.counter));
	}

	private void helpRelocate(int tableIndex, int index, boolean initiator) {
		while (true) {
			EntryCounter source = entries[tableIndex].get(index);
			if (initiator && source.entry == null) return;
			while (initiator && !source.entry.marked) {
				entries[tableIndex].compareAndSet(index, source,
						new EntryCounter(new MyEntry(source.entry, true), source.counter));
				source = entries[tableIndex].get(index);
				if (source.entry == null) return;
			}
			if (source == null || !source.entry.marked) return;
			int destinationIndex = tableIndex == 0
					? hash2(source.entry.hash)
					: hash1(source.entry.hash);
			EntryCounter destination = entries[tableIndex ^ 1].get(destinationIndex);
			if (destination == null || destination.entry == null) {
				long destinationCounter = destination == null ? 0 : destination.counter;
				long nextCount = source.counter > destinationCounter
						? source.counter + 1
						: destinationCounter + 1;
				if (source.entry != entries[tableIndex].get(index).entry) continue;
				if (entries[tableIndex ^ 1].compareAndSet(destinationIndex, destination, new EntryCounter(source.entry, nextCount))) {
					entries[tableIndex].compareAndSet(index, source, new EntryCounter(null, source.counter + 1));
					return;
				}
			}
			if (destination != null && source.entry == destination.entry) {
				entries[tableIndex].compareAndSet(index, source, new EntryCounter(null, source.counter + 1));
				return;
			}

			entries[tableIndex].compareAndSet(index, source,
					new EntryCounter(new MyEntry(source.entry, false), source.counter + 1));
			return;
		}
	}

	private int hash1(int h) {
		h ^= h;
		h += (h <<  15) ^ 0xffffcd7d;
		h ^= (h >>> 10);
		h += (h <<   3);
		h ^= (h >>>  6);
		h += (h <<   2) + (h << 14);
		h ^= (h >>> 16);
		return h & (capacity - 1);
	}

	private int hash2(int h) {
		h ^= (h << 13);
		h ^= (h >>> 17);
		h ^= (h << 5);
		return h & (capacity - 1);
	}

	private boolean checkCounters(long table1round1, long table2round1, long table1round2, long table2round2){
		return (table1round2 - table1round1) > 1
				&& (table2round2 - table2round1) > 1
				&& (table2round2 - table1round1) > 2;
	}

	private static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}

	private static final class FindResult<K, V> {
		final boolean exists;
		final EntryCounter<K, V> first;
		final EntryCounter<K, V> second;

		FindResult(boolean exists, EntryCounter<K, V> first, EntryCounter<K, V> second) {
			this.exists = exists;
			this.first = first;
			this.second = second;
		}
	}

	private final static class MyEntry<K, V> implements Entry<K, V> {
		final K key;
		final int hash;
		final boolean marked;

		volatile V value;

		public MyEntry(K key, V value, int hash) {
			this.key = key;
			this.value = value;
			this.hash = hash;
			marked = false;
		}

		public MyEntry(MyEntry<K, V> entry, boolean marked) {
			this.key = entry.key;
			this.value = entry. value;
			this.hash = entry.hash;
			this.marked = marked;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			if (value == null) throw new NullPointerException();
			V temp = this.value;
			this.value = value;
			return temp;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			Object k1 = getKey();
			Object k2 = e.getKey();
			if (k1 == k2 || (k1 != null && k1.equals(k2))) {
				Object v1 = getValue();
				Object v2 = e.getValue();
				if (v1 == v2 || (v1 != null && v1.equals(v2)))
					return true;
			}
			return false;
		}

		public final int hashCode() {
			return (key==null   ? 0 : key.hashCode()) ^
					(value==null ? 0 : value.hashCode());
		}

		public final String toString() {
			return getKey() + "=" + getValue();
		}
	}

	private static class EntryCounter<K, V> {
		final MyEntry<K, V> entry;
		final long counter;

		EntryCounter(MyEntry<K, V> entry, long counter) {
			this.entry = entry;
			this.counter = counter;
		}
	}

	/*Implementation was stolen from ConcurrentHashMap :)*/
	final class EntrySet extends AbstractSet<Entry<K,V>> {
		public Iterator<Entry<K,V>> iterator() {
			return new EntryIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?,?> e = (Map.Entry<?,?>)o;
			V v = CuckooHashMap.this.get(e.getKey());
			return v != null && v.equals(e.getValue());
		}
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?,?> e = (Map.Entry<?,?>)o;
			return CuckooHashMap.this.remove(e.getKey()) != null;
		}
		public int size() {
			return CuckooHashMap.this.size();
		}
		public boolean isEmpty() {
			return CuckooHashMap.this.isEmpty();
		}
		public void clear() {
			CuckooHashMap.this.clear();
		}
	}
	final class EntryIterator extends HashIterator implements Iterator<Entry<K,V>> {
		public Map.Entry<K,V> next() {
			MyEntry<K,V> e = super.nextEntry();
			return new WriteThroughEntry(e.key, e.value);
		}
	}

	abstract class HashIterator {
		int tableIndex;
		int index;

		MyEntry<K, V> nextEntry;
		MyEntry<K, V> lastReturned;

		HashIterator() {
			tableIndex = 0;
			index = 0;
			advance();
		}

		final void advance() {
			while (true) {
				if (index >= capacity) {
					if (tableIndex == 1) {
						nextEntry = null;
						return;
					}
					tableIndex = 1;
					index = 0;
					continue;
				}
				EntryCounter entryCounter = entries[tableIndex].get(index++);
				if (entryCounter != null && entryCounter.entry != null) {
					nextEntry = entryCounter.entry;
					return;
				}
			}
		}

		final MyEntry<K,V> nextEntry() {
			MyEntry<K,V> e = nextEntry;
			if (e == null) throw new NoSuchElementException();
			lastReturned = e;
			advance();
			return e;
		}

		public final boolean hasNext() { return nextEntry != null; }
		public final boolean hasMoreElements() { return nextEntry != null; }

		public final void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			CuckooHashMap.this.remove(lastReturned.key);
			lastReturned = null;
		}
	}

	final class WriteThroughEntry extends AbstractMap.SimpleEntry<K,V> {
		WriteThroughEntry(K k, V v) {
			super(k,v);
		}

		public V setValue(V value) {
			if (value == null) throw new NullPointerException();
			V v = super.setValue(value);
			CuckooHashMap.this.put(getKey(), value);
			return v;
		}
	}
}
