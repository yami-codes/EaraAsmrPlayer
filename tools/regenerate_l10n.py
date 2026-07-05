#!/usr/bin/env python3
"""Regenerate Android string resources and the Strings.kt facade from l10n/*.json.

Workflow (mirrors LizuNemuri's ARB + codegen pattern):
  1. Edit l10n/strings_en.json (template), strings_th.json, strings_zh-CN.json
  2. python3 tools/regenerate_l10n.py
  3. Commit the JSON sources + generated outputs
"""
from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L10N_DIR = ROOT / "l10n"
ANDROID_OUT = {
    "en": ROOT / "app/src/main/res/values/strings.xml",
    "th": ROOT / "app/src/main/res/values-th/strings.xml",
    "zh-CN": ROOT / "app/src/main/res/values-zh-rCN/strings.xml",
}
STRINGS_KT = ROOT / "app/src/main/java/com/asmr/player/i18n/Strings.kt"
MAPPING_FILE = L10N_DIR / "key_mapping.json"

STOP_WORDS = frozenset(
    "a an the to for of in on at is are be or and if it please this that with from".split()
)

# LizuNemuri semantic names for strings whose Chinese text matches Eara's.
LIZU_ZH_TO_KEY: dict[str, str] = {}


def load_lizu_mapping() -> None:
    lizu_path = ROOT / "reference" / "LizuNemuri" / "app_zh.arb"
    if not lizu_path.exists():
        return
    data = json.loads(lizu_path.read_text(encoding="utf-8"))
    for key, value in data.items():
        if key.startswith("@") or not isinstance(value, str):
            continue
        LIZU_ZH_TO_KEY.setdefault(value, camel_to_snake(key))


