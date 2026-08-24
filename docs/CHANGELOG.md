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
