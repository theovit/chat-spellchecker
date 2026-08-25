#!/usr/bin/env python3
"""
Tokenizes input text the same way WordTokenizer.java does - maximal runs of letters/apostrophes,
apostrophes trimmed from the ends, lowercased - and reports which resulting words aren't already
covered by the bundled dictionaries (ENABLE1, the frequency list, contractions, or the existing
osrs-terms.txt). Appends any new ones to the end of osrs-terms.txt (preserving its existing
hand-curated ordering/grouping) and prints just the added words to stdout, one per line, so the
calling workflow can tell whether anything changed and what to put in a PR body.

Usage: merge_new_terms.py <path-to-text-file>
       (or pipe text via stdin)
"""
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
RESOURCES = REPO_ROOT / "src" / "main" / "resources" / "com" / "chatspellcheck"

WORD_RE = re.compile(r"[A-Za-z']+")


def tokenize(text):
    words = set()
    for match in WORD_RE.finditer(text):
        word = match.group(0).strip("'").lower()
        if word:
            words.add(word)
    return words


def load_words(path):
    return {
        line.strip().lower()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    }


def main():
    text = Path(sys.argv[1]).read_text(encoding="utf-8") if len(sys.argv) > 1 else sys.stdin.read()

    known = set()
    known |= load_words(RESOURCES / "dictionary-enable1.txt")
    known |= load_words(RESOURCES / "word-frequency-en.txt")
    known |= load_words(RESOURCES / "contractions-en.txt")
    osrs_terms_path = RESOURCES / "osrs-terms.txt"
    known |= load_words(osrs_terms_path)

    new_words = sorted(tokenize(text) - known)
    if not new_words:
        return

    existing_lines = osrs_terms_path.read_text(encoding="utf-8").splitlines()
    osrs_terms_path.write_text("\n".join(existing_lines + new_words) + "\n", encoding="utf-8")

    for word in new_words:
        print(word)


if __name__ == "__main__":
    main()