def camel_to_snake(name: str) -> str:
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def snake_to_camel(name: str) -> str:
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def parse_strings_xml(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    tree = ET.parse(path)
    root = tree.getroot()
    out: dict[str, str] = {}
    for child in root.findall("string"):
        name = child.attrib.get("name")
        if name:
            out[name] = child.text or ""
    return out


def escape_android_xml(value: str) -> str:
    value = value.replace("&", "&amp;")
    value = value.replace("<", "&lt;")
    value = value.replace(">", "&gt;")
    value = value.replace("\\", "\\\\")
    value = value.replace("'", "\\'")
    value = value.replace("\n", "\\n")
    value = value.replace('"', '\\"')
    return value


def needs_formatted_false(value: str) -> bool:
    return "%" in value and "%%" not in value.replace("%%", "")


def write_strings_xml(path: Path, strings: dict[str, str]) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for key in sorted(strings.keys()):
        value = strings[key]
        attrs = ""
        if needs_formatted_false(value):
            attrs = ' formatted="false"'
        lines.append(f'    <string name="{key}"{attrs}>{escape_android_xml(value)}</string>')
    lines.append("</resources>")
    lines.append("")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")


def sanitize_resource_key(key: str) -> str:
    key = key.replace("-", "_")
    key = re.sub(r"_+", "_", key).strip("_")
    if re.match(r"^\d", key):
        key = f"key_{key}"
    return key


def english_to_key(english: str, taken: set[str]) -> str:
    text = re.sub(r"%\d+\$[sd]", " ", english)
    text = re.sub(r"%%", " ", text)
    text = re.sub(r"[^\w\s]", " ", text)
    words = [
        w
        for w in text.strip().lower().split()
        if w and w not in STOP_WORDS and not w.isdigit()
    ]
    if not words:
        words = ["text"]
    key = "_".join(words[:6])
    key = sanitize_resource_key(key)
    if len(key) > 48:
        key = key[:48].rstrip("_")
    base = key
    n = 2
    while key in taken:
        key = f"{base}_{n}"
        n += 1
    taken.add(key)
    return key


def kotlin_identifier_for_key(key: str) -> str:
    camel = snake_to_camel(sanitize_resource_key(key))
    if re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", camel):
        return camel
    safe = re.sub(r"[^a-zA-Z0-9_]", "_", camel)
    if re.match(r"^\d", safe):
        safe = f"key_{safe}"
    return safe


def is_semantic_key(key: str) -> bool:
    return not key.startswith("str_")


def build_semantic_mapping(
    en: dict[str, str], zh: dict[str, str]
) -> dict[str, str]:
    taken = {k for k in en if is_semantic_key(k)}
    mapping: dict[str, str] = {}

    for old_key in sorted(en.keys()):
        if is_semantic_key(old_key):
            mapping[old_key] = old_key
            continue
        zh_val = zh.get(old_key, "")
        if zh_val in LIZU_ZH_TO_KEY:
            candidate = LIZU_ZH_TO_KEY[zh_val]
            if candidate not in taken:
                taken.add(candidate)
                mapping[old_key] = candidate
                continue
        new_key = english_to_key(en[old_key], taken)
        mapping[old_key] = new_key

    return mapping


def remap_dict(data: dict[str, str], mapping: dict[str, str]) -> dict[str, str]:
    return {mapping.get(k, k): v for k, v in data.items()}


def count_placeholders(value: str) -> int:
    return len(re.findall(r"%\d+\$[sd]", value))


def generate_strings_kt(strings: dict[str, str]) -> str:
    lines = [
        "package com.asmr.player.i18n",
        "",
        "import android.content.Context",
        "import androidx.annotation.StringRes",
        "import androidx.compose.runtime.Composable",
        "import androidx.compose.ui.res.stringResource",
        "import com.asmr.player.R",
        "",
        "/**",
        " * Central UI copy — generated by tools/regenerate_l10n.py",
        " * Edit l10n/strings_*.json then re-run the script (LizuNemuri-style workflow).",
        " */",
        "object Strings {",
    ]

    for key in sorted(strings.keys()):
        value = strings[key]
        camel = kotlin_identifier_for_key(key)
        n = count_placeholders(value)
        res = f"R.string.{key}"

        if n == 0:
            lines.extend(
                [
                    "",
                    f"    @get:StringRes val {camel}Res: Int get() = {res}",
                    "",
                    f"    fun {camel}(context: Context): String = context.getString({camel}Res)",
                    "",
                    "    @Composable",
                    f"    fun {camel}(): String = stringResource({camel}Res)",
                ]
            )
        else:
            params = ", ".join(f"arg{i}: Any" for i in range(1, n + 1))
            args = ", ".join(f"arg{i}" for i in range(1, n + 1))
            lines.extend(
                [
                    "",
                    f"    @get:StringRes val {camel}Res: Int get() = {res}",
                    "",
                    f"    fun {camel}(context: Context, {params}): String =",
                    f"        context.getString({camel}Res, {args})",
                    "",
                    "    @Composable",
                    f"    fun {camel}({params}): String = stringResource({camel}Res, {args})",
                ]
            )

    lines.extend(["", "    fun languageLabelRes(@StringRes key: String): Int = when (key) {", '        "language_system" -> languageSystemRes', '        "language_english" -> languageEnglishRes', '        "language_thai" -> languageThaiRes', '        "language_chinese_simplified" -> languageChineseSimplifiedRes', "        else -> languageSystemRes", "    }", "}", ""])
    return "\n".join(lines)


def export_from_android() -> None:
    """One-time export of current Android XML into l10n JSON sources."""
    L10N_DIR.mkdir(parents=True, exist_ok=True)
    locales = {
        "en": ANDROID_OUT["en"],
        "th": ANDROID_OUT["th"],
        "zh-CN": ANDROID_OUT["zh-CN"],
    }
    for locale, path in locales.items():
        data = parse_strings_xml(path)
        out = L10N_DIR / f"strings_{locale}.json"
        out.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"exported {len(data)} strings -> {out.relative_to(ROOT)}")


