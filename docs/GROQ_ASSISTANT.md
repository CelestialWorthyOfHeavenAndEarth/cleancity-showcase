# Online Groq assistant

Updated 13 October 2026, preserving the user's AI Studio commit `52a5837`.

## Personal debug APK

At the owner's request, debug builds embed `GROQ_API_KEY` from the ignored project-root `.env`. The local handoff APK opens with chat configured: go to **Assistant** and ask a question without entering a key. The supplied key is extractable from this APK; the owner has explicitly accepted this for personal use. The key value and APK are excluded from Git.

`app/build.gradle.kts` defines a debug-only `BuildConfig.GROQ_API_KEY`; release builds explicitly set this field to an empty string. The Secrets Gradle Plugin still excludes the Groq property to prevent it from being included in all build types. If `.env` is absent or its Groq value is blank, debug builds use the device setup below. To remove the embedded credential from a future APK, clear the local property and rebuild; revoke the key in Groq to disable existing copies.

## Replace or remove the key on the device

1. Open Assistant and choose **Connection** (or **Connect assistant** when no key is configured).
2. Paste your Groq API key into the masked field and choose **Save key**.
3. Ask a question or select a suggested prompt. Use **Connection** to replace or remove the key.

An entered replacement key takes precedence over the bundled key. It is encrypted using AES-GCM with a key held by Android Keystore, and its ciphertext is stored under `noBackupFilesDir` using an atomic write. Entered keys are not stored in Compose saved state, shown in error messages, logged or backed up. Losing the device encryption key or corrupting the ciphertext returns the app to connection setup instead of silently switching accounts.

**Remove key** deletes the device override, clears the conversation, cancels pending requests and disables the bundled default on that installation. This choice survives app restarts and is excluded from backups. Saving a replacement key reconnects the assistant. Removal does not erase the key baked into the APK; clearing app data or a fresh installation restores the bundled default.

## Request behavior

`GroqService` calls [Groq's chat-completions API](https://console.groq.com/docs/api-reference#chat-create) over HTTPS with a bearer credential. The model is `openai/gpt-oss-120b`, selected from the models accessible to the supplied key and documented in [Groq's model reference](https://console.groq.com/docs/model/openai/gpt-oss-120b). Requests use low reasoning effort, hidden reasoning and a 1,024-token completion limit. No new dependency is needed; the app's existing OkHttp client library handles requests.

- User text is limited to 1,500 characters. At most twelve prior messages are sent, each capped at 4,000 characters. Model messages are mapped to the API's assistant role; unexpected roles are omitted.
- The system prompt requests the selected app language and concise plain text, and explains CleanCity's screens and sorting guidance. It does not contain household identity, addresses, payment records or uploaded photos.
- The assistant has no tools that can submit reports, move money, dispatch vehicles or mutate local records. It is instructed not to invent live pickup information or claim completed actions.
- Connection and overall request timeouts are bounded. Cancellation cancels the HTTP call. Redirects and automatic connection retries are disabled.
- Missing/rejected credentials, rate limits, connection failures and invalid responses have distinct, localized UI copy. Raw provider responses and credentials are never used in error text. Failed requests do not produce a canned local answer.
- The failed question remains visible. Retry sends the same question and preceding history without a duplicate user bubble. A completed request cannot be retried accidentally.
- Sign-out and household changes clear the conversation and cancel pending work. Conversation history is held in the current ViewModel session.
- Commissioner summaries continue to use local metrics; the optional legacy Gemini implementation is preserved separately.

## Verification

On 12 October, the supplied key successfully listed supported models and received HTTP 200 for a short paper-sorting question through a direct local request to Groq. That network check kept the credential in process memory. The 13 October revision separately embeds it in the personal debug APK as requested. Neither check is a simulator or installed-device test.

The automated tests cover request role mapping and limits, language, successful parsing, typed HTTP/connection failures, missing credentials, invalid responses, cancellation, retry behavior, conversation clearing, localized copy, encryption round-trip, corruption, removal and connection-sheet validation. Added checks cover immediate chat without setup, a bundled key on fresh installs without Android Keystore access, replacement precedence, persistent removal and corrupt-override handling. Credential tests inject dummy credentials and a temporary AES key; the real Android Keystore implementation still needs device verification.

The normal test suite does not contact Groq. An optional `liveGroqReplyWhenExplicitlyEnabled` test requires `CLEANCITY_LIVE_GROQ_KEY` in the test process environment and never uses BuildConfig. It is skipped otherwise.

Build/test totals and the handoff APK hash are recorded in [UI_CHANGES.md](../UI_CHANGES.md). On the user's simulator/device, check immediate reply generation, key replacement/removal and persistence, denied/revoked credentials, airplane mode, manual retry, household switching and sign-out cancellation.
