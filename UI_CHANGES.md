# CleanCity — TFI dataset and QR update

Updated 10 October 2026. This revision follows the request for memorable TFI names, substantially more data, real QR workflows and simpler product wording.

## Interface changes

The identity uses white and emerald green, sampled from the user's reference (`#166844`). The palette revision removes the former ivory canvas and pale lime accents from both themes, navigation, buttons, QR cards and illustrations. Green hero panels have white text and white QR buttons; Report and Bills use matching outlined controls. Material surface colors are explicitly set to avoid unrelated default tints. White text on the reference green has a 6.78:1 contrast ratio; body and supporting text on white exceed 4.5:1. The launcher and printable tag sheet use the same palette. Dark appearance uses corresponding green and neutral surfaces. The layout and all workflows are retained.

The normal phone header keeps brand, ward, role and preferences in one row; enlarged text moves the role control to another row. Household and crew lists use initial avatars and divided records. The illustrated citizen hero is shorter, bringing report and bill actions forward. Collection history includes a seven-day status strip.

Repeated Demo, Simulate and Local showcase wording has been removed from product surfaces. Actions use plain names: Get started, Scan household QR, Record payment, Payment reminder and Report vehicle issue. The payment confirmation explains its actual ledger effect. Reminders can be copied; vehicle issues appear in the supervisor queue. Neither action claims external delivery.

Preferences includes a searchable household chooser, so a presentation can move between Mahesh Babu, Prabhas, Allu Arjun, Jr NTR and the rest of the cast. Citizen navigation and drafts are saved separately for each household. Collector lists have name/address search and remaining/completed filters; supervisor reports have status filters; finance has separate dues and transaction views.

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

The debug APK builds successfully. All 32 local tests pass, covering household isolation, transactional payment and collection behavior, input validation, report resolution, version 2 and version 3 database upgrades, preservation of saved edits, one-time data enrichment, offline responses and real QR encoding/decoding. Every one of the 24 household QR tags round-trips to its own identifier; malformed IDs and non-QR images are rejected.

The verification command uses the installed JDK 22 and Android SDK 36.1:

```powershell
.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Thirty-six host-rendered Compose images cover light/dark phone screens, enlarged text, wide layouts, QR cards, supervisor reports/crew, and admin wards/finance/transactions. Representative renders were visually inspected, including the citizen home, QR card, collector, crew, report queue, finances and enlarged admin layout. They are rendered with Robolectric native graphics, not an installed APK or emulator. Scanner entry, invalid/valid household-ID lookup, the collection form and weight validation are checked through Compose interactions. Dialog-window appearance remains a device check because the host screenshot helper captures the activity view only.

The existing tooling can emit a non-fatal KSP shutdown exception. Lint was previously blocked by uncached instrumentation-test dependencies and is separate from these build/unit/render checks.

The local handoff package is `CleanCity-Showcase-debug.apk` (27,900,315 bytes), copied from the final successful debug build. SHA-256: `9C7DB88A3E7EBE7AAC8BABFDD799875640F87E85AFCA29F5C030517D10E79CEE`. The APK is excluded from Git; source, documentation and the printable QR assets are committed.

## Simulator/device checklist

1. Scan a printed QR or the included tag sheet from another screen; try low light, denied camera permission, cancellation and an unknown tag.
2. Import a tag PNG and verify that the matching household opens. Share a tag and confirm the receiving app can read it.
3. Record a collection, report, inspection result and payment, then switch roles and households to verify the connected results.
4. Test keyboard insets, Back behavior, rotation, large text, TalkBack, language changes and dark/system appearance.
5. Upgrade an existing installation and confirm saved work remains intact.

No emulator or Google AI Studio environment was set up. Network authentication, payment processing, SMS transport and external incident dispatch remain outside this app's current on-device workflows. The optional Gemini network path is retained but was not verified.
