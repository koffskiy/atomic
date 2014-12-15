package ua.dp.skillsup.cuckoo;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CuckooHashMapTest {

	@Test
	public void userCanRetrieveValueWhichWasStored() {
		//GIVEN
		Map<String, String> result = new CuckooHashMap<String, String>();
		result.put("key", "value");
		//WHEN
		String value = result.get("key");
		//THEN
		assertEquals("Cannot retrieve value!", "value", value);
	}

	@Test
	public void mapShouldResolveColissions() {
		//GIVEN
		Map<KeyWithCollision, String> result = new CuckooHashMap<KeyWithCollision, String>();
		KeyWithCollision key1 = new KeyWithCollision(1);
		KeyWithCollision key2 = new KeyWithCollision(2);
		result.put(key1, "value1");
		result.put(key2, "value2");
		//WHEN
		String value1 = result.get(key1);
		String value2 = result.get(key2);
		//THEN
		assertEquals("Cannot retrieve value!", "value1", value1);
		assertEquals("Cannot retrieve value!", "value2", value2);
	}

	@Test
	public void a() {
		System.out.println(1 ^ 1);
		System.out.println(0 ^ 1);
	}


	@Test(expected = IllegalStateException.class)
	public void mapShouldResolveThrowException() {
		//GIVEN
		Map<KeyWithCollision, String> result = new CuckooHashMap<KeyWithCollision, String>();
		KeyWithCollision key1 = new KeyWithCollision(1);
		KeyWithCollision key2 = new KeyWithCollision(2);
		KeyWithCollision key3 = new KeyWithCollision(3);

		//WHEN
		result.put(key1, "value1");
		result.put(key2, "value2");
		result.put(key3, "value3");
		//THEN
	}

	static class KeyWithCollision {
		int i;

		KeyWithCollision(int i) {
			this.i = i;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			KeyWithCollision that = (KeyWithCollision) o;
			return i == that.i;
		}

		@Override
		public int hashCode() {
			return 42;
		}
	}
}
