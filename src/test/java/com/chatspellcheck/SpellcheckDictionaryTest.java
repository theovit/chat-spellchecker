package com.chatspellcheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SpellcheckDictionaryTest
{
	private SpellcheckDictionary dictionary;

	@Before
	public void setUp()
	{
		dictionary = new SpellcheckDictionary();
	}

	@Test
	public void notLoadedUntilLoadIsCalled()
	{
		assertFalse(dictionary.isLoaded());
		dictionary.load();
		assertTrue(dictionary.isLoaded());
	}

	@Test
	public void knownWordsAreCorrectCaseInsensitively()
	{
		dictionary.load();

		assertTrue(dictionary.isCorrect("hello"));
		assertTrue(dictionary.isCorrect("Hello"));
		assertTrue(dictionary.isCorrect("WORLD"));
	}

	@Test
	public void unknownWordsAreNotCorrect()
	{
		dictionary.load();

		assertFalse(dictionary.isCorrect("wrold"));
		assertFalse(dictionary.isCorrect("zzzznotaword"));
	}

	@Test
	public void osrsTermsAreLoaded()
	{
		dictionary.load();

		assertTrue(dictionary.isCorrect("osrs"));
		assertTrue(dictionary.isCorrect("wilderness"));
	}

	@Test
	public void suggestsAValidDictionaryWordForACommonMisspelling()
	{
		dictionary.load();

		String suggestion = dictionary.suggest("recieve").orElse(null);

		assertTrue("expected a suggestion", suggestion != null);
		assertTrue("suggestion must itself be a correct word", dictionary.isCorrect(suggestion));
	}

	@Test
	public void noSuggestionBeyondEditDistanceTwo()
	{
		dictionary.load();

		assertFalse(dictionary.suggest("zzzznotawordatall").isPresent());
	}
}
