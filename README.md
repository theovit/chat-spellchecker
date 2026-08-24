# Chat Spellcheck

A RuneLite plugin that flags likely misspelled words as you type in the chatbox and warns
before sending a message with typos in it.

- **Highlighting** — misspelled words are underlined in an overlay drawn over the chat input,
  with a suggested correction shown as a tooltip on hover. You retype the word yourself; the
  plugin never inserts text into the chatbox.
- **Send-blocking** — pressing Enter with likely typos present shows a warning instead of
  sending immediately. Press Enter again with the text unchanged to send anyway.

Right-click a flagged word to add it to your personal ignore list.

See [docs/superpowers/specs/2026-08-23-chat-spellcheck-design.md](docs/superpowers/specs/2026-08-23-chat-spellcheck-design.md)
for the design, and [AGENTS.md](AGENTS.md) for the plugin development guidelines this repo follows.
