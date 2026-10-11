# CleanCity — Welcome video, motion and connected city workflows

Updated 11 October 2026. The latest revision increases welcome-video visibility and softens the overlay behind the heading and controls. The user's Google AI Studio update, connected workflows and full-name Home greeting remain incorporated.

## Interface changes

The identity uses white and emerald green, updated by the user in AI Studio (`#126B4D`). The palette revision removes the former ivory canvas and pale lime accents from both themes, navigation, buttons, QR cards and illustrations. Green hero panels have white text and an inset white Household QR action; Report and Bills use matching outlined shortcuts below. Material surface colors are explicitly set to avoid unrelated default tints. White text on the reference green has a 6.49:1 contrast ratio; body and supporting text on white exceed 4.5:1. The launcher and printable tag sheet use the same palette. Dark appearance uses corresponding green and neutral surfaces. The revised welcome and Home compositions preserve the existing workflows.

The normal phone header keeps brand, ward, role and preferences in one row; enlarged text moves the role control to another row. Household and crew lists use initial avatars and divided records. The citizen hero contains the household QR action, with Report and Bills below. Collection history includes a seven-day status strip.

Repeated Demo, Simulate and Local showcase wording has been removed from product surfaces. Actions use plain names: Get started, Scan household QR, Record payment, Payment reminder and Report vehicle issue. The payment confirmation explains its actual ledger effect. Reminders can be copied; vehicle issues appear in the supervisor queue. Neither action claims external delivery.

Preferences includes a searchable household chooser, so a presentation can move between Mahesh Babu, Prabhas, Allu Arjun, Jr NTR and the rest of the cast. Citizen navigation and drafts are saved separately for each household. Collector lists have name/address search and remaining/completed filters; supervisor reports have status filters; finance has separate dues and transaction views.

## Motion and Home update

Home displays **Mahesh Babu** in full. The pickup panel contains Household QR, with Report and Bills below and animated impact values following. Enlarged text uses scrolling navigation to avoid broken labels. The weekly strip uses localized narrow weekday labels.

Welcome has a strong headline, a translucent video background and a pinned Get started action. Saved entrance state prevents repeated text introductions on returning to a destination.

Shared controls have spring press feedback and button corner changes. Tabs use small directional transitions; role and entry changes fade through. Progress, totals, state labels and success checkmarks animate when their underlying state changes. Report and collection records expand into inline detail pages using shared bounds. Predictive Back reveals the list, restores a cancelled detail with its draft, and returns to the saved scroll position when completed. Forms and overlays keep the existing Back order.

Android semantic haptics accompany deliberate selections, QR matches and successfully saved operations. Failure does not produce success feedback. System touch-feedback preferences and disabled animations are respected. All new copy is translated into the nine existing languages. [MOTION_DESIGN.md](MOTION_DESIGN.md) records the interaction rules and scope.

## AI Studio sync and welcome video

Pulled commit `7e4a131` without conflicts. Its QR-in-hero layout, deep emerald color, scrolling chip rows, crew cards and launcher changes are retained.

The supplied `Video-25080.mp4` is now a bundled, silent welcome background behind a theme-aware gradient. The middle now reveals 65% of the footage, up from 42%, with less canvas coverage behind the heading and controls. Text and the primary button remain opaque. The derived 720 × 1280 H.264 clip runs for 8.58 seconds and uses a 0.75-second blend at the loop boundary. It is 2.2 MB, with a matching still image. The original file remains unchanged locally.

A pause/play control sits above Get started. Reduced motion, previews, loading and decoding errors use the still image. The native player releases immediately when the activity pauses, and also releases when the user opens forms, changes language through the sheet, enters the app or leaves the welcome screen. Late preparation callbacks cannot restart a disposed player. The clip needs no network service and entry does not wait for video preparation. Playback and pause labels are localized in all nine languages. Asset provenance and implementation details are in [docs/WELCOME_VIDEO.md](docs/WELCOME_VIDEO.md).

## Expanded connected dataset

| Records on a fresh installation | Count / coverage |
| --- | --- |
| TFI household cast | 24 households, three wards |
| Pickup history | 350 records across 15 days |
| Payments | 128 records across six months |
| Reports | 30, including Open, In progress, Escalated and Resolved |
| Crew | 12, including attendance and PPE states |
| Weighbridge | 18 records across three vehicles |
| Fines | Nine records for invented establishments |
| Announcements | Six community, collection and sorting updates |
| Driver route | Eight stops |

`CityFixtures` is the shared source for the installed database and preview data. Actor names are fictional presentation characters here; records and locations do not make claims about those real people. Fine records use invented business names. Dates are relative to initialization. The main household has fourteen consecutive successful historical pickups and remains ready for a collection today.

Room version 4 adds a seed marker and a non-destructive migration from version 3. The earlier version 2 migration remains supported. Existing scores, addresses, fees, payments and collection results are preserved. Recognized built-in names are updated, missing presentation records are added once, and subsequent launches do not repeat enrichment. Old generated receipt labels are normalized without changing payment amounts.

## Working QR features

