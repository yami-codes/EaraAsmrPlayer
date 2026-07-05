#!/usr/bin/env python3
"""One-time helper: rename ugly auto-generated string keys to short readable names.

After this, edit app/src/main/res/values*/strings.xml by hand — no JSON/codegen.
"""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALES = {
    "values": ROOT / "app/src/main/res/values/strings.xml",
    "values-th": ROOT / "app/src/main/res/values-th/strings.xml",
    "values-zh-rCN": ROOT / "app/src/main/res/values-zh-rCN/strings.xml",
}
KT_ROOT = ROOT / "app/src/main/java"

# Curated short names — readable in code, like LizuNemuri's cancel / retry / settings.
CURATED: dict[str, str] = {
    "loudness_leveling_pulls_overall_loudness_toward": "loudness_leveling_help",
    "speaker_headphones_bluetooth_output_disconnected": "output_disconnect_pause_hint",
    "cloud_sync_candidates_possible_matches_tap": "cloud_sync_candidates",
    "already_exists_locally_not_saved_again": "already_exists_local",
    "directory_does_not_exist_was_removed": "directory_removed",
    "invert_swaps_left_right_channels_clone": "channel_mode_help",
    "each_slider_corresponds_center_frequency_band": "eq_band_help",
    "when_volume_exceeds_value_will_reduced": "loudness_threshold_help",
    "online_playback_not_currently_supported_audio": "online_playback_unsupported",
    "directory_already_included_existing_scan_directo": "directory_already_scanned",
    "rescan_failed_check_directory_try_again": "rescan_failed_hint",
    "search_space_separated_school_uniform_means_all": "search_and_hint",
    "failed_get_directory_tree_dlsite_play": "dlsite_tree_failed",
    "failed_load_online_resources_try_again": "online_resources_failed",
    "exact_phrase_search_wrapped_english_double": "search_exact_hint",
    "higher_values_play_faster_affects_tempo": "playback_speed_help",
    "pause_playback_when_another_music_video": "audio_focus_pause_hint",
    "enter_new_list_name_2": "enter_list_name_prompt",
    "used_background_playback_controls_lock_screen": "media_notification_hint",
    "main_process_completed_items_remain_confirmed": "sync_items_remain",
    "farther_distance_softer_slightly_more_attenuated": "reverb_distance_help",
    "access_restricted_risk_control_was_triggered": "access_risk_control",
    "access_currently_restricted_try_again_later": "access_restricted",
    "access_restricted_try_again_later": "access_denied_retry",
    "allow_eara_install_apps_unknown_sources": "allow_unknown_sources",
    "downloaded_file_invalid_download_again": "invalid_apk_redownload",
    "text_2": "file_type_text",
    "text_3": "search_quote_suffix",
    "album_2": "album_label",
    "min_2": "duration_minutes",
    "selected_2": "selected_items",
    "selected_3": "selected_count",
    "audio_tracks_2": "track_count_fmt",
    "blocked_keywords_2": "blocked_keywords_label",
    "circles_2": "circles_count_fmt",
    "clear_search_history_2": "clear_search_history_confirm",
    "cloud_sync_2": "cloud_sync_fmt",
    "cloud_sync_canceled_2": "cloud_sync_canceled_fmt",
    "collapse_2": "collapse_label",
    "current_version_2": "current_version_fmt",
    "delete_2": "delete_item_fmt",
    "download_failed_2": "download_failed_fmt",
    "downloading_2": "downloading_fmt",
    "enter_new_group_name_2": "enter_group_name_prompt",
    "korean_2": "language_korean_short",
    "local_sync_2": "local_sync_fmt",
    "online_2": "online_label",
    "playback_mode_2": "playback_mode_label",
    "remove_2": "remove_from_group_fmt",
    "remove_album_2": "remove_album_fmt",
    "rescan_failed_2": "rescan_failed_fmt",
    "simplified_2": "language_simplified_short",
    "simplified_chinese_2": "language_zh_cn",
    "simplified_chinese_3": "language_zh_cn_alt",
    "traditional_chinese_2": "language_zh_tw",
    "traditional_chinese_3": "language_zh_tw_alt",
    "sync_error_2": "sync_error_fmt",
    "tags_2": "tags_count_fmt",
    "untitled_2": "untitled_label",
    "voice_actor_2": "voice_actor_label",
    "only_local_library_albums_support_manual_2": "manual_rj_bind_hint",
}


