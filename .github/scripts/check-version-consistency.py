"""Fail CI when release/version metadata drifts between source files."""

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BUILD_GRADLE = ROOT / "build.gradle"
MCMOD_INFO = ROOT / "src/main/resources/mcmod.info"
MOD_CLASS = ROOT / "src/main/java/com/example/revolution/RevolutionMod.java"
README = ROOT / "README.md"


def read_build_version():
    text = BUILD_GRADLE.read_text(encoding="utf-8-sig")
    match = re.search(r"(?m)^\s*version\s*=\s*['\"]([^'\"]+)['\"]", text)
    if not match:
        raise ValueError("build.gradle: version assignment was not found")
    return match.group(1)


def read_mcmod_version():
    data = json.loads(MCMOD_INFO.read_text(encoding="utf-8-sig"))
    if not isinstance(data, list) or not data or not isinstance(data[0], dict):
        raise ValueError("src/main/resources/mcmod.info: expected a non-empty JSON array")
    version = data[0].get("version")
    if not isinstance(version, str) or not version.strip():
        raise ValueError("src/main/resources/mcmod.info: version is missing")
    return version.strip()


def read_source_version():
    text = MOD_CLASS.read_text(encoding="utf-8-sig")
    match = re.search(
        r'public\s+static\s+final\s+String\s+VERSION\s*=\s*"([^"]+)"\s*;',
        text,
    )
    if not match:
        raise ValueError(
            "src/main/java/com/example/revolution/RevolutionMod.java: VERSION constant was not found"
        )
    return match.group(1)


def read_readme_stable_version():
    text = README.read_text(encoding="utf-8-sig")
    match = re.search(r"\*\*Стабильная версия:\s*`v([0-9]+\.[0-9]+\.[0-9]+)`\.\*\*", text)
    if not match:
        raise ValueError("README.md: stable version marker was not found")
    return match.group(1)


def read_readme_contract_version():
    text = README.read_text(encoding="utf-8-sig")
    match = re.search(
        r"\[Контракт v([0-9]+\.[0-9]+\.[0-9]+)\]\(docs/releases/v\1\.md\)",
        text,
    )
    if not match:
        raise ValueError("README.md: current release contract link was not found")
    return match.group(1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--tag",
        help="Optional release tag (for example v1.2.12) that must match all metadata",
    )
    args = parser.parse_args()

    try:
        versions = {
            "build.gradle": read_build_version(),
            "src/main/resources/mcmod.info": read_mcmod_version(),
            "src/main/java/com/example/revolution/RevolutionMod.java": read_source_version(),
            "README.md stable version": read_readme_stable_version(),
            "README.md current release contract": read_readme_contract_version(),
        }
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"Version consistency check failed: {exc}")
        return 1

    for path, version in versions.items():
        print(f"{path}: {version}")

    expected = versions["build.gradle"]
    # Development source may be ahead of the published Stable. Tags remain strict.
    source_paths = (
        "build.gradle",
        "src/main/resources/mcmod.info",
        "src/main/java/com/example/revolution/RevolutionMod.java",
    )
    stable = versions["README.md stable version"]
    contract = versions["README.md current release contract"]
    mismatches = [
        f"{path}={versions[path]} (expected {expected})"
        for path in source_paths
        if versions[path] != expected
    ]
    if stable != contract:
        mismatches.append(
            f"README.md release contract={contract} (expected published Stable {stable})"
        )

    def semantic_tuple(value):
        if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", value):
            raise ValueError(f"Invalid semantic version: {value}")
        return tuple(int(piece) for piece in value.split("."))

    try:
        if semantic_tuple(stable) > semantic_tuple(expected):
            mismatches.append(
                f"README.md published Stable={stable} newer than source version {expected}"
            )
    except ValueError as exc:
        mismatches.append(str(exc))

    if args.tag:
        if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+", args.tag):
            mismatches.append(f"tag={args.tag} (expected semantic tag vX.Y.Z)")
        else:
            tag_version = args.tag[1:]
            if tag_version != expected:
                mismatches.append(f"tag={args.tag} (expected v{expected})")
            # Tag publication requires that the README no longer points at old Stable.
            if stable != expected:
                mismatches.append(
                    f"README.md stable version={stable} (tag requires {expected})"
                )
            if contract != expected:
                mismatches.append(
                    f"README.md release contract={contract} (tag requires {expected})"
                )

    if mismatches:
        print("Version metadata mismatch detected:")
        for mismatch in mismatches:
            print(f"- {mismatch}")
        return 1

    if args.tag:
        print(f"Version metadata, README and release tag are consistent: {args.tag}")
    else:
        print(f"Source metadata consistent: {expected}; published Stable README: {stable}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
