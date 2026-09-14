# LLM subtitle translation (ported from LizuNemuri)

This feature translates **existing** subtitle files (LRC / VTT / SRT) using an OpenAI-compatible LLM. It does **not** perform speech-to-text transcription.

## LizuNemuri source modules

| LizuNemuri path | Eara path |
|-----------------|-----------|
| `lib/core/llm/subtitle_translation_service.dart` | `app/src/main/java/com/asmr/player/data/llm/SubtitleTranslationService.kt` |
| `lib/data/services/llm_client.dart` | `app/src/main/java/com/asmr/player/data/llm/LlmClient.kt` |
| `lib/core/llm/streaming_translation_parser.dart` | `app/src/main/java/com/asmr/player/data/llm/StreamingTranslationParser.kt` |
| `lib/core/llm/llm_batch_planner.dart` | `app/src/main/java/com/asmr/player/data/llm/LlmBatchPlanner.kt` |
| `lib/core/llm/subtitle_translation_cache.dart` | `app/src/main/java/com/asmr/player/data/llm/SubtitleTranslationCache.kt` |
| `lib/data/repositories/llm_api_key_repository.dart` | `app/src/main/java/com/asmr/player/data/llm/LlmApiKeyStore.kt` |
| `lib/core/settings/app_settings_service.dart` (LLM keys) | `SettingsDataStore.kt` / `SettingsRepository.kt` |
| `lib/screens/settings/llm_translation_settings_screen.dart` | `ui/settings/LlmTranslationSettingsScreen.kt` |
| `lib/presentation/viewmodels/player_viewmodel.dart` (translate flow) | `ui/player/LyricsViewModel.kt` |
| `lib/l10n/app_*.arb` + `lib/core/settings/app_language.dart` | `res/values*/strings.xml` + `i18n/AppLanguage.kt` |

## App language (i18n)

Eara already ships UI locales **English**, **Thai**, and **Chinese (Simplified)** via `strings.xml` and **Settings → Language**, matching LizuNemuri's `app_language` picker (`system` / `en` / `th` / `zh`).

## Configuration

No API keys are committed. Users enter a key in **Settings → AI subtitle translation → API & prompts**.

Default endpoint/model presets mirror LizuNemuri:

- OpenRouter (`https://openrouter.ai/api/v1`, Gemma free models)
- OpenAI (`https://api.openai.com/v1`)
- Gemini (`https://generativelanguage.googleapis.com/v1beta/openai`)

Translated lines are cached under app files: `files/translated_subtitles/{workId}/{hash}_{lang}.json`.

## How to test

### Language switching

1. Open **Settings → Language**.
2. Pick **Follow system**, **English**, **Thai**, or **Simplified Chinese**.
3. Confirm new LLM strings localize (section title, buttons, errors).

### AI subtitle translation

1. Open **Settings → AI subtitle translation**.
2. Enable **Auto-translate subtitles on playback** (optional).
3. Tap **API & prompts**, choose a provider preset, enter your API key, save.
4. Play a track that already has LRC/VTT/SRT subtitles (local sibling file, import, or embedded).
5. Open the lyrics surface (cover tap / lyrics mode).
6. Use the **⋮** menu → **Translate subtitles with LLM**.
7. Optional: set **Subtitle display → Dual** to show original + translation.
8. Use **Show original subtitles** to revert.

### Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.asmr.player.data.llm.*"
```
