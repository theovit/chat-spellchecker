package com.chatspellcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class ChatInputTrackerTest
{
	private ChatInputTracker tracker;

	@Before
	public void setUp()
	{
		SpellcheckDictionary dictionary = new SpellcheckDictionary();
		dictionary.load();
		// contains() never touches configManager, so null is safe here as long as load()/add() aren't called.
		IgnoreListStore ignoreListStore = new IgnoreListStore(null);
		tracker = new ChatInputTracker(null, dictionary, ignoreListStore);
	}

	@Test
	public void suggestionPinsWhileAnyCharacterRemainsThenClearsOnceEmpty()
	{
		tracker.recompute("wrld");
		assertEquals("world", tracker.getCurrentWordSuggestion());

		// Backspacing the misspelled word: the pin should hold as long as at least one character
		// of it remains, regardless of what the shrinking fragment would suggest on its own.
		tracker.recompute("wrl");
		assertEquals("world", tracker.getCurrentWordSuggestion());
		tracker.recompute("wr");
		assertEquals("world", tracker.getCurrentWordSuggestion());
		tracker.recompute("w");
		assertEquals("world", tracker.getCurrentWordSuggestion());

		// The instant it's fully gone, the suggestion drops immediately - it must not linger with
		// no word left to anchor to.
		tracker.recompute("");
		assertNull(tracker.getCurrentWordSuggestion());
	}

	@Test
	public void suggestionClearsWhenRetypedTextDivergesBeforeWordIsEmptied()
	{
		tracker.recompute("wrld");
		assertEquals("world", tracker.getCurrentWordSuggestion());

		tracker.recompute("wrl");
		assertEquals("world", tracker.getCurrentWordSuggestion());

		// Typing something inconsistent with the pinned target (without emptying the word first)
		// clears the pin and resumes live suggestions.
		tracker.recompute("wrlx");
		assertNotEquals("world", tracker.getCurrentWordSuggestion());
	}

	@Test
	public void suggestionCompletesCorrectlyClearsThePin()
	{
		tracker.recompute("wrld");
		assertEquals("world", tracker.getCurrentWordSuggestion());

		tracker.recompute("wr");
		assertEquals("world", tracker.getCurrentWordSuggestion());

		// Typing the rest of the pinned word out completes it - no longer flagged, no suggestion.
		tracker.recompute("wor");
		assertEquals("world", tracker.getCurrentWordSuggestion());
		tracker.recompute("worl");
		assertEquals("world", tracker.getCurrentWordSuggestion());
		tracker.recompute("world");
		assertNull(tracker.getCurrentWordSuggestion());
	}

	@Test
	public void matchedLengthTracksHowMuchOfTheSuggestionHasBeenTypedSoFar()
	{
		tracker.recompute("wrld");
		// "wrld" isn't itself a prefix of "world" - nothing to highlight as "typed" yet.
		assertEquals(0, tracker.getCurrentWordSuggestionMatchedLength());

		tracker.recompute("wr");
		assertEquals("world", tracker.getCurrentWordSuggestion());
		assertEquals(0, tracker.getCurrentWordSuggestionMatchedLength());

		tracker.recompute("wor");
		assertEquals("world", tracker.getCurrentWordSuggestion());
		assertEquals(3, tracker.getCurrentWordSuggestionMatchedLength());

		tracker.recompute("worl");
		assertEquals(4, tracker.getCurrentWordSuggestionMatchedLength());
	}
}
