# TamaPoke Android

A from-scratch Android port of the pet-simulation game logic from the
[TamaPoke](../README.md) firmware (ESP32-S3 / Waveshare AMOLED board).
Built entirely via GitHub Actions (`.github/workflows/android-build.yml`) —
no local Android SDK install required.

## Modules

- `core-pet/` — pure Kotlin/JVM port of `pet.h`/`pet.cpp`. No Android
  dependency, so it runs as plain JUnit5 tests on any JDK 17+.
- `app/` — Compose UI, Room persistence, WorkManager background catch-up,
  and a Glance home-screen widget.

## Regenerating species data

`tools/dex_data.py` (repo root) is the single source of truth for species,
evolution and rarity data — it already feeds the firmware's `dex.h`. To
regenerate the Android copy (`app/src/main/assets/dex.json`) after editing
`dex_data.py`, run (requires Python 3):

```sh
python3 tools/gen_dex_json.py
```

## Building

CI (`.github/workflows/android-build.yml`) runs `:core-pet:test` then
`:app:assembleDebug` on every push touching `android/**`, and uploads the
resulting debug APK as a build artifact — sideload it directly, no signing
required.

To build locally (needs a JDK and, for the `:app` module specifically, an
Android SDK — `core-pet` alone only needs a JDK):

```sh
cd android
./gradlew :core-pet:test        # pure-JVM engine tests, no SDK needed
./gradlew :app:assembleDebug    # needs ANDROID_HOME / local.properties
```

**Windows path caveat:** Gradle's test worker fails to load compiled test
classes (`ClassNotFoundException`) when the project path contains an
apostrophe (e.g. this repo originally sat under `.../App's/TamaPoke`).
`:core-pet:test` will fail locally under such a path even though the code
and CI are fine — either rename/move the folder to drop the apostrophe, or
just trust CI, which checks out to a clean path.

## Status

- **Phase 1**: core game loop (`PetEngine`), Room persistence, offline
  catch-up, a minimal main screen, and the CI pipeline.
- **Phase 2**: starter picker, evolve/farewell/runaway decision flow
  (`declineEvolve`/`declineFarewell`), ceremony dialog, bottom-nav shell
  (Home/Pokedex/Stats/Settings), full 151-species Pokedex grid, and the
  4-page stat card (Profile/Battle/Medals/Progress).
- **Phase 3**: minigame and training-bag screens wired to
  `playResult()`/`trainStrength()` — simplified tap-based mechanics rather
  than a pixel-perfect port of the original's flappy-bird-style physics
  (documented in `MinigameScreen.kt`). Real PMD-sprite art is **not**
  included yet: the UI still uses placeholder shapes/initials — converting
  the ~40MB PMD SpriteCollab pack (`tools/pack_pmd.py`'s data source) into
  Android sprite sheets is a follow-up.
- **Phase 4**: `ChiptuneSynth`/`SfxPlayer` port audio.cpp's square-wave SFX
  tables to `AudioTrack` 1:1, wired to feed/pet/evolve/hatch/level-up/medal/
  ceremony events. `tools/gen_android_strings.py` (mirrored by the Node
  version used to bootstrap this repo) parses `i18n.h`/`i18n.cpp`'s 6-language
  table into `res/values{,-es,-fr,-de,-it,-pt}/strings_original.xml`
  (`orig_*` keys); language switching goes through
  `AppCompatDelegate.setApplicationLocales`. Only a representative subset of
  screens (main screen, stat card, ceremony dialog, starter picker) actually
  read from these keys so far — the rest of the catalog is generated and
  ready for the remaining screens to adopt.
