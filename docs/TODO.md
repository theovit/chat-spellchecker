# TODO — Chat Spellcheck

Implementation checklist for [chat-spellcheck-design.md](superpowers/specs/2026-08-23-chat-spellcheck-design.md).

## Template rename
- [x] Bundle ENABLE1 wordlist (public domain, 172,823 words) as `dictionary-enable1.txt`
- [x] Rename package `com.example` → `com.chatspellcheck`
- [x] Rename `ExamplePlugin`/`ExampleConfig` → `ChatSpellcheckPlugin`/`ChatSpellcheckConfig`
- [x] Update `build.gradle`, `settings.gradle`, `runelite-plugin.properties`

## Components
- [x] `SpellcheckDictionary` — load wordlist + OSRS terms, `isCorrect`, Norvig `suggest`
- [x] OSRS supplemental terms resource (item/skill names, common abbreviations)
- [x] `IgnoreListStore` — ConfigManager-backed persistence
- [x] `ChatInputTracker` — `VarClientStrChanged` subscriber, tokenizer, flagged-word state
- [x] `ChatSpellcheckOverlay` — per-frame render of flagged words + suggestion tooltip
- [x] Right-click ignore integration (`SpellcheckMenuManager`, `MenuEntryAdded`/`MenuEntry#onClick`)
- [x] `SendGuard` — `KeyManager` Enter interception, block/confirm state
- [x] `ChatSpellcheckConfig` — `blockOnTypos`, `highlightMisspelledWords`
- [x] `ChatSpellcheckPlugin` — wiring/DI

## Tests & verification
- [x] JUnit tests: tokenizer, dictionary lookup/suggest, ignore-list serialization, geometry offset mapping (17 tests)
- [x] `./gradlew build` passes
- [ ] Manual in-client test checklist handed to user (per AGENTS.md — no automated game input)

## Docs
- [x] `docs/CHANGELOG.md` entry
- [x] `README.md` update

## Known follow-ups (not blocking)
- Overlay/menu geometry (`ChatInputGeometry`) is a best-effort mapping from typed-text offsets to
  the widget's rendered text (which splices in a cursor glyph); verify visually in-client,
  especially while editing mid-string with arrow keys.
- `runelite-plugin.properties` `author` is still a placeholder (`Nobody`) — update if publishing to the plugin hub.
