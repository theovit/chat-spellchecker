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
- [x] `IgnoreListStore` — ConfigManager-backed persistence, editable in settings panel
- [x] `ChatInputTracker` — `ScriptPostFired`-driven tokenizer + flagged-word state
- [x] `ChatSpellcheckOverlay` — underline at real text baseline, inline boxed suggestion
- [x] Right-click ignore integration (`SpellcheckMenuManager`, `MenuEntryAdded`/`MenuEntry#onClick`)
- [x] `SendGuard` — `ChatboxInput` veto-based send blocking, block/confirm state
- [x] `ChatSpellcheckConfig` — `blockOnTypos`, `highlightMisspelledWords`, `ignoreList`
- [x] `ChatSpellcheckPlugin` — wiring/DI, all shared components `@Singleton`

## Tests & verification
- [x] JUnit tests: tokenizer, dictionary lookup/suggest, ignore-list serialization, geometry substring search (17 tests)
- [x] `./gradlew build` passes
- [x] Manual in-client verification — highlighting, suggestion, ignore-list, send-block all confirmed working

## Docs
- [x] `docs/CHANGELOG.md` entry
- [x] `README.md` update

## Known follow-ups (not blocking)
- `runelite-plugin.properties` `author` is still a placeholder (`Nobody`) — update if publishing to the plugin hub.
