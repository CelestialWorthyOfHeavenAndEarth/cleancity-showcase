# CleanCity motion design

Implemented 10 October 2026. The white and emerald palette is retained.

## Opening and Home

The welcome screen introduces the app with a large headline, a translucent neighborhood video and a pinned Get started action. The supplied video loops silently, with a pause control and a still fallback. It stops in the background, behind forms and after entry. Video preparation never holds up entry. The Home illustration retains its one-time vehicle arrival.

Home uses a time-of-day greeting and the household's full resident name, including **Mahesh Babu**. The AI Studio follow-up places Household QR inside the pickup panel, followed by Report and Bills. Scores and streaks follow.

First-entry content rises 14dp with a short fade. The first four groups are offset by 40ms. Entry state is saved so returning to a destination does not replay the introduction.

## Interaction rules

| Interaction | Behavior |
| --- | --- |
| Primary and outlined buttons, chips, household rows | Spring press scale to 0.98; release settles without a large bounce |
| Primary button state | Corners change from 16dp to 20dp while pressed and 28dp while saving; Working label accompanies progress |
| Citizen and staff tabs | Small directional movement with a fade; page state remains saved |
| Role and sign-in changes | Fade-through content transition |
| Report and collection details | Shared bounds connect the originating record to an inline detail page |
| Predictive Back from detail | Gesture progress reveals the list; cancellation restores the detail and its draft |
| Collection progress | A critically damped spring follows the saved value |
| Score, streak and route totals | Updated numbers interpolate briefly; initial values appear immediately |
| Successful action | Checkmark drawing and semantic confirmation feedback |
| Sorting guidance | Arrow rotates as guidance expands |

Springs govern physical movement and bounds. Opacity and short number changes use eased durations. The welcome video is the single intentional decorative loop and can be paused. There is no perpetual illustration travel, decorative shimmer or forced loading delay.

Details use the same composition as their lists because shared bounds cannot connect separate Android dialog windows. Other forms continue to use Material sheets and their platform motion. The app opts into Android's Back callback integration; custom gesture progress applies to report and household details. Other Back destinations retain their existing navigation behavior.

## Haptics and accessibility

Selection feedback accompanies deliberate chip changes and saved PPE updates. Confirmation feedback follows successful record mutations, a matched QR, route completion and recorded acknowledgments. Failed saves do not receive success feedback.

Feedback uses Android's semantic haptic constants through the view, without forcing vibration or overriding the user's touch-feedback preference. Actual strength depends on the device. Motion observes the system animator duration setting; disabled animations show the final state immediately. Platform Compose animations also follow the system duration scale.

Controls retain minimum touch sizes, scrolling forms, keyboard insets and localized labels. Newly introduced copy is supplied in all nine app languages.

## Verification boundaries

`MotionInteractionTest` exercises full-name rendering and shortcuts, a cancelled and completed predictive Back sequence, preserved detail drafts and list position, success-only feedback dispatch, and disabled-animation behavior. WelcomeVideoTest checks muted looping, release on lifecycle changes, delayed preparation after disposal, error fallback, reduced-motion behavior, pause controls and immediate entry. Existing data, QR, migration and screen tests remain part of verification.

Static host screenshots use the reduced-motion path because a software bitmap canvas cannot draw Compose shared-transition RenderNodes. These screenshots assess settled layout, not animation smoothness. Gesture feel, hardware haptic strength, actual frame pacing and physical-device rendering still require simulator/device review. Current test counts and APK identity are recorded in [UI_CHANGES.md](UI_CHANGES.md).

## Implementation references

- [Compose animation customization](https://developer.android.com/develop/ui/compose/animation/customize)
- [Compose shared elements](https://developer.android.com/develop/ui/compose/animation/shared-elements)
- [Predictive Back progress](https://developer.android.com/develop/ui/compose/system/predictive-back-progress)
- [Android haptic principles](https://developer.android.com/develop/ui/views/haptics/haptics-principles)
