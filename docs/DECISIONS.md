# Decisions

## 2026-08-24 — The right-click menu panel can't be restyled, and we're not replacing it

The "Ignore" entry added via `client.getMenu().createMenuEntry()` renders inside the game's
native right-click menu, and the user found its default translucent background/border hard to
read. Checked the actual API surface (`javap` on `runelite-api`'s `MenuEntry`/`Menu`) before
concluding anything: the only appearance hooks are `setOption`/`setTarget` (text, which supports
embedded `<col=RRGGBB>` tags — now wired to `ChatSpellcheckConfig#ignoreMenuColor()`). There is no
background/border color, no style hook of any kind — the panel is drawn by the client's own
interface rendering, the same code path used for every right-click menu in the game, not
something attached per-entry.

The real fix would be to stop using the native menu for this entirely and draw our own clickable
"ignore" control via a custom overlay (fully colorable, like the suggestion box) with our own
click hit-testing. Presented this tradeoff to the user — they chose to keep the native menu entry
as-is rather than take on that additional interaction-model work. Revisit if this becomes a
recurring complaint.

## 2026-08-24 — Suggestion pin, timeout, and full color/size customization

Several rounds of live-tested refinement, in order:

- **Frequency-ranked suggestions.** `SpellcheckDictionary` previously returned whichever
  edit-distance candidate a `HashSet` happened to iterate first. Added a `hermitdave/FrequencyWords`
  50k-word list, unioned into the correctness set (it also plugs the ENABLE1 gap where short
  common words like "a"/"i" aren't included) and used to rank candidates by real-world commonness.
- **Contractions.** Neither bundled word list contains any apostrophe words - `WordTokenizer`
  already handled apostrophes correctly (`don't` tokenized as one word), but the word just was
  never in the dictionary. Added a small bundled contractions list rather than trying to
  synthesize them from a base-verb list, since the closed set of English contractions is small and
  the "expand + inflect" alternative is significantly more machinery for the same result.
- **Pinned/sticky suggestion.** Since the plugin can never insert the correction itself, the
  suggestion for the word currently being typed now "pins" once shown, so backspacing doesn't
  flicker through intermediate garbage suggestions. Clears immediately at zero characters (nothing
  left to anchor to) rather than lingering. A configurable auto-hide timeout was added alongside
  it for players who want the box to disappear on its own regardless.
- **Split-colored suggestion text.** The part of the suggestion matching what's typed so far
  renders in a different color than the remaining part, so progress is visible without reading
  closely.
- **Full appearance customization.** Every color previously hardcoded as a `static final Color`
  (underline, suggestion box text/background/border, blocked-message text/background/border,
  ignore-menu text) moved to `@ConfigItem`/`@Alpha` fields under a new "Appearance" config
  section. Suggestion/blocked-message box size was deliberately *not* made a separate setting -
  both are already fully derived from font metrics, so a font-size setting alone keeps the box
  and its text in sync automatically; a separate size knob would just let them go out of sync.
- **Blocked-message notice redesign.** Previously positioned above the chat history and drawn as
  plain `OverlayUtil` text (a drop shadow, no box) - hard to read, and anchored somewhere the user
  wasn't looking. Also, its logic tried to distinguish public chat (persisted text) from PMs
  (didn't) by comparing the input box's current text to what was blocked, but that assumption was
  wrong for public chat too - the client clears the input on Enter in both modes regardless of the
  block. Replaced with a single boxed notice (same background/border/text styling as the
  suggestion box, all colorable) anchored directly at the input line and shown for a fixed 3
  seconds after any block, mode-independent.

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

## 2026-08-23 — Private messages need a second var, a second widget, and different confirm-banner logic

Public/clan chat and private messages turned out to be genuinely separate subsystems in the
client, not just different labels on the same input. Discovered by adding temporary in-client
diagnostics across several rounds (typing while composing, dumping candidate widgets) after the
user reported PM support silently not working:

- **Two different vars carry the typed text.** `VarClientID.CHATINPUT` for public/clan/friends
  chat; `VarClientID.MESLAYERINPUT` for private messages (also used for bank search, GE, etc.).
  Reading only one broke the other mode - `ChatInputTracker.currentTypedText()` now reads both
  and uses whichever is non-empty. (`VarClientStr.CHATBOX_TYPED_TEXT`, tried first, turned out to
  be the deprecated legacy form of `CHATINPUT` - same var, different API surface.)
- **Two different widgets render the text.** `Chatbox.INPUT` for public chat; `Chatbox.MES_TEXT2`
  for PMs (`Chatbox.INPUT` stays hidden throughout PM composition). Found by dumping every
  `Chatbox.*` widget's hidden/text/bounds/font while composing a PM.
  `ChatInputTracker.currentInputWidget()` resolves the right one.
- **The PM compose window closes on Enter even when the send is blocked** - the client resets it
  regardless of `event.consume()`. Public chat's box persists across a block, which is what the
  "retype it unchanged to confirm" flow relies on. For PMs there's nothing left to compare
  against, so `SendGuard` now also tracks `pendingSince` and the overlay shows a 3-second timed
  banner for PMs instead of a text-match-gated one.
- **`MES_TEXT2` is center-aligned** within a much wider container than the text itself, while
  `Chatbox.INPUT` is left-flush. `ChatInputGeometry` assumed left-flush, which visually offset the
  underline/suggestion box to the left of the real glyphs. Fixed by reading
  `Widget#getXTextAlignment()` and computing the actual rendered text-block start accordingly.
