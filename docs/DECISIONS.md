# Decisions

## 2026-08-23 — Input-detection and send-block mechanisms deviate from the original design spec

The [design spec](superpowers/specs/2026-08-23-chat-spellcheck-design.md) originally called for
`VarClientStrChanged` (chatbox text changed) and a `KeyManager`-registered `KeyListener` (Enter
interception). Live in-client testing showed neither works:

- `VarClientStrChanged` never fires for the chatbox composition var in the current game build,
  despite being a real, documented event other plugins use for other varcstrs.
- RuneLite's `KeyListener` chain never sees keystrokes typed into the chatbox — they're consumed
  by the client's own input handling before RuneLite's `KeyManager` processes them (confirmed via
  bytecode: `KeyManager.processKeyPressed` bails out immediately if the event already arrived
  consumed).

**What actually works, verified in-client:**

- **Input tracking** (`ChatInputTracker`): subscribes to `ScriptPostFired`, which fires on
  essentially every client tick regardless of which script ran. Rather than filtering to one
  specific script ID (the documented `CHAT_TEXT_INPUT_REBUILD` never fired either — script IDs
  drift between game updates and RuneLite's constants can lag), it treats any firing as a cheap
  "check the input" pulse and only does real tokenize/dictionary work when the text actually
  differs from what was last seen.
- **Send blocking** (`SendGuard`): subscribes to `net.runelite.client.events.ChatboxInput`, the
  event RuneLite's own built-in chat-command handling (`ChatInputManager`) uses to intercept and
  optionally veto a send via `event.consume()`. This is the sanctioned mechanism, not a workaround.

**A second, unrelated bug found during the same debugging pass:** `SpellcheckDictionary`,
`IgnoreListStore`, `ChatInputTracker`, and `SendGuard` are each injected at multiple sites
(plugin, overlay, menu manager, each other). None were `@Singleton`, so Guice was silently
creating a separate instance per injection site — the overlay was reading a `ChatInputTracker`
that never received any events. All four are now `@Singleton`.

## 2026-08-23 — Ignore list is user-editable, not additive-only

The original spec deliberately scoped ignore-list removal out (YAGNI). The user asked for it
directly after using the plugin. Since `IgnoreListStore` already persisted through
`ConfigManager` under the group/key `chat-spellcheck`/`ignoreList`, exposing it as a real
`@ConfigItem` string field on `ChatSpellcheckConfig` with the same key was a ~5-line change — no
migration needed. `IgnoreListStore` now also subscribes to `ConfigChanged` to reload its
in-memory cache when the user edits the field directly (bypassing `add()`).
