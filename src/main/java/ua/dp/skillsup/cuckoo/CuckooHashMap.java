package ua.dp.skillsup.cuckoo;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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

    }

    public V remove(Object key) {

    }

    public void clear() {
    }


    private void find(Object key) {
        int result = 0;

        int h1 = indexOf(hash1(key.hashCode()));
        int h2 = indexOf(hash2(key.hashCode()));

        while (true) {
            EntryCounter<K, V> e1r1 = entries[0][h1];
            if (e1r1 != null) {
                if (e1r1.entry.marked) {
                    //TODO: insert help_relocate
                     continue;
                }
                if (e1r1.entry.key.equals(key)) {
                    result = 1;
                }
            }

            EntryCounter<K, V> e2r1 = entries[1][h2];
            if (e2r1 != null) {
                if (e2r1.entry.marked) {
                    //TODO: insert help_relocate
                    continue;
                }
                if (e2r1.entry.key.equals(key)) {
                    if (result == 1) {
                        //insert remove dublicate
                    } else {
                        result = 2;
                    }
                }
            }

            if (result == 1 || result == 2) {
                return;
            }

            EntryCounter<K, V> e1r2 = entries[0][h1];
            if (e1r2 != null) {
                if (e1r2.entry.marked) {
                    //TODO: insert help_relocate
                    continue;
                }
                if (e1r2.entry.key.equals(key)) {
                    result = 1;
                }
            }

            EntryCounter<K, V> e2r2 = entries[1][h2];
            if (e2r2 != null) {
                if (e2r2.entry.marked) {
                    //TODO: insert help_relocate
                    continue;
                }
                if (e2r2.entry.key.equals(key)) {
                    if (result == 1) {
                        //insert remove dublicate
                    } else {
                        result = 2;
                    }
                }
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
        final MyEntry<K, V> entry;
        final long counter;

        EntryCounter(MyEntry<K, V> entry) {
            this.entry = entry;
            counter = 0;
        }

        EntryCounter(MyEntry<K, V> entry, long value) {
            this.entry = entry;
            this.counter = value;
        }
    }

    public static class MyEntry<K, V> implements Entry<K, V> {
        final K key;
        V value;
        boolean marked;

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
            this.value = value;
            return value;
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
