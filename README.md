# CleanCity — Android showcase

A complete local showcase for citizen, collector, driver, supervisor, and municipal admin journeys, built with Kotlin, Jetpack Compose, Material 3, a shared ViewModel, and Room. The visual system uses warm ivory, forest green, soft lime, Plus Jakarta Sans, and native illustrated neighborhood, vehicle, bin, and route scenes.

## Start the demo

Choose **Explore demo** on the welcome screen. This opens the Jubilee Hills household in Ward 104. The visible role control switches between all five roles; the account control provides nine languages, Light/Dark/System appearance, and sign-out. Light is the initial appearance. Login, registration, and staff entry remain available as demonstrations; the displayed sample OTP can be filled automatically.

The showcase runs without a service key. Authentication, payments, tag scanning, routes, reminders, dispatch, and emergency acknowledgments are explicitly simulated. No money is transferred and no SMS or emergency call is sent.

## Connected walkthrough

1. **Citizen → Report:** enter a description and editable address, or select the sample location. Submit and open My reports.
2. **Supervisor → Reports:** open that report, inspect it, select an image or the clearly labeled sample proof, then resolve it. Return to Citizen to see the resolved history.
3. **Citizen → Bills:** confirm a demo payment and choose View receipt. The household disappears from the admin's unpaid list.
4. **Collector:** simulate a scan, select `BIN-JH-101`, and save a collected, mixed-waste, or not-collected result. Citizen pickup status and score update from the same records. A second submission for the same stop/day cannot inflate the score.
5. **Driver:** complete route stops, record a valid weighbridge entry, and acknowledge the demo SOS. The route survives role switching; the supervisor sees the acknowledgment.
6. **Municipal admin:** compare ward metrics, generate a local commissioner brief, preview a reminder, and create a validated sample fine.

Room records persist across launches. Appearance and language are stored locally. Driver stop completion and simulated acknowledgments are saved with the current Android demo session. Existing version 2 databases migrate to version 3 without deleting records.

## Build and review

The project uses Android SDK 36.1 and the checked-in Gradle 9.3.1 wrapper. The available Windows JDK 22 and SDK were used for verification. Set `JAVA_HOME` and `ANDROID_HOME` to your installed tools if needed, or open the project in Android Studio.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The locally generated package is `CleanCity-Showcase-debug.apk`, also available after a build at `app/build/outputs/apk/debug/app-debug.apk`. Generated APKs and local credentials are excluded from Git. The final offline build and all 26 tests passed; see the handoff for verification details. The older root-level `CleanCity.apk` is a legacy artifact and was not replaced.

`app/src/debug/java/com/example/ui/DesignPreviews.kt` contains 44 Compose preview configurations covering major screens and interface states in light, dark, wide, and enlarged-text variants. Host-rendered images are produced by `ShowcaseRenderTest` in `app/build/showcase-renders/`; these do not represent an installed APK or device test.

## Optional network assistant

`GeminiService` defaults to deterministic, localized offline answers and summaries. The existing optional network implementation remains behind the same interface. Enabling it requires an explicit code opt-in (`GeminiService(useNetwork = true)`), a valid `GEMINI_API_KEY`, and a compatible model configuration. Adding a key alone does not turn network calls on. That optional path was not verified during this showcase work.

## Handoff

- [Accepted showcase plan](CLEANCITY_SHOWCASE_PLAN.md)
- [Implementation, verification, and simulator checklist](UI_CHANGES.md)

No emulator, simulator, or hosting environment was provisioned. Device rendering and platform interactions remain for the simulator review described in the handoff.
