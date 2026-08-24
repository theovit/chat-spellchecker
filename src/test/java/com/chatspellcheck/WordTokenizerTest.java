package com.chatspellcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class WordTokenizerTest
{
	@Test
	public void emptyAndNullReturnNoTokens()
	{
		assertTrue(WordTokenizer.tokenize("").isEmpty());
		assertTrue(WordTokenizer.tokenize(null).isEmpty());
	}

	@Test
	public void splitsOnPunctuationAndDigits()
	{
		List<WordToken> tokens = WordTokenizer.tokenize("hello, wrold! 123 test");

		assertEquals(3, tokens.size());
		assertEquals("hello", tokens.get(0).getWord());
		assertEquals("wrold", tokens.get(1).getWord());
		assertEquals("test", tokens.get(2).getWord());
	}

	@Test
	public void offsetsPointBackIntoOriginalString()
	{
		String text = "a bc def";
		List<WordToken> tokens = WordTokenizer.tokenize(text);

		for (WordToken token : tokens)
		{
			assertEquals(token.getWord(), text.substring(token.getStartOffset(), token.getEndOffset()));
		}
	}

	@Test
	public void contractionsStayOneToken()
	{
		List<WordToken> tokens = WordTokenizer.tokenize("don't stop");

		assertEquals(2, tokens.size());
		assertEquals("don't", tokens.get(0).getWord());
	}

	@Test
	public void leadingAndTrailingApostrophesAreTrimmed()
	{
		List<WordToken> tokens = WordTokenizer.tokenize("'word'");

		assertEquals(1, tokens.size());
		assertEquals("word", tokens.get(0).getWord());
	}
}
