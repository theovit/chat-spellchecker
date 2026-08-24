package com.chatspellcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Case-insensitive English word lookup plus Norvig-style single/double-edit-distance suggestions.
 * No client dependency: {@link #load()} is plain Java I/O so it's safe to call from a background
 * executor, and it fails open (leaves the dictionary "not loaded") on any error rather than
 * throwing, so a corrupt/missing resource can never break the plugin.
 */
@Slf4j
@Singleton
class SpellcheckDictionary
{
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
	private static final String[] PLAIN_RESOURCES = {
		"/com/chatspellcheck/dictionary-enable1.txt",
		"/com/chatspellcheck/osrs-terms.txt",
		"/com/chatspellcheck/contractions-en.txt"
	};
	private static final String FREQUENCY_RESOURCE = "/com/chatspellcheck/word-frequency-en.txt";

	private volatile Set<String> words = Collections.emptySet();
	// Lower rank = more common. Multiple equally-valid edit-distance candidates are ranked by
	// this so "world" wins over "wold" for "wrld" instead of an arbitrary HashSet order.
	private volatile Map<String, Integer> wordRank = Collections.emptyMap();
	private volatile boolean loaded = false;

	void load()
	{
		Set<String> loadedWords = new HashSet<>(220_000);
		Map<String, Integer> loadedRank = new HashMap<>(50_000);

		for (String resource : PLAIN_RESOURCES)
		{
			if (!loadResource(resource, loadedWords))
			{
				log.warn("Chat Spellcheck: failed to load dictionary resource {}, spellcheck disabled for this session", resource);
				return;
			}
		}

		if (!loadFrequencyResource(FREQUENCY_RESOURCE, loadedWords, loadedRank))
		{
			log.warn("Chat Spellcheck: failed to load dictionary resource {}, spellcheck disabled for this session", FREQUENCY_RESOURCE);
			return;
		}

		words = loadedWords;
		wordRank = loadedRank;
		loaded = true;
		log.debug("Chat Spellcheck: loaded {} words ({} ranked)", loadedWords.size(), loadedRank.size());
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

	// Frequency-list entries are also union-ed into the correctness set: it's real English
	// vocabulary, and ENABLE1 (a word-game list) excludes short/common words like "a" and "i"
	// that would otherwise be incorrectly flagged as typos.
	private boolean loadFrequencyResource(String resource, Set<String> wordsOut, Map<String, Integer> rankOut)
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
				int rank = 0;
				while ((line = reader.readLine()) != null)
				{
					String word = line.trim().toLowerCase();
					if (!word.isEmpty())
					{
						wordsOut.add(word);
						rankOut.putIfAbsent(word, rank);
						rank++;
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

	/** Returns the most common valid correction within edit distance 2, or empty if none found. */
	Optional<String> suggest(String word)
	{
		String lower = word.toLowerCase();

		Set<String> edits1 = editsOne(lower);
		Optional<String> best = bestCandidate(edits1);
		if (best.isPresent())
		{
			return best;
		}

		Set<String> edits2 = new HashSet<>();
		for (String e1 : edits1)
		{
			edits2.addAll(editsOne(e1));
		}
		return bestCandidate(edits2);
	}

	private Optional<String> bestCandidate(Set<String> candidates)
	{
		String best = null;
		int bestRank = Integer.MAX_VALUE;

		for (String candidate : candidates)
		{
			if (!isCorrect(candidate))
			{
				continue;
			}

			Integer rank = wordRank.get(candidate);
			if (rank != null)
			{
				if (rank < bestRank)
				{
					bestRank = rank;
					best = candidate;
				}
			}
			else if (best == null)
			{
				best = candidate;
			}
		}

		return Optional.ofNullable(best);
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
