# Localization

Edit these files **by hand**:

- `app/src/main/res/values/strings.xml` — English (default)
- `app/src/main/res/values-th/strings.xml` — Thai
- `app/src/main/res/values-zh-rCN/strings.xml` — Chinese (Simplified)

Use short, readable resource names — same idea as [LizuNemuri](https://github.com/yami-codes/LizuNemuri) (`cancel`, `retry`, `nav_search`), not hash keys like `str_cd712071`.

```xml
<string name="cancel">Cancel</string>
<string name="retry">Retry</string>
```

In Kotlin:

```kotlin
stringResource(R.string.cancel)
stringResource(R.string.retry)
```

Optional typed helpers live in `app/src/main/java/com/asmr/player/i18n/Strings.kt` — add entries there only when you want a shorter call site.

Placeholders use Android format: `%1$s`, `%2$d`.
