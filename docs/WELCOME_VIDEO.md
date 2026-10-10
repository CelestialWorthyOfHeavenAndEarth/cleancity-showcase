# Welcome background video

Updated 10 October 2026 after pulling the user's Google AI Studio commit `7e4a131`.

## Source and bundled assets

The source is the user-supplied `Video-25080.mp4`, described by the user as downloaded from Instagram. No creator name or source URL was supplied. It shows a sunlit residential street with trees and houses. The original file remains unchanged in the workspace and is excluded from Git.

| Asset | Details |
| --- | --- |
| Original | 720 × 1280, H.264, 24 fps, 9.4 seconds, AAC audio, 3,530,122 bytes |
| Bundled video | `app/src/main/res/raw/welcome_neighborhood.mp4`, 720 × 1280, H.264 Main 3.1, 24 fps, 8.58 seconds, no audio track, 2,199,278 bytes |
| Still fallback | `app/src/main/res/drawable-nodpi/welcome_neighborhood_poster.jpg`, first frame of the bundled video, 192,402 bytes |

The derived video starts 0.75 seconds into the source. Its final 0.75 seconds blend into the source's opening 0.75 seconds, softening the loop boundary. The frame is preserved; the app crops uniformly to fill its available background. The source's warm sunlight is retained.

Bundled video SHA-256: `007c03cbccfe45146c0ea28327a527689ab1ce36c3b9bd7202bd92bd156cc9f1`.

## Presentation and lifecycle

The welcome illustration is replaced by full-size footage behind a theme-aware gradient. Video visibility peaks at 28% in the middle; the headline and controls have stronger coverage. Text and buttons remain opaque. Dark appearance uses the same footage under the dark canvas color.

- The clip is bundled and requires no network request or media permission.
- Android `MediaPlayer` and `TextureView` handle local playback; no Android library was added.
- The still image remains behind the video, including during asynchronous preparation. The texture appears only after its first rendered frame.
- Both the file and player are silent. Playback loops without forcing a loading delay before Get started.
- Pause/play is available above the main action, with labels in all nine app languages. The pause choice survives configuration changes.
- Disabled system animations, editor previews and decode errors use the still image.
- Sign-in forms, language selection, successful entry and leaving the welcome page dispose of playback.
- A native lifecycle observer releases the player immediately on activity pause, even if Compose cannot produce another frame. Compose disposal and surface destruction also release media and surface resources safely.
- A callback from a previously released player cannot restart playback.

## Preparation

FFmpeg 7.1 was used from a workspace-local `imageio-ffmpeg` helper under the ignored `build/media-tools` folder. It is not an app or build dependency. Encoding used H.264, CRF 26, a 24 fps constant frame rate, fast-start metadata, no audio and no inherited metadata. The still is extracted from the first output frame.

The loop filter was:

```text
[0:v]fps=24,split=2[body][head];
[body]trim=start=0.75:end=9.333333,setpts=PTS-STARTPTS,fps=24[a];
[head]trim=start=0:end=0.75,setpts=PTS-STARTPTS,fps=24[b];
[a][b]xfade=transition=fade:duration=0.75:offset=7.833333,format=yuv420p[v]
```

## Verification boundaries

`WelcomeVideoTest` exercises silent looping, lifecycle release, delayed callbacks after release, error handling, reduced motion, overlay gating, pause/play and entry without waiting for playback. These are host tests with a shadow player, not hardware-decoder verification. Existing screenshot checks render the actual poster and gradient in light, dark and enlarged-text layouts. Current results and APK identity are in [UI_CHANGES.md](../UI_CHANGES.md).

Review actual video decoding, loop continuity, app background/resume and frame pacing on the user's simulator or device.

Implementation references: [MediaPlayer lifecycle](https://developer.android.com/reference/android/media/MediaPlayer), [TextureView transforms](https://developer.android.com/reference/android/view/TextureView), [Views in Compose](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose).
