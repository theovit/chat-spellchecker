# Chat Spellcheck

A RuneLite plugin that flags likely misspelled words as you type in the chatbox, shows a
suggested correction, and warns before sending a message with typos in it. Works in public,
clan/friends, and private-message chat.

The plugin never inserts or modifies text in the chatbox — you always retype the word yourself.

## Features

- **Highlighting** — misspelled words are underlined live as you type, driven entirely by the
  chatbox input actually changing (never a tick or frame timer).
- **Inline suggestion** — a boxed label next to the word you're typing shows the best correction,
  ranked by real-world word frequency rather than an arbitrary dictionary match. The part you've
  already typed correctly is colored differently from the part still left to type, so progress is
  visible at a glance. The suggestion "pins" while you backspace a misspelled word so it doesn't
  flicker between intermediate guesses, and clears once the word is fully deleted or you type
  something that no longer matches it. An optional auto-hide timeout is available in settings.
- **Send-blocking** — pressing Enter with likely typos present blocks the send and shows a boxed
  notice at the input line instead of sending immediately. Retype and press Enter again to send.
- **Ignore list** — right-click a flagged word to add it to your personal ignore list, editable
  directly as a comma-separated field in the plugin's settings.
- **Dictionary** — the public-domain ENABLE1 word list plus a curated OSRS-terms list, a 50k-word
  frequency list for suggestion ranking, and common English contractions (`don't`, `can't`, `y'all`, ...).
- **Appearance settings** — underline color, suggestion box colors (text/background/border), the
  blocked-message notice colors, the right-click ignore entry's text color, and font size are all
  customizable.

## Not customizable

The right-click "ignore" menu entry's panel background/border is drawn natively by the game
client and can't be restyled by any plugin — only the RuneLite/OSRS client itself controls that.
Its text color is customizable, as noted above.