def parse_xml(path: Path) -> dict[str, str]:
    root = ET.parse(path).getroot()
    return {c.attrib["name"]: c.text or "" for c in root.findall("string")}


def short_key_from_english(key: str, english: str, taken: set[str]) -> str:
    if key in CURATED:
        candidate = CURATED[key]
        if candidate not in taken:
            return candidate

    text = re.sub(r"%\d+\$[sd]", "", english)
    text = re.sub(r"%%", "", text)
    text = re.sub(r"\\\"", "", text)
    text = re.sub(r"[^\w\s]", " ", text)
    words = [w.lower() for w in text.split() if len(w) > 2 and w.lower() not in {
        "the", "and", "for", "please", "will", "this", "that", "when", "from", "with", "your",
    }]
    if not words:
        words = ["text"]
    candidate = "_".join(words[:3])
    if len(candidate) > 28:
        candidate = "_".join(words[:2])
    candidate = re.sub(r"_+", "_", candidate).strip("_")
    if re.match(r"^\d", candidate):
        candidate = f"key_{candidate}"
    base = candidate
    n = 2
    while candidate in taken and candidate != key:
        candidate = f"{base}_{n}"
        n += 1
    return candidate


def build_rename_map(strings: dict[str, str]) -> dict[str, str]:
    taken = set(strings.keys())
    mapping: dict[str, str] = {}
    for key in sorted(strings.keys()):
        needs = (
            key.startswith("str_")
            or re.search(r"_\d+$", key)
            or len(key) > 28
            or key in CURATED
        )
        if not needs:
            mapping[key] = key
            continue
        new_key = short_key_from_english(key, strings[key], taken)
        if new_key != key:
            taken.discard(key)
            if new_key in taken:
                base = new_key
                n = 2
                while new_key in taken:
                    new_key = f"{base}_{n}"
                    n += 1
            taken.add(new_key)
        mapping[key] = new_key
    return mapping


def write_xml(path: Path, strings: dict[str, str]) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for key in sorted(strings.keys()):
        value = strings[key]
        attrs = ""
        if "%" in value and "%%" not in value.replace("%%", ""):
            attrs = ' formatted="false"'
        esc = (
            value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace('"', '\\"')
        )
        lines.append(f'    <string name="{key}"{attrs}>{esc}</string>')
    lines.append("</resources>\n")
    path.write_text("\n".join(lines), encoding="utf-8")


def codemod_kotlin(mapping: dict[str, str]) -> int:
    changes = {o: n for o, n in mapping.items() if o != n}
    count = 0
    for path in KT_ROOT.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        original = text
        for old, new in sorted(changes.items(), key=lambda x: -len(x[0])):
            text = text.replace(f"R.string.{old}", f"R.string.{new}")
        if text != original:
            path.write_text(text, encoding="utf-8")
            count += 1
    return count


def main() -> None:
    en = parse_xml(LOCALES["values"])
    mapping = build_rename_map(en)
    renamed = sum(1 for o, n in mapping.items() if o != n)
    print(f"renaming {renamed} keys")

    for dir_name, path in LOCALES.items():
        data = parse_xml(path)
        remapped = {mapping.get(k, k): v for k, v in data.items()}
        write_xml(path, remapped)
        print(f"updated {path.relative_to(ROOT)}")

    n = codemod_kotlin(mapping)
    print(f"updated {n} kotlin files")


if __name__ == "__main__":
    main()
