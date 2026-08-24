package com.chatspellcheck;

import lombok.Value;

/** A word found by {@link WordTokenizer}, with its character offsets into the source string. */
@Value
public class WordToken
{
	String word;
	int startOffset;
	int endOffset;
}
