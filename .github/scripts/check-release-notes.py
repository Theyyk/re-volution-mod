"""Validate the Russian documentation and English publication notes for a release."""

import re
import sys
from pathlib import Path


def validate(tag, russian, english):
    errors = []
    if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+", tag):
        errors.append("Expected a release tag such as v1.1.7")
    if not russian.strip() or not re.search(r"[А-Яа-яЁё]", russian):
        errors.append("Russian release documentation is missing or has no Russian text")
    if not english.strip():
        errors.append("English release notes are missing or empty")
    if re.search(r"[А-Яа-яЁё]", english):
        errors.append("English release notes contain Russian text")
    for heading in (f"## Custom GUI Mod {tag}", "### Added", "### Fixed", "### Other", "### Download"):
        if heading not in english.splitlines():
            errors.append(f"Missing English release heading: {heading}")
    if not re.search(r"^\*\*Full Changelog:\*\*\s+\S", english, re.MULTILINE):
        errors.append("Missing Full Changelog link")
    return errors


def main():
    if len(sys.argv) != 2:
        print("Usage: python check-release-notes.py vX.Y.Z")
        return 1
    tag = sys.argv[1]
    if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+", tag):
        print("Expected a release tag such as v1.1.7")
        return 1
    root = Path(__file__).resolve().parents[2]
    paths = (root / "docs/releases" / f"{tag}.md", root / ".github/release-notes" / f"{tag}.md")
    texts = [path.read_text(encoding="utf-8-sig") if path.is_file() else "" for path in paths]
    errors = validate(tag, *texts)
    for error in errors:
        print(error)
    if errors:
        return 1
    print(f"Russian and English release descriptions validated: {tag}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
