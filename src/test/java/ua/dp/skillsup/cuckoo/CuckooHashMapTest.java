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
}
