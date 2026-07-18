# Desktop video playback

The desktop feed uses Compose `VerticalPager` and one screen-scoped
`CallbackMediaPlayerComponent`. The native LibVLC player is reused when the settled page changes,
so pages retained by the pager do not allocate their own decoders.

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

- mouse wheel over the video: previous or next page;
- `Previous` and `Next`: explicit page navigation;
- click the metadata panel or use `Play`/`Pause`: toggle playback;
- `Mute`/`Unmute`: update both shared feed state and the native player.

Playback pauses while the pager is moving. The settled page starts automatically, resumes its last
saved position, and loops after reaching the end.

## Packaging and license

The Maven dependency does not bundle LibVLC. A packaged application must either document a system
VLC requirement or bundle the correct native runtime for each OS and CPU architecture.

vlcj is available under GPL-3.0-or-later or a commercial license. Review the intended distribution
model before shipping a binary from this MIT-licensed laboratory project.
