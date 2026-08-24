package com.chatspellcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Case-insensitive English word lookup plus Norvig-style single/double-edit-distance suggestions.
 * No client dependency: {@link #load()} is plain Java I/O so it's safe to call from a background
 * executor, and it fails open (leaves the dictionary "not loaded") on any error rather than
 * throwing, so a corrupt/missing resource can never break the plugin.
 */
@Slf4j
class SpellcheckDictionary
{
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
	private static final String[] RESOURCES = {
		"/com/chatspellcheck/dictionary-enable1.txt",
		"/com/chatspellcheck/osrs-terms.txt"
	};

	private volatile Set<String> words = Collections.emptySet();
	private volatile boolean loaded = false;

	void load()
	{
		Set<String> loadedWords = new HashSet<>(180_000);
		for (String resource : RESOURCES)
		{
			if (!loadResource(resource, loadedWords))
			{
				log.warn("Chat Spellcheck: failed to load dictionary resource {}, spellcheck disabled for this session", resource);
				return;
			}
		}

		words = loadedWords;
		loaded = true;
		log.debug("Chat Spellcheck: loaded {} words", loadedWords.size());
	}

	private boolean loadResource(String resource, Set<String> into)
	{
		try (InputStream is = SpellcheckDictionary.class.getResourceAsStream(resource))
		{
			if (is == null)
			{
				return false;
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					String word = line.trim().toLowerCase();
					if (!word.isEmpty())
					{
						into.add(word);
					}
				}
			}
			return true;
		}
		catch (IOException e)
		{
			log.warn("Chat Spellcheck: error reading dictionary resource {}", resource, e);
			return false;
		}
	}

	boolean isLoaded()
	{
		return loaded;
	}

	boolean isCorrect(String word)
	{
		return words.contains(word.toLowerCase());
	}

	/** Returns the top suggestion for a misspelled word, or empty if none was found within edit distance 2. */
	Optional<String> suggest(String word)
	{
		String lower = word.toLowerCase();

		Set<String> edits1 = editsOne(lower);
		for (String candidate : edits1)
		{
			if (isCorrect(candidate))
			{
				return Optional.of(candidate);
			}
		}

		for (String e1 : edits1)
		{
			for (String e2 : editsOne(e1))
			{
				if (isCorrect(e2))
				{
					return Optional.of(e2);
				}
			}
		}

		return Optional.empty();
	}

	private static Set<String> editsOne(String word)
	{
		Set<String> result = new HashSet<>();
		for (int i = 0; i <= word.length(); i++)
		{
			String left = word.substring(0, i);
			String right = word.substring(i);

			if (!right.isEmpty())
			{
				// delete
				result.add(left + right.substring(1));
			}

			if (right.length() > 1)
			{
				// transpose
				result.add(left + right.charAt(1) + right.charAt(0) + right.substring(2));
			}

			if (!right.isEmpty())
			{
				// replace
				for (int c = 0; c < ALPHABET.length(); c++)
				{
					result.add(left + ALPHABET.charAt(c) + right.substring(1));
				}
			}

			// insert
			for (int c = 0; c < ALPHABET.length(); c++)
			{
				result.add(left + ALPHABET.charAt(c) + right);
			}
		}
		return result;
	}
}
