# Online Groq assistant

Updated 12 October 2026, preserving the user's AI Studio commit `52a5837`.

## Connect on the device

1. Open Assistant and choose **Connect assistant**.
2. Paste your Groq API key into the masked field and choose **Save key**.
3. Ask a question or select a suggested prompt. Use **Connection** to replace or remove the key.

The key is encrypted using AES-GCM with a key held by Android Keystore. Its ciphertext is stored under `noBackupFilesDir` using an atomic write. It is not stored in Compose saved state, shown in error messages, logged, backed up or packaged in either APK build type. Removing it clears the conversation and cancels pending requests. Losing the device encryption key or corrupting the ciphertext returns the app to connection setup.

The provided development key is retained only in the ignored local `.env` for local API checks. The Secrets Gradle Plugin explicitly excludes `GROQ_API_KEY`, so setting this environment file does not configure an installed app. A fresh installation requires the one-time device setup above. For a shared service-owned credential, use an authenticated backend instead of distributing that credential inside an APK.

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

The supplied key successfully listed supported models and received HTTP 200 for a short paper-sorting question through a direct local request to Groq. This check kept the credential in process memory and did not write it into a build artifact. It is not a simulator or installed-device test.

The automated tests cover request role mapping and limits, language, successful parsing, typed HTTP/connection failures, missing credentials, invalid responses, cancellation, retry behavior, conversation clearing, localized copy, encryption round-trip, corruption, removal and connection-sheet validation. The credential tests inject a temporary AES key; the real Android Keystore implementation still needs device verification.

The normal test suite does not contact Groq. An optional `liveGroqReplyWhenExplicitlyEnabled` test requires `CLEANCITY_LIVE_GROQ_KEY` in the test process environment and never uses BuildConfig. It is skipped otherwise.

Build/test totals and the handoff APK hash are recorded in [UI_CHANGES.md](../UI_CHANGES.md). On the user's simulator/device, check key entry and persistence, reply generation, denied/revoked credentials, airplane mode, manual retry, household switching and sign-out cancellation.
