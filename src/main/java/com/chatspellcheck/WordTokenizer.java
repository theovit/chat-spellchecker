package com.chatspellcheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits chatbox input text into words with character offsets. A "word" is a maximal run of
 * letters/apostrophes (so contractions like "don't" stay one token); digits and punctuation are
 * treated as separators and never flagged.
 */
final class WordTokenizer
{
	private WordTokenizer()
	{
	}

	static List<WordToken> tokenize(String text)
	{
		List<WordToken> tokens = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return tokens;
		}

		int start = -1;
		for (int i = 0; i <= text.length(); i++)
		{
			boolean isWordChar = i < text.length() && isWordChar(text.charAt(i));
			if (isWordChar)
			{
				if (start == -1)
				{
					start = i;
				}
			}
			else if (start != -1)
			{
				Trimmed trimmed = trimApostrophes(text, start, i);
				if (trimmed.endWord > trimmed.startWord)
				{
					tokens.add(new WordToken(text.substring(trimmed.startWord, trimmed.endWord), trimmed.startWord, trimmed.endWord));
				}
				start = -1;
			}
		}

		return tokens;
	}

	private static boolean isWordChar(char c)
	{
		return Character.isLetter(c) || c == '\'';
	}

	// Apostrophes at the very start/end of a run (e.g. quoting: 'word') aren't part of the word.
	private static Trimmed trimApostrophes(String text, int start, int end)
	{
		int s = start;
		int e = end;
		while (s < e && text.charAt(s) == '\'')
		{
			s++;
		}
		while (e > s && text.charAt(e - 1) == '\'')
		{
			e--;
		}
		return new Trimmed(s, e);
	}

	private static final class Trimmed
	{
		final int startWord;
		final int endWord;

		Trimmed(int startWord, int endWord)
		{
			this.startWord = startWord;
			this.endWord = endWord;
		}
	}
}
