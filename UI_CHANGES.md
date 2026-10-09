# CleanCity showcase implementation handoff

Updated 10 October 2026. Scope: all five roles in a native, offline-first Android demonstration. The accepted plan is in [CLEANCITY_SHOWCASE_PLAN.md](CLEANCITY_SHOWCASE_PLAN.md).

## Delivered design system

- Semantic light/dark Material 3 colors replace the old Mobbin/Dark aliases. The light identity combines warm ivory, forest green, white surfaces and restrained lime; dark surfaces use deep greens with warm text.
- Shared page headings, panels, divided records, metrics, choices, forms, navigation, status badges, sheets, snackbars, and interface states bring the same visual language to every role.
- Locally drawn Compose vector scenes provide neighborhood, collection vehicle, sorting bins, and schematic route illustrations without network assets.
- Typography uses the bundled Plus Jakarta Sans. Inspection found a static regular face; misleading duplicate weight registrations were removed, allowing Android's normal synthesized weights and script fallback. Display text is 32/28sp with readable body and supporting text.
- Phone content has 20dp margins, primary controls have a 56dp minimum height, selectable controls have 48dp minimum targets, and sheets can scroll above the keyboard. Spacious compositions are reserved for key summaries; operational records use denser rows.
- Wide operational summaries switch to paired panes at the equivalent of 840dp window width. Phone pages use a single column. Enlarged-text tabs use horizontal scrolling to preserve usable labels.
- Role and destination transitions use 220ms Compose motion, and progress changes animate without decorative loops. Compose/Material motion follows the platform animation duration scale.

## Completed screens

| Area | Result |
| --- | --- |
| Welcome and entry | Illustrated Explore demo entry; citizen login, registration and staff forms; visible sample OTP, autofill, validation and submission feedback. |
| Shared shell | Brand and ward context, visible role switching, language and appearance preferences, sign-out, saved role tabs, and Back navigation. |
| Citizen Home | Illustrated sample pickup schedule, household tag sheet, report/bill shortcuts, actual collection status, score/streak, announcements and history. |
| Report | New report/My reports, camera/gallery hooks, editable address, explicit sample location, optional GPS, stable categories, filters and detail timeline. Failed submissions retain drafts. |
| Sort and Assistant | Illustrated sorting categories with expandable guidance; localized prompt chips, selectable messages, loading/retry feedback and keyboard-safe composer. |
| Bills | Household-scoped records, demo payment review and processing state, paid state, and View receipt sheet. |
| Collector | Daily route/exception metrics, household stops, simulated tag selection opening the selected household, quality choices and missed-pickup reasons, plus labeled sample earnings. |
| Driver | Vehicle summary, schematic route, session-persistent stop completion, validated weighbridge form/history, and visible local SOS acknowledgment. |
| Supervisor | Overview/Reports/Crew tabs, schematic map, SLA-ordered reports, inspection/resolution sheet with selected image or explicit sample proof, escalation feedback and PPE controls. |
| Municipal admin | Overview/Wards/Finance/Fines, shared actual metrics, ward comparisons, commissioner brief, reminder review/local acknowledgment, and validated fine creation. |

All nine existing languages remain available: English, Telugu, Hindi, Kannada, Tamil, Malayalam, Marathi, Bengali, and Gujarati. New interface controls and deterministic assistant/brief text have corresponding localized copy. Household names, addresses and record content remain user/sample data.

## Connected data and reliability

`DemoScenario` defines the Jubilee Hills / Ward 104 story, household, vehicle, sample OTP, and date helpers. Fresh records are dated relative to initialization. Seeding is idempotent and preserves edits; an existing unrelated household with ID 1 is not overwritten.

The active household derives reactively from its selected ID and Room data. Citizen reports, receipts and pickup history are scoped to that ID. Staff retain their operational views. Collector records update household status and scores; supervisor resolution updates citizen history; payment transactions update fees and admin unpaid lists.

Payment-plus-fee and pickup-plus-score writes are transactional. Duplicate payment/collection actions cannot create extra monthly receipts or increase a day's score repeatedly. Collection metrics use distinct stops from today, with successful collections separated from exceptions. Admin summaries and the commissioner brief use the same metrics.

Shared operation states expose saving, success and error outcomes by action. Buttons prevent repeated submissions while busy, forms keep their contents on failure, and successful submissions clear the appropriate draft. Reports, fines and weights validate required fields, finite positive amounts, and gross weight greater than tare weight.

Room version 3 adds nullable report ownership. The non-destructive version 2 migration normalizes PENDING to OPEN and assigns ownership only for unambiguous normalized phone matches. Unmatched legacy reports remain visible to staff. Existing stored addresses are preserved.

## Verification actually performed

Final command on 10 October 2026:

```powershell
.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain
```

**BUILD SUCCESSFUL.** Kotlin compilation and debug APK packaging completed. All **26 tests passed**, with zero failures, errors or skipped tests. The test suites cover:

- Initialization without overwrites, including an unrelated ID-1 record.
- Household report isolation and report resolution lifecycle.
- Concurrent payment idempotency and repeated pickup/score protection.
- Distinct daily collection calculations and mixed/missed outcomes.
- Full Room version 2→3 schema migration with ambiguous ownership retained as null.
- Invalid report, fine and weighbridge rejection without writes.
- Offline replies for all four suggested prompts in all nine languages.
- Explicit sample fallback when location is unavailable.
- Compose scan selection and invalid weighbridge interaction checks.

`ShowcaseRenderTest` draws actual Compose views using Robolectric native graphics. It generates 29 nonblank host images: ten major screens in both themes, six large-text screens and three wide screens. These were visually reviewed during implementation, with follow-up layout changes for shortcut placement, text density and tab wrapping. This is useful host rendering evidence, not Android Studio preview execution or an installed APK's rendering.

The local debug APK was saved as `CleanCity-Showcase-debug.apk` (excluded from Git), copied from `app/build/outputs/apk/debug/app-debug.apk` (23,409,578 bytes). Application ID: `com.aistudio.cleancity.kxwzqa`, version 1.0, minimum Android API 24. SHA-256: `7D45BEA619FE9CAFA07DFCD39E23E25F8855BE38D87EDEA541F37DEF63633E17`. The old root `CleanCity.apk` is unchanged.

The build logged a non-fatal KSP tooling shutdown exception; Gradle completed successfully and the tests and packaging passed.

Lint could not complete offline because the available cache lacks `androidx.test.espresso:espresso-core:3.7.0` and `androidx.test:runner:1.6.2`. No dependencies were removed to hide that limitation.

## Remaining simulator review

No app was installed or launched in an emulator/device, and no environment was set up for Google AI Studio. Review these on the user's simulator/device:

1. Run the connected walkthrough in README, including switching roles after each write.
2. Verify system bars, gesture navigation, Back/overlay priority, keyboard resizing, scrolling sheets, rotation, and saved session restoration.
3. Exercise camera capture, gallery selection, granted/denied GPS permissions, real location availability, and photo proof display.
4. Review light/dark/system appearance, all language choices, Indian-script font fallback, TalkBack labels, large text, and small/wide windows.
5. Confirm the visual hierarchy, spacing, text wrapping and motion on actual device rendering.

The optional Gemini network path, real payment verification, authentication services, live scanning/tracking, SMS delivery and emergency dispatch are outside this verified local showcase. Release signing/distribution is not configured by this work.
