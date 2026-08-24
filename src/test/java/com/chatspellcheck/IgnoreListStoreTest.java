package com.chatspellcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

public class IgnoreListStoreTest
{
	@Test
	public void deserializeOfNullOrBlankIsEmpty()
	{
		assertTrue(IgnoreListStore.deserialize(null).isEmpty());
		assertTrue(IgnoreListStore.deserialize("").isEmpty());
		assertTrue(IgnoreListStore.deserialize("   ").isEmpty());
	}

	@Test
	public void deserializeSplitsTrimsAndLowercases()
	{
		Set<String> words = IgnoreListStore.deserialize("Osrs, gwd ,ironman");

		assertEquals(3, words.size());
		assertTrue(words.contains("osrs"));
		assertTrue(words.contains("gwd"));
		assertTrue(words.contains("ironman"));
	}

	@Test
	public void serializeRoundTripsWithDeserialize()
	{
		Set<String> original = new LinkedHashSet<>();
		original.add("osrs");
		original.add("gwd");

		Set<String> roundTripped = IgnoreListStore.deserialize(IgnoreListStore.serialize(original));

		assertEquals(original, roundTripped);
	}
}
