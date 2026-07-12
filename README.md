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

## Universal Media Lab

The project integrates with `universal-media-lab` to simulate realistic, non-ideal network profiles (LTE, flaky connections) and server-side faults (TTFB delays, HTTP 500 status codes). 

The setup script clones the lab at a pinned commit into the ignored `.local/universal-media-lab` directory and applies Nginx route overlays.

### Quickstart with 20 Real Video Assets

1. **Bootstrap the Media Lab**:
   Run the following command to clone the lab, download 20 sample video files in parallel, transcode them into progressive MP4, HLS, and DASH streams, and start the Docker Compose stack:
   ```bash
   make media-lab-bootstrap
   ```

2. **Verify Setup**:
   Ensure that the gateway endpoints are serving all manifests and assets properly:
   ```bash
   make media-lab-smoke
   ```

3. **Shutdown**:
   To stop the container stack, run:
   ```bash
   make media-lab-down
   ```

### Root Makefile Commands Reference

All commands should be executed from the root of the KMP project:

| Command | Description |
|---|---|
| `make media-lab-setup` | Clones and applies the video-feed Nginx configurations overlay to the lab. |
| `make media-lab-download-videos` | Downloads 20 lightweight, high-quality sample videos in parallel to the lab inbox. |
| `make media-lab-bootstrap` | Downloads, transcodes (ingests) all 20 videos, starts Docker containers, and runs smoke tests. |
| `make media-lab-up` | Starts the Docker containers (Nginx gateway, origin, Toxiproxy, imgproxy, Wiremock). |
| `make media-lab-down` | Stops the active Docker containers. |
| `make media-lab-smoke` | Runs network connectivity integration checks against Toxiproxy and gateway. |
| `make media-lab-nginx-test` | Validates Nginx configurations inside the gateway. |

### Network Connectivity Guide

The Media Lab uses Toxiproxy to simulate network conditions (Toxiproxy operates on port `18080` for dynamic profiles, and `18081`-`18088` for stable networks). Below is how each client platform connects to these services:

#### A. Android Emulator (AVD)
* **Host Address**: The emulator maps the host machine loopback (`localhost`) to the IP address `10.0.2.2`.
* **Resolution**: The app is pre-configured to resolve server paths automatically to `http://10.0.2.2:<port>`.
* **HTTP (Cleartext)**: Cleartext HTTP traffic is enabled only for the Android `debug` source set. No additional configuration is required.

#### B. Physical Android Device (over USB)
* **Host Address**: A physical device connected via USB cannot resolve `10.0.2.2`. It must use `127.0.0.1`.
* **Port Forwarding**: You must reverse-forward Nginx and all Toxiproxy ports to your host machine via ADB:
  ```bash
  # Forward main gateway and dynamic Toxiproxy ports
  adb reverse tcp:8080 tcp:8080
  adb reverse tcp:18080 tcp:18080
  
  # Forward stable network ports (18081: clean, 18083: LTE, 18087: flaky, etc.)
  for port in {18081..18088}; do adb reverse tcp:$port tcp:$port; done
  ```
  Once run, the app on the physical device will connect to `127.0.0.1:18080` and load the video feeds successfully.

#### C. Desktop Target (JVM)
* **Host Address**: Runs natively on the host machine. Resolves directly to `127.0.0.1` (localhost).
* **Port Forwarding**: No port forwarding is required.

### Video Catalog and Media Contracts

The app's video catalog is managed dynamically by [DemoVideoCatalog.kt](file:///Users/dev/Developer/@PortfolioProjects/video-feed-lab/shared/src/commonMain/kotlin/com/nikkiw/videofeedlab/shared/catalog/DemoVideoCatalog.kt). It loops over the 20 downloaded assets and maps them into the video feed, cycling through different streaming protocols (HLS, DASH, Progressive MP4) and network profiles (Clean, LTE, Flaky) to simulate real-world production conditions.

The gateway exposes the following project contracts:
* `GET /vfl/api/feed` (Returns the JSON feed catalog)
* `GET /vfl/media/<asset-id>/progressive` (Standard MP4 stream)
* `GET /vfl/media/<asset-id>/hls/master.m3u8` (HLS master playlist)
* `GET /vfl/media/<asset-id>/dash/manifest.mpd` (DASH manifest)
* `GET /vfl/poster/<asset-id>/feed.webp` (Poster image, 720×1280)
* `GET /vfl/poster/<asset-id>/thumbnail.webp` (Thumbnail image, 360×640)
* `GET /vfl/fault/ttfb/<delayMs>/<asset-id>/hls/master.m3u8` (HLS stream delayed by 200/1000/3000 ms)

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
