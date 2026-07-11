# Video Feed Lab

Kotlin Multiplatform laboratory for experimenting with the exact video problems that appear in production short-form feeds: adaptive HLS/DASH playback, autoplay, preloading, startup latency, rebuffering and player lifecycle.

The project is based on the structure of `nikkiw/kmp-modular-template`: Android + Desktop entry points, KMP shared code, `feature/*/api|impl`, and Gradle convention plugins in `build-logic`.

## What is implemented

- KMP shared video model and public test catalog.
- Feature API/implementation split.
- Android short-video feed with `VerticalPager`.
- One shared `ExoPlayer` instead of one player per list item.
- Media3 `DefaultPreloadManager`.
- Tiered preload policy:
  - next item: 3 seconds;
  - previous item: 1.5 seconds;
  - ±2: tracks selected;
  - ±4: source prepared.
- Sliding preload window to keep memory bounded for a larger feed.
- HLS and DASH media items in the same feed.
- QoE debug overlay:
  - request → first frame startup time;
  - rebuffer count;
  - current bitrate;
  - current resolution;
  - playing state.
- Desktop target remains buildable and shows the catalog, while Media3 playback stays platform-specific.

## Architecture

```text
androidApp / desktopApp
        ↓
feature:video-feed:api
        ↑
feature:video-feed:impl
   commonMain
      └─ entry point / platform boundary
   androidMain
      ├─ VerticalPager
      ├─ AndroidPlaybackCoordinator
      ├─ DefaultPreloadManager
      ├─ ExoPlayer
      └─ QoE metrics
   desktopMain
      └─ diagnostic placeholder
        ↓
shared
   ├─ VideoItem
   ├─ PlaybackSource
   ├─ StreamType
   ├─ DemoVideoCatalog
   └─ PlaybackUrlProvider boundary
```

The important boundary is deliberate: `commonMain` does not know about `ExoPlayer`. Media3 is an Android playback engine, while the content model and future URL resolution logic remain shared.

## Run

Open the project in a recent Android Studio with JDK 17+.

This generated archive intentionally does not include the binary `gradle-wrapper.jar`. Copy the `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` files from the source template, or run `gradle wrapper` once with a local Gradle installation.

Then:

```bash
./gradlew :androidApp:installDebug
```

## Replace the demo catalog with real vertical 9:16 assets

For a realistic experiment, upload 5–10 vertical MP4 files to Mux (or another managed video platform), then replace the URLs in:

```text
shared/src/commonMain/.../DemoVideoCatalog.kt
```

The app itself still does not need a backend for public playback URLs.

For future signed URLs, keep the secret on a server and implement the existing `PlaybackUrlProvider` boundary. Never put a signing secret in the APK.

## Suggested experiment sequence

1. Establish baseline startup p50/p95 with preloading disabled.
2. Enable source preparation only.
3. Enable `TRACKS_SELECTED` for ±2.
4. Preload 1.5–3 seconds around the current item.
5. Compare memory, bandwidth and first-frame latency.
6. Test on constrained 4G and a low/mid-tier Android device.
7. Add disk cache only after measuring the memory-only preload baseline.

See `docs/EXPERIMENTS.md` for the next implementation steps.
