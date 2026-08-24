# Changelog

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