def migrate_keys() -> dict[str, str]:
    en_path = L10N_DIR / "strings_en.json"
    zh_path = L10N_DIR / "strings_zh-CN.json"
    if not en_path.exists():
        export_from_android()

    en = json.loads(en_path.read_text(encoding="utf-8"))
    zh = json.loads(zh_path.read_text(encoding="utf-8")) if zh_path.exists() else {}
    load_lizu_mapping()
    mapping = build_semantic_mapping(en, zh)

    # Only rewrite JSON if hash keys remain
    if any(k.startswith("str_") for k in en):
        for locale in ("en", "th", "zh-CN"):
            src = json.loads((L10N_DIR / f"strings_{locale}.json").read_text(encoding="utf-8"))
            remapped = remap_dict(src, mapping)
            (L10N_DIR / f"strings_{locale}.json").write_text(
                json.dumps(remapped, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
        MAPPING_FILE.write_text(
            json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        renamed = sum(1 for o, n in mapping.items() if o != n)
        print(f"renamed {renamed} hash keys to semantic names")
    else:
        mapping = json.loads(MAPPING_FILE.read_text(encoding="utf-8")) if MAPPING_FILE.exists() else {k: k for k in en}

    return mapping


def regenerate_android() -> dict[str, str]:
    en = json.loads((L10N_DIR / "strings_en.json").read_text(encoding="utf-8"))
    for locale, out_path in ANDROID_OUT.items():
        data = json.loads((L10N_DIR / f"strings_{locale}.json").read_text(encoding="utf-8"))
        write_strings_xml(out_path, data)
        print(f"wrote {len(data)} strings -> {out_path.relative_to(ROOT)}")
    STRINGS_KT.write_text(generate_strings_kt(en), encoding="utf-8")
    print(f"wrote facade -> {STRINGS_KT.relative_to(ROOT)}")
    return en


def codemod_kotlin(mapping: dict[str, str]) -> int:
    changes = {o: n for o, n in mapping.items() if o != n}
    if not changes:
        return 0
    app_dir = ROOT / "app/src/main/java"
    count = 0
    for path in app_dir.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        original = text
        for old, new in sorted(changes.items(), key=lambda x: -len(x[0])):
            text = text.replace(f"R.string.{old}", f"R.string.{new}")
        if text != original:
            path.write_text(text, encoding="utf-8")
            count += 1
    return count


def fix_invalid_keys() -> dict[str, str]:
    """Rename keys that are invalid Android/Kotlin identifiers."""
    mapping: dict[str, str] = {}
    for locale in ("en", "th", "zh-CN"):
        path = L10N_DIR / f"strings_{locale}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        taken = set(data.keys())
        updated: dict[str, str] = {}
        for key, value in data.items():
            new_key = sanitize_resource_key(key)
            if new_key != key:
                base = new_key
                n = 2
                while new_key in taken or new_key in updated:
                    new_key = f"{base}_{n}"
                    n += 1
                mapping[key] = new_key
                taken.add(new_key)
                updated[new_key] = value
            else:
                updated[key] = value
        path.write_text(json.dumps(updated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if mapping:
        MAPPING_FILE.write_text(
            json.dumps(
                {**json.loads(MAPPING_FILE.read_text(encoding="utf-8")), **mapping},
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        print(f"fixed {len(mapping)} invalid resource keys")
    return mapping


def main() -> int:
    cmd = sys.argv[1] if len(sys.argv) > 1 else "all"
    if cmd == "export":
        export_from_android()
        return 0
    if cmd == "migrate":
        mapping = migrate_keys()
        print(f"mapping entries: {len(mapping)}")
        return 0
    if cmd == "generate":
        regenerate_android()
        return 0
    if cmd == "codemod":
        mapping = json.loads(MAPPING_FILE.read_text(encoding="utf-8"))
        n = codemod_kotlin(mapping)
        print(f"updated {n} kotlin files")
        return 0

    mapping = migrate_keys()
    key_fixes = fix_invalid_keys()
    regenerate_android()
    combined = {**mapping, **key_fixes}
    n = codemod_kotlin({k: v for k, v in combined.items() if k != v})
    print(f"updated {n} kotlin files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
