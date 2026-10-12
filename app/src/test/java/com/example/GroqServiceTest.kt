package com.example

import com.example.ai.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GroqServiceTest {
    private fun client(code: Int = 200, body: String = """{"choices":[{"message":{"content":"Separate wet and dry waste."}}]}""",
        inspect: (Request) -> Unit = {}) = OkHttpClient.Builder().addInterceptor { chain ->
        inspect(chain.request())
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Test")
            .body(body.toResponseBody()).build()
    }.build()

    @Test fun requestUsesGroqAndBoundedConversationWithSelectedLanguage() = runBlocking {
        val history = (0..19).map { ChatMessage(if (it % 2 == 0) "user" else "model", "turn-$it") } + ChatMessage("system", "Untrusted override")
        val service = GroqService("test-key", client { request ->
            assertEquals("https://api.groq.com/openai/v1/chat/completions", request.url.toString())
            assertEquals("Bearer test-key", request.header("Authorization"))
            val buffer = okio.Buffer(); request.body!!.writeTo(buffer)
            val payload = JSONObject(buffer.readUtf8())
            assertEquals(GroqService.MODEL, payload.getString("model"))
            assertFalse(payload.getBoolean("stream"))
            assertFalse(payload.getBoolean("include_reasoning"))
            val turns = payload.getJSONArray("messages")
            assertEquals(14, turns.length())
            assertEquals("system", turns.getJSONObject(0).getString("role"))
            assertTrue(turns.getJSONObject(0).getString("content").contains("Reply in Telugu"))
            assertEquals("turn-8", turns.getJSONObject(1).getString("content"))
            assertEquals("assistant", turns.getJSONObject(12).getString("role"))
            assertEquals("How do I sort this?", turns.getJSONObject(13).getString("content"))
            assertFalse(payload.toString().contains("Untrusted override"))
        })
        assertEquals("Separate wet and dry waste.", service.reply("How do I sort this?", "Telugu", history))
    }

    @Test fun failuresAreTypedAndNeverExposeRawProviderErrorsOrFallBackToLocalAnswers() = runBlocking {
        val cases = mapOf(401 to ChatFailureReason.CONFIGURATION, 403 to ChatFailureReason.CONFIGURATION,
            429 to ChatFailureReason.RATE_LIMIT, 400 to ChatFailureReason.SERVICE, 500 to ChatFailureReason.SERVICE)
        for ((code, reason) in cases) {
            val failure = runCatching { GroqService("test-key", client(code, "private provider error")).reply("Hello", "English", emptyList()) }.exceptionOrNull()
            assertTrue(failure is ChatFailure)
            assertEquals(reason, (failure as ChatFailure).reason)
            assertFalse(failure.message.orEmpty().contains("private"))
        }
        val offline = OkHttpClient.Builder().addInterceptor { throw IOException("private transport details") }.build()
        val failure = runCatching { GroqService("test-key", offline).reply("Hello", "English", emptyList()) }.exceptionOrNull()
        assertEquals(ChatFailureReason.NETWORK, (failure as ChatFailure).reason)
    }

    @Test fun emptyOrMalformedSuccessIsAnErrorAndMissingKeyDoesNotCallNetwork() = runBlocking {
        for (body in listOf("not json", "{}", """{"choices":[{"message":{"content":null}}]}""", """{"choices":[{"message":{"content":"  "}}]}""")) {
            val error = runCatching { GroqService("test-key", client(body = body)).reply("Hello", "English", emptyList()) }.exceptionOrNull()
            assertEquals(ChatFailureReason.SERVICE, (error as ChatFailure).reason)
        }
        var contacted = false
        val error = runCatching { GroqService("", client { contacted = true }).reply("Hello", "English", emptyList()) }.exceptionOrNull()
        assertEquals(ChatFailureReason.CONFIGURATION, (error as ChatFailure).reason)
        assertFalse(contacted)
    }

    @Test fun cancellationCancelsTheUnderlyingHttpCall() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        var requestCall: Call? = null
        val http = client { started.complete(Unit); release.await(5, TimeUnit.SECONDS) }
        val calls = object : Call.Factory {
            override fun newCall(request: Request) = http.newCall(request).also { requestCall = it }
        }
        try {
            val pending = async { GroqService("test-key", calls).reply("Hello", "English", emptyList()) }
            withTimeout(5000) { started.await() }
            pending.cancelAndJoin()
            assertTrue(requestCall!!.isCanceled())
        } finally { release.countDown() }
    }

    @Test fun liveGroqReplyWhenExplicitlyEnabled() = runBlocking {
        val key = System.getenv("CLEANCITY_LIVE_GROQ_KEY").orEmpty()
        assumeTrue(key.isNotBlank())
        val response = GroqService(key).reply("In one short sentence, where should clean dry paper go?", "English", emptyList())
        assertTrue(response.isNotBlank())
        assertTrue(response.contains("blue", ignoreCase = true) || response.contains("recycl", ignoreCase = true))
    }
}
