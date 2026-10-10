# CleanCity — Complete Visual Showcase Plan

## Goal and scope

Create a complete native Android visual showcase across Citizen, Collector, Driver, Supervisor, and Municipal Admin. Preserve Kotlin, Jetpack Compose, Material 3, ViewModel, Room, camera/gallery support, localization, and the optional Gemini integration. Authentication, payments, scanning, maps, reminders, and dispatch are clearly labeled demonstrations. No production backend, emulator setup, hosting, or AI Studio conversion is included.

## Art direction

- Warm civic premium: ivory #F6F4EC, forest #124734, leaf #207450, pale lime #DCEAA7, white surfaces.
- Matching dark theme: #111D18 background, #1D2B24 surfaces, #E9EFE7 text, #A2D5AA accent. Light default with Light/Dark/System preference.
- Bundled Plus Jakarta Sans, correct weights, system fallback for Indian scripts; 32/28sp display, 22sp headings, 16sp body, 13sp supporting text.
- 4dp spacing rhythm, 20dp phone margins, at least 48dp touch targets, 56dp primary actions, 12/20/28dp shapes.
- Custom local vector neighborhood, vehicle, and waste-container illustrations; coherent Material iconography and clearly labeled schematic maps.
- Shared app header, role/account selectors, navigation, buttons, inputs, status badges, metrics, progress, rows, sheets, dialogs, and loading/empty/error/success states.
- Restrained 150/220/300ms feedback/content/sheet motion; honor disabled system animations.

## Screen coverage

| Area | Intended result |
|---|---|
| Welcome/auth | Illustrated entry, prominent Explore demo, focused resident login/registration/staff forms, sample OTP autofill, validation. |
| Shell | Compact identity and ward header, role switcher, account settings for language/theme/sign-out, saved role/tab state. |
| Citizen home | Illustrated pickup hero, actual local pickup status, shortcuts, score/streak, announcements, branded demo household tag. |
| Reporting | New report/My reports views; photo, editable location, explicit sample location, categories, validation, retained drafts, success and detail timeline. |
| Sorting | Illustrated wet/dry/hazardous guidance and expandable examples. |
| Assistant | Localized prompts, readable conversation, loading, retry, keyboard-safe input, deterministic offline default. |
| Bills | Scoped household history, demo confirmation, processing/paid states, accurate receipt; View receipt label. |
| Collector | Daily route progress, dense stops, simulated scanner leading to household actions, collected/quality/mixed/not-collected flows, sample earnings. |
| Driver | Vehicle, schematic route, saved-session stop progress, validated weighbridge form/history, acknowledged demo SOS. |
| Supervisor | Overview/Reports/Crew; schematic map, SLA order, report details, explicitly sample inspection proof, resolve/escalate, PPE. |
| Admin | Overview/Wards/Finance/Fines; shared metrics and commissioner summary, reminder preview/acknowledgment, validated fine creation. |

## Connected functionality

1. Centralize the fresh default dataset around the existing Jubilee Hills citizen and Ward 104. Use relative fixture dates; preserve locations in existing records.
2. Seed once without overwriting edits during startup or login.
3. Observe the active household by ID from Room; scope citizen payments, pickups, and complaints to that household.
4. Connect collector results to citizen status, citizen reports to supervisor queue, resolution to citizen history, and paid bills to admin defaulters.
5. Expose operation states, prevent duplicate submissions, retain failed forms, and make payment/fee and pickup/score changes transactional.
6. Calculate daily distinct collection totals; distinguish exceptions; share dashboard/brief metrics.
7. Complete scanner selection, local reminder/SOS acknowledgments, and saved-session driver stops.
8. Use stable report categories, normalize PENDING to OPEN, and validate fields, finite amounts, and gross greater than tare weight.
9. Default to offline assistant/brief responses while retaining optional Gemini behind the same interface.
10. Preserve nine languages, localize new controls, respect Back hierarchy, drafts, and scroll state.

Add UI/operation state types, a central demo scenario, household report ownership and queries, and a non-destructive Room v2→v3 migration. Backfill ownership only with reliable matches; unmatched legacy complaints remain visible to staff. Persist existing Room data and appearance/language preferences. Driver route presentation and simulated notices use saved demo-session state.

## Sequence

1. Save this plan and record baseline.
2. Correct data initialization, ownership, reactive state, and operations.
3. Implement design tokens, reusable components, illustrations, and shell.
4. Complete citizen, collector/driver, then supervisor/admin screens.
5. Finish themes, languages, motion, accessibility, and responsive layouts.
6. Update handoff and actual verification results.

## Acceptance and verification

- Test ownership isolation, payment/collection idempotency, daily metrics, report lifecycle, migration, seeding, local assistant, and invalid forms.
- Provide major-screen previews in light/dark, 360dp phone, wide and enlarged-text variants, plus loading/empty/error states.
- Layout: one column below 600dp; paired summaries/wider forms from 600dp; two-pane operations from 840dp.
- Review system/IME insets, overlay scrolling, labels, contrast, 48dp targets, and wrapping.
- Use only already available build/test tooling; no emulator provisioning.
- Every visible action produces its intended local result; all roles use the shared identity; connected journeys work offline.
- Clearly distinguish static inspection, actual executed checks, and simulator/visual validation still outstanding.

## Inspected baseline (2026-10-09)

Native Compose app with eight screen source files, Room version 2, nine locale bundles, and existing unit/Robolectric tests. No Git repository is present in this checkout. Existing issues include repeated demo replacement, stale selected household objects, unscoped citizen data, incomplete scanner selection, no-op SMS/SOS actions, transient driver stops, hardcoded conflicting geography, simulated services presented as live, and light-only color aliases. Runtime rendering has not been inspected.

## Implementation handoff (2026-10-10)

The five-role showcase, shared visual system, connected local journeys, and Room version 3 migration are implemented. See [UI_CHANGES.md](UI_CHANGES.md) for the delivered screen coverage, executed checks, limitations, and simulator checklist, and [README.md](README.md) for the connected demo walkthrough and build instructions. This baseline section records the original audit; current verification is maintained in the handoff.

## Follow-up revision (2026-10-10)

The subsequent user request supersedes the simulated-scanner and repeated demonstration-badge requirements above. The app now uses real household QR encoding, camera/image decoding and sharing; a larger fictional TFI cast dataset; concise product wording; and searchable household switching. Room version 4 enriches prior installations without deleting saved records. Payment and communication controls describe their actual on-device effects. Current verification and the presentation walkthrough are maintained in UI_CHANGES.md and README.md.

## Palette revision — 10 October 2026

The user-approved white and emerald reference supersedes the original warm ivory/lime direction. Use emerald `#166844`, white surfaces, green text and subtle neutral-green dividers. Remove pale lime accents throughout both themes, controls, QR cards and illustrations; retain the established layout and features.

## Motion and Home revision — 10 October 2026

The approved follow-up adds physical press feedback, shared-bound report/collection details, predictive Back with preserved drafts and scroll position, meaningful progress and success animation, and restrained semantic haptics. Welcome and Home have been recomposed with a one-time neighborhood arrival, concise pickup information and direct shortcuts. Home shows the full resident name: Mahesh Babu. The reference palette is retained. Motion respects disabled system animations. See [MOTION_DESIGN.md](MOTION_DESIGN.md) for the implemented rules and [UI_CHANGES.md](UI_CHANGES.md) for actual verification.
