package ua.dp.skillsup.cuckoo;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CuckooHashMap<K, V> extends AbstractMap<K, V> {

	private static final int INITIAL_CAPACITY = 16;
	private AtomicReferenceArray<EntryCounter>[] entries;
	private final int length;

	public CuckooHashMap() {
        this(INITIAL_CAPACITY);
	}

    public CuckooHashMap(int capacity) {
        length = findNextPositivePowerOfTwo(capacity);
        entries = new AtomicReferenceArray[] {
                new AtomicReferenceArray<EntryCounter>(length),
                new AtomicReferenceArray<EntryCounter>(length)
        };
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public int size() {
		return entrySet().size();
	}

	public boolean containsKey(Object key) {
		int h = key.hashCode();
		int h1 = indexOf(hash1(h));
		int h2 = indexOf(hash2(h));

		while (true) {
			EntryCounter<K, V> e1r1 = entries[0].get(h1);
			if (e1r1 != null && h == e1r1.entry.hash && e1r1.entry.key.equals(key)) {
				return true;
			}

			EntryCounter<K, V> e2r1 = entries[1].get(h2);
			if (e2r1 != null && h == e2r1.entry.hash && e2r1.entry.key.equals(key)) {
				return true;
			}
			//Second try
			EntryCounter<K, V> e1r2 = entries[0].get(h1);
			if (e1r2 != null && h == e1r2.entry.hash && e1r2.entry.key.equals(key)) {
				return true;
			}

			EntryCounter<K, V> e2r2 = entries[1].get(h2);
			if (e2r2 != null && h == e2r2.entry.hash && e2r2.entry.key.equals(key)) {
				return true;
			}

			if (!checkCounters(
					e1r1 == null ? 0 : e1r1.counter,
					e2r1 == null ? 0 : e2r1.counter,
					e1r2 == null ? 0 : e1r2.counter,
					e2r2 == null ? 0 : e2r2.counter)) {
				return false;
			}
		}
	}

	public V get(Object key) {
        int h = key.hashCode();
        int h1 = indexOf(hash1(h));
		int h2 = indexOf(hash2(h));

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
        int h = key.hashCode();
        int h1 = indexOf(hash1(h));
        int h2 = indexOf(hash2(h));

		while (true) {
			FindResult<K, V> findResult = find(key);
            if (findResult.exists) {
                //If find return true, then either first not null or the second
                //Entry counter is immutable, so we can ignore checking entry is not null
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

			if (relocate(0, h)) continue;
			throw new IllegalStateException("Rehashing is needed to proceed");
		}
	}

	public V remove(Object key) {
        int h = key.hashCode();
        int h1 = indexOf(hash1(h));
        int h2 = indexOf(hash2(h));
        while (true) {
            FindResult<K, V> findResult = find(key);
            if (!findResult.exists) return null;
            if (findResult.first != null) {
                if (entries[0].compareAndSet(h1, findResult.first, new EntryCounter(null, findResult.first.counter)))
                    return findResult.first.entry.getValue();
            } else {
                if (entries[0].get(h1) != findResult.first) continue;
                if (entries[1].compareAndSet(h2, findResult.second, new EntryCounter(null, findResult.second.counter)))
                    return findResult.second.entry.getValue();
            }
        }
	}

	public void clear() {
	}

	static class FindResult<K, V> {
		boolean exists;
		EntryCounter<K, V> first;
		EntryCounter<K, V> second;

		FindResult(boolean exists, EntryCounter<K, V> first, EntryCounter<K, V> second) {
			this.exists = exists;
			this.first = first;
			this.second = second;
		}
	}

	private FindResult<K, V> find(Object key) {
		int h = key.hashCode();
		int h1 = indexOf(hash1(h));
		int h2 = indexOf(hash2(h));
		boolean exists;

		while (true) {
			exists = false;
			//TODO: CHECK time between getting counter and getting entry
			EntryCounter<K, V> e1r1 = entries[0].get(h1);
			if (e1r1 != null && e1r1.entry != null) {
				if (e1r1.entry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				exists = h == e1r1.entry.hash && e1r1.entry.key.equals(key);
			}

			EntryCounter<K, V> e2r1 = entries[1].get(h2);
			if (e2r1 != null && e2r1.entry != null) {
				if (e2r1.entry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				if (h == e2r1.entry.hash && e2r1.entry.key.equals(key)) {
					if (exists) {
						//insert remove dublicate
					} else {
						exists = true;
					}
				}
			}

			if (exists) return new FindResult<K, V>(true, e1r1, e2r1);

			EntryCounter<K, V> e1r2 = entries[0].get(h1);
			if (e1r2 != null && e1r2.entry != null) {
				if (e1r2.entry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				exists = h == e1r2.entry.hash && e1r2.entry.key.equals(key);
			}

			EntryCounter<K, V> e2r2 = entries[1].get(h2);
			if (e2r2 != null && e2r2.entry != null) {
				if (e2r2.entry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				if (h == e2r2.entry.hash && e2r2.entry.key.equals(key)) {
					if (exists) {
						//insert remove dublicate
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
			return new FindResult<K, V>(false, e1r2, e1r2);
		}
	}


	private int hash1(int h) {
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	private int hash2(int h) {
		h ^= (h << 13);
		h ^= (h >>> 17);
		h ^= (h << 5);
		return h;
	}

	private int indexOf(int hash) {
		return hash & (length - 1);
	}

	private boolean checkCounters(long table1round1, long table2round1, long table1round2, long table2round2){
		return (table1round2 - table1round1) > 1
				&& (table2round2 - table2round1) > 1
				&& (table2round2 - table1round1) > 2;
	}

	public static class MyEntry<K, V> implements Entry<K, V> {
		final K key;
		volatile V value;
        final int hash;

        boolean marked;

		public MyEntry(K key, V value, int hash) {
			this.key = key;
			this.value = value;
			this.hash = hash;
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

	private static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}
}
