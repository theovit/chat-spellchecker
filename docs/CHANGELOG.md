# Changelog

## 2026-08-23

- Implemented the Chat Spellcheck plugin end to end: overlay highlighting of misspelled chat
  input words with suggestion tooltips, right-click "add to ignore list", and a send-block
  confirmation prompt for messages containing likely typos. Nothing runs on a tick or frame
  timer — everything is driven by the chatbox input actually changing.
- Renamed the plugin template from `com.example`/`Example` to `com.chatspellcheck`/`Chat Spellcheck`.
- Bundled the ENABLE1 word list (public domain, 172,823 words) plus a curated OSRS-terms list
  so common game/community vocabulary isn't flagged.
