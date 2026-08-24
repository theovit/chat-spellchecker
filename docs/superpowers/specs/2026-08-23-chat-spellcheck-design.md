# Chat Spellcheck — Design Spec

## Overview

A RuneLite plugin that spellchecks the chatbox input as the player types, without ever programmatically modifying the chatbox text or injecting input — both of which are forbidden by Jagex's third-party client guidelines (see [AGENTS.md](../../../AGENTS.md)). The plugin provides two complementary behaviors:

1. **Overlay highlighting** — misspelled words are visually flagged in an overlay drawn over the chat input, with a suggested correction shown as a tooltip. The player must retype the word themselves; the plugin never inserts text.
2. **Send-blocking** — pressing Enter with likely typos present shows a confirmation prompt instead of sending immediately. A second Enter (with the text unchanged) lets the real keypress through and the message sends normally. The plugin never alters the outgoing message, it only chooses whether to consume the genuine keypress once.

Both behaviors read from a single shared "currently flagged words" computation, kept in sync via RuneLite's `VarClientStrChanged` event (not per-tick polling — see Data Flow).

## Architecture

```
VarClientStrChanged (chatbox input changed)
        │
        ▼
ChatInputTracker  ──uses──▶ SpellcheckDictionary + IgnoreListStore
        │
        ▼
  flagged words: [{word, startOffset, endOffset}, ...]
        │
        ├──▶ ChatSpellcheckOverlay (per-frame render, reads flagged list only)
        ├──▶ Right-click ignore integration (MenuEntryAdded / MenuOptionClicked)
        └──▶ SendGuard (KeyManager, Enter interception)
```

## Components

### `SpellcheckDictionary`
- Loads a bundled English wordlist (union of British + American spellings) plus a supplemental OSRS-terms list (item/skill names, common abbreviations) from plugin resources.
- Loaded into a `HashSet<String>` on plugin startup, off the client thread (via the plugin's executor — resource read is I/O).
- `isCorrect(String word)` — case-insensitive set membership check.
- `suggest(String word)` — Norvig-style correction: generate all edit-distance-1 candidates (deletes, transposes, replaces, inserts) and check set membership; if none found, generate edit-distance-2 by composing edit-1 over each edit-1 candidate. No external dependency required.

### `IgnoreListStore`
- Persists user-added ignored words via `ConfigManager`, using a hidden (non-`@ConfigItem`) key scoped to the group `chat-spellcheck`, so it rides along with the user's RuneLite profile.
- `add(String word)`, `contains(String word)`.
- Exposed as an editable `ignoreList` `@ConfigItem` (comma-separated) on `ChatSpellcheckConfig`, so entries can be reviewed and removed directly in the plugin's settings panel.

### `ChatInputTracker`
- `@Subscribe public void onVarClientStrChanged(VarClientStrChanged event)`, filtered to the chatbox-input var index.
- On a relevant change, tokenizes the current typed text into words with character offsets, filters out words that are correct or ignore-listed, and stores the resulting flagged-word list (with offsets) as plugin state.
- Does not run on every game tick or frame — only on actual text change.

### `ChatSpellcheckOverlay`
- Standard RuneLite `Overlay`, rendered over the chat input widget.
- For each flagged word, computes its bounding box using `Widget#getFont()` text-width measurement of the substring preceding it, combined with the widget's screen bounds.
- Draws a highlight/underline per flagged word and a suggestion tooltip (from `SpellcheckDictionary#suggest`) near the cursor or hovered word.
- Per-frame cost stays cheap: it only reads the already-computed flagged-word list, it never re-tokenizes or re-checks the dictionary.
- Toggled by `highlightMisspelledWords` config (default on).

### Right-click ignore integration
- On `MenuEntryAdded`, checks whether the current mouse position falls inside a tracked flagged-word bounding box.
- If so, injects a menu entry ("Add '<word>' to spellcheck ignore list") via `client.createMenuEntry(...)`.
- `MenuOptionClicked` handler for that entry calls `IgnoreListStore#add` and triggers a re-check so the word stops being flagged immediately.

### `SendGuard`
- A `KeyListener` registered via `KeyManager`, active while the chat input is focused.
- On Enter: if `blockOnTypos` is enabled and the current flagged-word list is non-empty, consume the event once and show a "typos found — press Enter again to send" prompt, recording the text snapshot this block applies to.
- On a subsequent Enter where the text is unchanged since the block, do not consume — the real keypress passes through and the game sends the message normally.
- Any edit to the text after a block clears the pending-confirm state, requiring a fresh confirmation.
- Toggled by `blockOnTypos` config (default **on**).

### `ChatSpellcheckConfig`
- Config group: `chat-spellcheck`.
- `blockOnTypos: boolean`, default `true`.
- `highlightMisspelledWords: boolean`, default `true`.

## Data Flow

Typing triggers `VarClientStrChanged` → `ChatInputTracker` recomputes the flagged-word list once → overlay, right-click integration, and `SendGuard` all read that same list on demand (overlay every frame, the other two on their respective events). No component re-derives flagged words independently, and nothing runs on a tick or frame timer.

## Error Handling

- Dictionary load failure (missing/corrupt resource) fails open: log at `debug`/`warn`, disable checking for the session, chat behaves as vanilla. Spellcheck must never be able to block chat entirely.
- Overlay render guards against null/invalid widget bounds (e.g. during UI resize) by skipping the frame rather than throwing.
- `SendGuard`'s block state is tied to a snapshot of the flagged text, so it cannot get stuck swallowing Enter indefinitely — any text change clears it.

## Testing

- `SpellcheckDictionary`, the tokenizer, the suggestion algorithm, and `IgnoreListStore` serialization are plain Java with no client dependency — covered by JUnit tests.
- Overlay rendering, right-click menu injection, and key interception require a running client and manual verification (per this repo's testing rules — no automated game input). A specific test checklist will be provided once implemented, covering: overlay highlighting on typos, suggestion tooltip accuracy, right-click ignore persisting across relog, send-block prompt + second-Enter passthrough, and both config toggles.

## Out of Scope (v1)

- Remote/updatable dictionary — bundled wordlist only, no network calls.
- Per-word suggestion cycling (only the top suggestion is shown).
