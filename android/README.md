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
- **Phase 3**: the minigame is now a faithful port of the firmware's real
  physics (`PokeballGame.kt`, mirroring `TamaPoke.ino`'s `respawnBall()`/
  `gameTap()`/`stepGame()`): juggle a Pokeball in a circular arena against
  gravity, with wall bounce and tap-impulse, 3 misses ends the round, same
  85ms step cadence as the device. Training bag stays a tap-count challenge
  (matches `trainStrength()`'s "~4 hits = +1 STRENGTH" curve; the original's
  version is also just a tap counter, so no simplification there).
  **Real animated PMD sprites** are wired up for all 151 species, normal
  *and* shiny: `tools/pack_pmd_android.py` fetches the same
  `PMDCollab/SpriteCollab` sheets the firmware's `tools/pack_pmd.py` uses
  (shiny at `sprite/<dex>/0000/0001/...`, same layout as normal), but
  (unlike the firmware) keeps the original PNG as-is and stores
  frame-rect/timing metadata in a sidecar `.json` instead of re-encoding
  into the TPK2 binary format - Android crops frames at render time
  (`AnimatedSprite.kt`, `Canvas.drawImage` with `srcOffset`/`srcSize`).
  Bundled: all 151 species × Idle/Walk-L/Walk-R/Sleep/Eat/Hurt/Attack/Pose
  (where the source sheet has that animation - not every species has all
  eight), both variants, ~26MB total (`assets/sprites/<dex>/<action>.png`
  for normal, `assets/sprites/<dex>/shiny/<action>.png` for shiny).
  `SpriteLoader.load(..., shiny = true)` falls back to the normal variant
  if a species has no shiny sheet at all.
- **Phase 4**: `ChiptuneSynth`/`SfxPlayer` port audio.cpp's square-wave SFX
  tables to `AudioTrack` 1:1, wired to feed/pet/evolve/hatch/level-up/medal/
  ceremony events. `tools/gen_android_strings.py` (mirrored by the Node
  version used to bootstrap this repo) parses `i18n.h`/`i18n.cpp`'s 6-language
  table into `res/values{,-es,-fr,-de,-it,-pt}/strings_original.xml`
  (`orig_*` keys); language switching goes through
  `AppCompatDelegate.setApplicationLocales`. All screens that display
  original-firmware text now read from these keys (main screen, Pokedex,
  stat card, minigame/training-bag, ceremony dialog, starter picker, and the
  home-screen widget via `context.getString`); app-only UI chrome that has
  no firmware equivalent (Feed/Play/Bath/Sleep button labels, tab names,
  Settings category labels, "Sleeping" mood, medal total) is also fully
  translated in `res/values{,-es,-fr,-de,-it,-pt}/strings.xml` - all 6
  languages now cover every string the app displays, including the
  training bag's "STR +N" feedback (shown as a Snackbar, matching the
  firmware's on-screen confirmation).
- **Phase 5 (visual, in progress)**: the app started out as generic
  Material3 chrome, which looks nothing like the device's round 466x466
  AMOLED UI. First step: `ui/device/RoundDeviceFrame.kt` clips the main
  screen into a circular bezel, and `ui/device/BiomeBackground.kt` renders
  the sky/ground using the *exact* hex colors from `TamaPoke.ino`'s
  `drawScene()`/`BIOME_SOIL` (time-of-day sky gradient + per-species biome
  ground color, night-tinted). `ui/device/ArcButtonBar.kt` replaces the
  text Feed/Play/Bath/Sleep buttons with icon-only round buttons near the
  bottom of the circle (a straight row rather than a literal curved arc -
  good enough at this size, real trig placement would be the next
  refinement). Not yet done: swipe-based navigation (currently a Material
  bottom nav bar instead of the original's swipe gestures), evolution/
  ceremony animations, and applying the round frame to the other screens.
