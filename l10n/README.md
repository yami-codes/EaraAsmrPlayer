# Eara localization sources

Edit these JSON files, then regenerate Android resources:

```bash
python3 tools/regenerate_l10n.py
```

This mirrors the [LizuNemuri](https://github.com/yami-codes/LizuNemuri) workflow (`lib/l10n/*.arb` + codegen), adapted for Android/Kotlin:

| LizuNemuri (Flutter) | Eara (Android) |
|----------------------|----------------|
| `lib/l10n/app_*.arb` | `l10n/strings_*.json` |
| `flutter gen-l10n` | `tools/regenerate_l10n.py` |
| `Strings.cancel` facade | `Strings.cancel()` in `app/.../i18n/Strings.kt` |

Locales: `en` (template), `th`, `zh-CN`.

Generated outputs (do not edit by hand):

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-th/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`
- `app/src/main/java/com/asmr/player/i18n/Strings.kt`
