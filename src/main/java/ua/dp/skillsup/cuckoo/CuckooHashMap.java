package ua.dp.skillsup.cuckoo;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CuckooHashMap<K, V> extends AbstractMap<K, V> {

	public static final int INITIAL_CAPACITY = 16;
	private EntryCounter[][] entries;
	private int length;

	public CuckooHashMap() {
		entries = new EntryCounter[2][INITIAL_CAPACITY];
		length = INITIAL_CAPACITY;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public int size() {
		return entrySet().size();
	}

	public boolean containsKey(Object key) {
		int h1 = indexOf(hash1(key.hashCode()));
		int h2 = indexOf(hash2(key.hashCode()));

		while (true) {
			EntryCounter<K, V> e1r1 = entries[0][h1];
			if (e1r1 != null && e1r1.entry.key.equals(key)) {
				return true;
			}

			EntryCounter<K, V> e2r1 = entries[1][h2];
			if (e2r1 != null && e2r1.entry.key.equals(key)) {
				return true;
			}

			EntryCounter<K, V> e1r2 = entries[0][h1];
			if (e1r2 != null && e1r2.entry.key.equals(key)) {
				return true;
			}

			EntryCounter<K, V> e2r2 = entries[1][h2];
			if (e2r2 != null && e2r2.entry.key.equals(key)) {
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
		int h1 = indexOf(hash1(key.hashCode()));
		int h2 = indexOf(hash2(key.hashCode()));

		while (true) {
			EntryCounter<K, V> e1r1 = entries[0][h1];
			if (e1r1 != null && e1r1.entry.key.equals(key)) {
				return e1r1.entry.value;
			}

			EntryCounter<K, V> e2r1 = entries[1][h2];
			if (e2r1 != null && e2r1.entry.key.equals(key)) {
				return e2r1.entry.value;
			}

			EntryCounter<K, V> e1r2 = entries[0][h1];
			if (e1r2 != null && e1r2.entry.key.equals(key)) {
				return e1r2.entry.value;
			}

			EntryCounter<K, V> e2r2 = entries[1][h2];
			if (e2r2 != null && e2r2.entry.key.equals(key)) {
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
		while (true) {
			FindResult<K, V> findResult = find(key);
			if (findResult.exists && findResult.first != null) {
				EntryCounter<K, V> first = findResult.first;
				first.counter.incrementAndGet();

				return first.entry.get().setValue(value);
			}
			if (findResult.exists && findResult.second != null) {
				EntryCounter<K, V> second = findResult.second;
				second.counter.incrementAndGet();

				return second.entry.get().setValue(value);
			}

			if ()

			if (relocate(0, h1)) continue;
			throw new IllegalStateException("Rehashing is needed to proceed");
		}
	}

	public V remove(Object key) {

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
			EntryCounter<K, V> e1r1 = entries[0][h1];
			long c1r1 = 0;
			if (e1r1 != null) {
				MyEntry<K, V> myEntry = e1r1.entry.get();
				c1r1 = e1r1.counter.get();
				if (myEntry.marked) {
					//TODO: insert help_relocate
					continue;
				}

				exists = h == myEntry.hash && myEntry.key.equals(key);
			}

			EntryCounter<K, V> e2r1 = entries[1][h2];
			long c2r1 = 0;
			if (e2r1 != null) {
				MyEntry<K, V> myEntry = e2r1.entry.get();
				c2r1 = e2r1.counter.get();
				if (myEntry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				if (h == myEntry.hash && myEntry.key.equals(key)) {
					if (exists) {
						//insert remove dublicate
					} else {
						exists = true;
					}
				}
			}

			if (exists) {
				return new FindResult<K, V>(true, e1r1, e2r1);
			}

			EntryCounter<K, V> e1r2 = entries[0][h1];
			long c1r2 = 0;
			if (e1r2 != null) {
				MyEntry<K, V> myEntry = e1r2.entry.get();
				c2r1 = e1r2.counter.get();
				if (myEntry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				exists = h == myEntry.hash && myEntry.key.equals(key);
			}

			EntryCounter<K, V> e2r2 = entries[1][h2];
			long c2r2 = 0;
			if (e2r2 != null) {
				MyEntry<K, V> myEntry = e2r2.entry.get();
				c2r2 = e2r2.counter.get();
				if (myEntry.marked) {
					//TODO: insert help_relocate
					continue;
				}
				if (h == myEntry.hash && myEntry.key.equals(key)) {
					if (exists) {
						//insert remove dublicate
					} else {
						exists = true;
					}
				}
			}

			if (exists) return new FindResult<K, V>(true, e1r2, e2r2);

			if (checkCounters(c1r1, c2r1, c1r2, c2r2)) continue;

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

	static class EntryCounter<K, V> {

		final AtomicReference<MyEntry<K, V>> entry;
		final AtomicLong counter = new AtomicLong();

		EntryCounter(MyEntry<K, V> entry) {
			this.entry = new AtomicReference<MyEntry<K, V>>(entry);
		}
	}

	public static class MyEntry<K, V> implements Entry<K, V> {
		final K key;
		volatile V value;

		boolean marked;
		int hash;

		public MyEntry(K key, V value) {
			this.key = key;
			this.value = value;
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
}
