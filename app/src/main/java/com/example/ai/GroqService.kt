package com.example.ai

import com.example.data.DemoScenario
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun interface ChatAssistant {
    suspend fun reply(message: String, language: String, history: List<ChatMessage>): String
}

enum class ChatFailureReason(val copyKey: String) {
    CONFIGURATION("chatKeyError"), NETWORK("chatNetworkError"), RATE_LIMIT("chatRateError"), SERVICE("chatServiceError")
}
class ChatFailure(val reason: ChatFailureReason) : Exception(reason.name)

class GroqService(
    private val apiKey: String,
    private val calls: Call.Factory = client
) : ChatAssistant {
    companion object {
        const val MODEL = "openai/gpt-oss-120b"
        const val MAX_MESSAGE_LENGTH = 1500
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
    }

    override suspend fun reply(message: String, language: String, history: List<ChatMessage>): String {
        if (apiKey.isBlank()) throw ChatFailure(ChatFailureReason.CONFIGURATION)
        require(message.isNotBlank() && message.length <= MAX_MESSAGE_LENGTH)
        val selectedLanguage = language.takeIf { it in DemoScenario.languages } ?: "English"
        val turns = JSONArray().put(JSONObject().put("role", "system").put("content", """
            You are CleanCity's helpful waste and sanitation assistant for Andhra Pradesh.
            Reply in $selectedLanguage, in plain text without markdown or emojis. Be concise, usually under 120 words.
            Explain sorting, composting, pickup schedules, reporting and the app's bill records.
            App guide: household collection window is 7:00–9:30 AM. Monthly fees are residential ₹90,
            commercial shops ₹350 and restaurants ₹850. Explain these as app information, not verified municipal policy.
            Green bins hold food and garden waste; blue bins hold clean dry recyclables.
            Keep batteries, electronics and sanitary waste separate and follow local collection guidance.
            Home shows collection status and Household QR. Report accepts a photo, address and description.
            Bills shows saved payments and receipts. Recording a payment does not transfer money.
            You can provide guidance only. You cannot submit reports, pay bills, dispatch vehicles or change records.
            You do not have household records, live vehicle positions, actual pickup confirmations or current regulations.
            Never invent these or claim an action was completed. Direct the user to the relevant app screen.
            For uncertain local rules or hazardous waste, suggest contacting the local sanitation team.
        """.trimIndent()))
        history.filter { it.sender in setOf("user", "model") && it.text.isNotBlank() }.takeLast(12).forEach {
            turns.put(JSONObject().put("role", if (it.sender == "user") "user" else "assistant").put("content", it.text.take(4000)))
        }
        turns.put(JSONObject().put("role", "user").put("content", message))
        val body = JSONObject().put("model", MODEL).put("messages", turns)
            .put("max_completion_tokens", 1024).put("reasoning_effort", "low")
            .put("include_reasoning", false).put("stream", false)
        val request = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey.trim()).header("Accept", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return suspendCancellableCoroutine { continuation ->
            val call = calls.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(ChatFailure(ChatFailureReason.NETWORK))
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        try {
                            val reason = when (response.code) {
                                401, 403 -> ChatFailureReason.CONFIGURATION
                                429 -> ChatFailureReason.RATE_LIMIT
                                in 200..299 -> null
                                else -> ChatFailureReason.SERVICE
                            }
                            if (reason != null) throw ChatFailure(reason)
                            val content = JSONObject(response.body?.string().orEmpty()).optJSONArray("choices")
                                ?.optJSONObject(0)?.optJSONObject("message")?.opt("content") as? String
                            val answer = content?.trim()?.takeIf { it.isNotEmpty() }
                                ?: throw ChatFailure(ChatFailureReason.SERVICE)
                            continuation.resume(answer)
                        } catch (e: ChatFailure) { continuation.resumeWithException(e) }
                        catch (_: IOException) { continuation.resumeWithException(ChatFailure(ChatFailureReason.NETWORK)) }
                        catch (_: Exception) { continuation.resumeWithException(ChatFailure(ChatFailureReason.SERVICE)) }
                    }
                }
            })
        }
    }
}
