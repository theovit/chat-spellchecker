package com.chatspellcheck;

import lombok.Value;

/**
 * A word in the chatbox input that failed the dictionary/ignore-list check, with its character
 * offsets into the full input string and a precomputed suggestion (if any). Computed once by
 * {@link ChatInputTracker} and read by the overlay, menu integration, and send guard.
 */
@Value
public class FlaggedWord
{
	String word;
	int startOffset;
	int endOffset;
	String suggestion;
}
