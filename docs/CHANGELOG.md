# Changelog

## 2026-08-24

- Suggestions are now ranked by real-world word frequency instead of arbitrary set-iteration
  order, so "wrld" suggests "world" instead of the rarer "wold". Bundled a 50k-word frequency
  list (`hermitdave/FrequencyWords`, CC-BY-SA 4.0) alongside ENABLE1.
- Contractions (`don't`, `can't`, `won't`, `y'all`, `gonna`, ...) are recognized instead of being
  flagged as typos — neither bundled word list contained any apostrophe words at all.
- The suggestion for the word you're actively typing now "pins": backspacing a misspelled word
  keeps showing its last suggestion instead of flickering through whatever each half-deleted
  fragment would suggest on its own, for as long as at least one character remains. It drops
  immediately once the word is fully backspaced away, and clears if you retype something that
  diverges from the pinned target. A configurable auto-hide timeout (on/off + seconds) is
  available in settings for players who don't want it lingering indefinitely.
- The suggestion box now colors the part you've already typed differently from the part still
  left to type, so progress toward the correction is visible at a glance.
- The underline, the suggestion box (text/background/border), the "message blocked" notice
  (text/background/border), the right-click "ignore" menu entry's text color, and the box font
  size are all now customizable in a new "Appearance" settings section.
- The "message blocked" notice is now a proper boxed label anchored at the (now-empty) input
  line instead of a plain drop-shadowed line of text floating above the chat history — it was
  effectively unreadable against some backgrounds before.
- Simplified send-block banner logic: it previously tried to tell public chat and private
  messages apart by checking whether the input box still had the blocked text in it, but the
  client actually clears the input on Enter in both modes regardless of whether the send was
  blocked. It's now a single timed notice for both, which is both simpler and matches what
  actually happens in-client.

## 2026-08-23

- Implemented the Chat Spellcheck plugin end to end: overlay highlighting of misspelled chat
  input words with suggestion tooltips, right-click "add to ignore list", and a send-block
  confirmation prompt for messages containing likely typos. Nothing runs on a tick or frame
  timer — everything is driven by the chatbox input actually changing.
- Renamed the plugin template from `com.example`/`Example` to `com.chatspellcheck`/`Chat Spellcheck`.
- Bundled the ENABLE1 word list (public domain, 172,823 words) plus a curated OSRS-terms list
  so common game/community vocabulary isn't flagged.
- Fixed live-typing detection and send-blocking against the actual running client: neither
  `VarClientStrChanged` nor RuneLite's `KeyListener` chain fires for chatbox composition in this
  game build, so input tracking now piggybacks on `ScriptPostFired` and send-blocking uses
  RuneLite's own `ChatboxInput` veto event. Also fixed a Guice scoping bug where several
  components weren't `@Singleton`, so different parts of the plugin were silently reading and
  writing separate instances.
- Polish: underline now sits at the widget font's real baseline instead of the bottom of the
  whole input line; the suggestion is now a boxed inline label next to the word currently being
  typed instead of a hover tooltip; the ignore list is now an editable field in the plugin's
  settings panel instead of hidden config.
- Added private-message support (highlighting, suggestion, send-block, and right-click ignore
  all now work in PMs, not just public/clan chat). This took several rounds of in-client
  debugging: PMs turned out to use an entirely different var (`VarClientID.MESLAYERINPUT` vs
  `VarClientID.CHATINPUT`) and a different display widget (`Chatbox.MES_TEXT2` vs
  `Chatbox.INPUT`, discovered by dumping widget candidates live). The PM compose window also
  closes on Enter even when the send is blocked, so the confirm banner uses a timed fallback
  there instead of the text-match check public chat uses. `MES_TEXT2` also turned out to be
  center-aligned text within a much wider container, so bounds math had to become
  alignment-aware (`Widget#getXTextAlignment()`) instead of assuming left-flush. See
  docs/DECISIONS.md for the full trail.
