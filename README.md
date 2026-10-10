# CleanCity

A native Android app for household waste collection, reporting, sorting, payments and municipal operations. Built with Kotlin, Jetpack Compose, Material 3 and Room, with a white/emerald identity, dark appearance and nine languages.

## Get started

Choose **Get started** to open Mahesh Babu's household. Use the role control in the header to move between Citizen, Collector, Driver, Supervisor and Admin. Preferences lets you choose another household, change language or appearance, and sign out.

The presentation dataset uses **fictional records with TFI actor names**. Names are used as a memorable cast; addresses, financial records, crew assignments and activities do not describe the real people or imply endorsement. Fine records use invented establishment names.

## Included data

A fresh installation includes 24 households across three wards, 350 pickup records across 15 days, 128 payments across six months, 30 reports across four statuses, 12 crew members, 18 weighbridge entries, nine fines, six announcements and eight route stops. Existing installations receive the additional records once, preserving saved edits and transactions.

## QR workflow

1. In Citizen, open **Household QR Code**. Each household has a real encoded QR image and readable ID.
2. Use **Share QR tag** to export a PNG, or open the printable [household tag sheet](docs/household-tags/index.html).
3. Switch to Collector and select **Scan household QR**.
4. Open the camera to scan a printed tag or another screen, or choose **Read QR from image** to read a saved PNG.
5. The decoded ID opens that household's collection form. If the camera is unavailable, enter the household ID directly.

QR encoding and decoding use [ZXing](https://github.com/zxing/zxing); camera scanning uses [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded). No scanner app, cloud decoder, service key or downloaded recognition model is required.

## Connected presentation walkthrough

- **Collector → Citizen:** search for Mahesh Babu, scan his QR or open his stop, then record the collection quality. Citizen status, score and collection history update. Repeating today's collection does not inflate scores.
- **Citizen → Supervisor:** create a report with a photo and address. Filter the supervisor queue by status, add an inspection photo or note, and resolve it. Citizen history reflects the result.
- **Citizen → Admin:** record a bill payment and view its receipt. The admin's outstanding balance and transaction list update. Recording changes the ledger; it does not transfer money.
- **Driver → Supervisor:** complete route stops, enter a weighbridge slip, or report a vehicle issue. Progress is retained across role switches and the issue appears in the supervisor view.
- **Admin:** compare three wards, review six months of payments, copy a payment reminder, create a fine and generate a commissioner brief from the same dashboard metrics.

Search and status filters keep the larger lists manageable. Citizen drafts and navigation are saved separately for each selected household. Room records and appearance/language preferences persist; route completion and acknowledgments are held in the saved session.

## Motion and navigation

Home displays the full household name, including **Mahesh Babu**. The AI Studio update places Household QR inside the pickup card, with Report and Bills below, and improves crew cards and scrolling filters. Controls use spring press feedback, tabs transition directionally, and saved values animate. Report and collection records expand into their detail pages, with predictive Back that preserves drafts on cancellation and restores list position.

The welcome page uses the supplied neighborhood video behind a translucent theme-aware gradient. The bundled clip is silent, local and 8.58 seconds long, with a softened loop boundary. A pause/play control is available; disabled system animations use a still image. Video stops for sign-in forms, language selection, leaving the welcome page or backgrounding the app. Playback failure also falls back to the still image, without blocking entry. See [MOTION_DESIGN.md](MOTION_DESIGN.md) and [welcome video notes](docs/WELCOME_VIDEO.md).

## Build

Use the checked-in Gradle 9.3.1 wrapper and Android SDK 36.1. This workspace was built with JDK 22.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The package is generated at `app/build/outputs/apk/debug/app-debug.apk`. Local `.env` files, signing credentials, APKs and build caches are excluded from Git. `CleanCity-Showcase-debug.apk` is the local handoff copy.

Debug builds use a project-root `debug.keystore` when supplied; otherwise they use Android's standard local debug key.

`DesignPreviews.kt` supplies light/dark, phone, wide and large-text configurations. Host renders are written to `app/build/showcase-renders/`; QR tests produce `app/build/household-tags/`. These checks do not replace testing camera permissions, hardware scanning and platform interactions on a device.

## Integration boundaries

The assistant and commissioner brief work offline. `GeminiService(useNetwork = true)` preserves the optional service integration, which needs a compatible model and your own key. Login remains an on-device entry flow. Payment records do not process money, copied reminders are not sent automatically, and vehicle issues are added to the in-app queue rather than dispatched to an external emergency service.

See [UI_CHANGES.md](UI_CHANGES.md) for current changes, verification results and remaining simulator checks. The original [showcase plan](CLEANCITY_SHOWCASE_PLAN.md) is retained as implementation history.