- Actual black/white QR matrices encode each household's stable ID, with a quiet zone and error correction.
- The citizen tag and collector household sheet display the same generated code.
- Share QR tag exports a PNG using a narrowly scoped FileProvider cache path.
- Scan household QR opens a real camera scanner using ZXing Android Embedded.
- Read QR from image decodes a selected image on a background thread.
- A manual household-ID field provides a camera-independent fallback. Unknown or malformed codes produce a visible error; they do not silently choose a household.
- [Printable QR sheet](docs/household-tags/index.html) and 24 matching PNGs are included for presentation use.

The QR tests verify every cast household by encoding and decoding its tag. No external recognition service is used. Camera permissions, focus and scanning from actual hardware remain device checks.

## Other completed interactions

Supervisor resolution accepts an entered inspection note or a selected image; there is no fabricated proof button. Resolution history displays the stored note or image. Receipt titles and references describe saved payment records. The admin reminder action copies the visible text to the clipboard. The fuller collection and payment histories remain scoped to the selected citizen while staff views cover the operational dataset.

The existing nine languages, light/dark/system preferences, transactional payment/collection protections, offline assistant, shared commissioner metrics, scrollable sheets and responsive layouts remain in place. New control wording is provided in all nine languages.

## Verification

The debug APK builds successfully. All 41 local tests passed for the video integration before the middle-opacity adjustment, covering household isolation, transactional payment and collection behavior, input validation, report resolution, version 2 and version 3 database upgrades, preservation of saved edits, one-time data enrichment, offline responses and real QR encoding/decoding. Every one of the 24 household QR tags round-trips to its own identifier; malformed IDs and non-QR images are rejected. Four motion interaction tests verify the full-name Home and shortcuts, predictive Back cancellation/completion with preserved state, success-only haptic dispatch, and immediate rendering when system animations are disabled. Five video tests cover muted looping, immediate lifecycle release, late callbacks after disposal, decoder-error fallback, motion/overlay gating, manual pause/play and immediate entry.

For the 11 October overlay adjustment to 65% video visibility, the APK was rebuilt and the two existing `majorScreensRenderInBothThemes` and `largeTextOperationalScreensRender` tests passed, refreshing 27 captures. The welcome captures in light, dark and enlarged text were visually inspected. No playback behavior changed; the full 41-test suite was not repeated for this styling-only change.

The full-suite verification command uses the installed JDK 22 and Android SDK 36.1:

```powershell
.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain
```

The focused command for the latest overlay adjustment was:

```powershell
.\gradlew.bat --offline :app:testDebugUnitTest --tests 'com.example.ShowcaseRenderTest.majorScreensRenderInBothThemes' --tests 'com.example.ShowcaseRenderTest.largeTextOperationalScreensRender' :app:assembleDebug --console=plain
```

Thirty-nine host-rendered Compose images cover light/dark phone screens, enlarged text, wide layouts, QR cards, supervisor reports/crew, admin wards/finance/transactions, and the new report/collection detail pages. Representative renders were visually inspected, including welcome, Home in both themes, large-text Home, wide Home and both detail forms. Static screenshots use the reduced-motion path because a software bitmap canvas cannot draw shared-transition RenderNodes; welcome captures therefore show the actual poster and gradient. Separate interaction tests exercise motion, Back behavior and the video lifecycle using a shadow player. Every frame of the bundled video was decoded with FFmpeg, and the video embedded in the APK matches the prepared asset byte for byte.

These are Robolectric host renders, not screenshots from an installed APK or emulator. Scanner entry, invalid/valid household-ID lookup, the collection form and weight validation are checked through Compose interactions. Dialog-window appearance, physical haptic strength, gesture feel, hardware video playback, loop continuity and animation frame pacing remain device checks.

The existing tooling can emit a non-fatal KSP shutdown exception. Lint was previously blocked by uncached instrumentation-test dependencies and is separate from these build/unit/render checks.

The local handoff package is `CleanCity-Showcase-debug.apk` (27,900,492 bytes), copied from the final successful debug build. SHA-256: `FA59FC5A3F6C37B9662EF42B6F1D09AADD6174CD795E75B8AF02E8E522F380BF`. The APK is excluded from Git; source, documentation and the printable QR assets are committed.

## Simulator/device checklist

1. Scan a printed QR or the included tag sheet from another screen; try low light, denied camera permission, cancellation and an unknown tag.
2. Import a tag PNG and verify that the matching household opens. Share a tag and confirm the receiving app can read it.
3. Record a collection, report, inspection result and payment, then switch roles and households to verify the connected results.
4. Test keyboard insets, Back behavior, rotation, large text, TalkBack, language changes and dark/system appearance.
5. Open report and collection details, swipe Back partway and cancel, then complete Back. Verify drafts, scroll position, touch feedback and reduced-motion settings.
6. On welcome, check video playback, pause/play, reduced motion, light/dark appearance, form opening, background/resume and entry. Confirm that no audio plays.
7. Upgrade an existing installation and confirm saved work remains intact.

No emulator or Google AI Studio environment was set up. Network authentication, payment processing, SMS transport and external incident dispatch remain outside this app's current on-device workflows. The optional Gemini network path is retained but was not verified.
