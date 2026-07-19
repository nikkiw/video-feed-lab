# Desktop video playback

The desktop feed uses Compose `VerticalPager`, one screen-scoped `MediaPlayerFactory`, and two
reusable `EmbeddedMediaPlayer` slots. The active slot renders the current page while the standby
slot opens and buffers the target or adjacent page without allocating a player per feed item.

## Runtime requirement

Install VLC 3.x before running the desktop target. VLC and the JDK must use the same CPU
architecture. For example, an Apple Silicon JDK requires the Apple Silicon VLC build.

```bash
./gradlew :desktopApp:run
```

vlcj searches the normal platform locations for LibVLC. A custom native directory can be set in
`~/.config/vlcj/vlcj.config`:

```properties
nativeDirectory=/absolute/path/to/vlc/lib
```

If LibVLC cannot be loaded, the feed shows an explanatory state instead of terminating the
application.

## Interaction

- mouse wheel or trackpad over the video: continuously drag the feed, then snap to the adjacent page;
- `Previous` and `Next`: explicit page navigation;
- click the metadata panel or use `Play`/`Pause`: toggle playback;
- `Mute`/`Unmute`: update both shared feed state and the native player.

Playback pauses while the pager is moving. The native VLC surfaces stay mounted in a fixed layer;
the pager moves a Compose snapshot of the old frame and a poster for the target page instead of
moving `SwingPanel` instances. If frame capture is unavailable, the old page also falls back to its
poster. The shared animated loader remains visible on the settled target until its first playback
frame is presented. A previously visited page requests a time-based thumbnail at its saved position
and falls back to the regular poster when it is unavailable.

The pager's `targetPage` prepares the standby slot before settling. On settle, a prepared standby
is promoted without reopening its media URL; the old active slot is then reused to preload the next
page. The settled page resumes its saved position and loops after reaching the end.

Both slots use LibVLC's network/live/file caching options. This is an in-memory playback pipeline,
not a persistent disk cache.

The application enables Compose/Swing interop blending before creating its window so the Compose
poster and loading overlay can render above the LibVLC AWT surfaces.

## Packaging and license

The Maven dependency does not bundle LibVLC. A packaged application must either document a system
VLC requirement or bundle the correct native runtime for each OS and CPU architecture.

vlcj is available under GPL-3.0-or-later or a commercial license. Review the intended distribution
model before shipping a binary from this MIT-licensed laboratory project.

## Desktop source compatibility policy

LibVLC 3.x on desktop can restart the adaptive fMP4 demux between HLS/DASH
fragments (`Fragment sequence discontinuity` followed by `needrestart`). In the
feed this appears as playback starting normally, freezing after roughly one or
two seconds, and then continuing after the decoder is recreated.

For that reason Desktop uses **progressive MP4 compatibility mode by default**.
The host and network-profile port selected by the catalog are preserved, while
known `/vfl/media/<asset>/hls/master.m3u8` and
`/vfl/media/<asset>/dash/manifest.mpd` routes are rewritten to
`/vfl/media/<asset>/progressive`.

Android is unchanged and continues to exercise HLS/DASH through Media3.

To reproduce and diagnose the native LibVLC adaptive behavior explicitly:

```bash
VIDEO_FEED_LAB_DESKTOP_SOURCE_MODE=adaptive ./gradlew :desktopApp:run
```

Adaptive desktop mode uses the original catalog URL and disables the standby
player preload. A decoded first frame is not treated as proof that the next
adaptive fragment can continue without a demux restart.

In progressive mode the standby player starts only after the active player:

1. has presented a frame;
2. is not buffering;
3. has played continuously for at least 750 ms.

If active playback enters rebuffering while standby warmup is running, standby
is stopped and retried only after active playback becomes stable again. Active
uninterrupted playback always has priority over speculative preload.

