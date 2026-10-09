package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val sender: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MunicipalStats(
    val totalHouseholds: Int,
    val collectedToday: Int,
    val coveragePercent: Double,
    val segregationRate: Double,
    val wasteRecoveredTons: Double,
    val costRecoveryPercent: Double,
    val openComplaints: Int,
    val resolvedComplaints: Int
)

class GeminiService(private val useNetwork: Boolean = false) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-3.8-flash"
    private val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    suspend fun chatAssistant(
        userMessage: String,
        language: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        if (!useNetwork) return@withContext localAnswer(userMessage, language)
        val apiKey = (if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY !in setOf("\"\"", "your_api_key_here")) {
            BuildConfig.GEMINI_API_KEY
        } else {
            System.getenv("GEMINI_API_KEY") ?: ""
        }).trim().trim('"')

        if (apiKey.isBlank() || apiKey in setOf("MY_GEMINI_API_KEY", "YOUR_GEMINI_API_KEY", "your_api_key_here")) {
            return@withContext localAnswer(userMessage, language)
        }

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()

            val systemObj = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", """
                        You are the helpful CleanCity Municipal Waste Assistant for a clean city.
                        Respond to citizens in ${language}. This is a local showcase; payments, alerts, dispatch and resolution are demonstrations.
                        Knowledge base:
                        - Monthly waste charge: Residential 90 rupees/month, Commercial shops 350 rupees/month, Restaurants 850 rupees/month.
                        - Pickup timings: Door-to-door is 7:00 AM to 9:30 AM daily.
                        - Two-bin rule: Green Bin (organic kitchen wet waste, fruit peels, vegetable scraps, leftover food), Blue Bin (dry recyclable paper, plastics, tins, cardboards). Sanitary and batteries go in a separate Red bag/hazardous bin.
                        - Uncollected waste reporting: Citizens can upload a photo of uncollected trash and capture their GPS location in the 'Report Waste' tab.
                        - The sample queue has a 24-hour target. Do not promise real collection, alerts, payments or resolution.
                        IMPORTANT: Do not use any emojis in your response. Keep answers concise, clear, and professional.
                    """.trimIndent()))
                }
                put("parts", parts)
            }
            rootJson.put("systemInstruction", systemObj)

            // Multi-turn Gemini API rules: must start with user turn and strictly alternate user/model
            val priorMessages = if (history.isNotEmpty() && history.last().text == userMessage) {
                history.dropLast(1)
            } else {
                history
            }

            val validHistory = priorMessages.dropWhile { it.sender != "user" }.takeLast(6)
            var lastRole: String? = null
            for (msg in validHistory) {
                val role = if (msg.sender == "user") "user" else "model"
                if (role != lastRole) {
                    val turn = JSONObject().apply {
                        val p = JSONArray().apply { put(JSONObject().put("text", msg.text)) }
                        put("role", role)
                        put("parts", p)
                    }
                    contentsArray.put(turn)
                    lastRole = role
                }
            }

            // Append current user message
            val currentTurn = JSONObject().apply {
                val p = JSONArray().apply { put(JSONObject().put("text", userMessage)) }
                put("role", "user")
                put("parts", p)
            }
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$endpointUrl?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val respJson = JSONObject(responseBody)
                    val text = respJson.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext stripEmojis(text)
                    }
                } else {
                    android.util.Log.e("GeminiService", "API error ${response.code}: $responseBody")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiService", "Chat call exception", e)
        }

        localAnswer(userMessage, language)
    }

    suspend fun generateExecutiveSummary(stats: MunicipalStats, language: String = "English"): String = withContext(Dispatchers.IO) {
        if (!useNetwork) return@withContext localSummary(stats, language)
        val apiKey = (if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY !in setOf("\"\"", "your_api_key_here")) {
            BuildConfig.GEMINI_API_KEY
        } else {
            System.getenv("GEMINI_API_KEY") ?: ""
        }).trim().trim('"')

        if (apiKey.isBlank() || apiKey in setOf("MY_GEMINI_API_KEY", "YOUR_GEMINI_API_KEY", "your_api_key_here")) {
            return@withContext localSummary(stats, language)
        }

        try {
            val prompt = """
                You are Chief Municipal Sanitation Advisor.
                Turn these raw city waste monitoring metrics into an executive briefing for the City Municipal Commissioner:
                - Total registered households: ${stats.totalHouseholds}
                - Households collected today: ${stats.collectedToday}
                - Route Coverage: ${String.format("%.1f", stats.coveragePercent)}%
                - Source Segregation Rate: ${String.format("%.1f", stats.segregationRate)}%
                - Daily Waste Recovered: ${stats.wasteRecoveredTons} Metric Tonnes
                - Cost Recovery: ${String.format("%.1f", stats.costRecoveryPercent)}%
                - Open Waste Reports: ${stats.openComplaints}
                - Resolved reports (all time): ${stats.resolvedComplaints}

                Generate a 3-paragraph executive summary without emojis:
                1. Operational highlights and city sanitation progress
                2. Key concerns such as uncollected waste hotspots or pending reports
                3. Three actionable recommendations for field inspectors.
            """.trimIndent()

            val rootJson = JSONObject().apply {
                val cArray = JSONArray().apply {
                    val cObj = JSONObject().apply {
                        val pArray = JSONArray().apply { put(JSONObject().put("text", prompt)) }
                        put("parts", pArray)
                    }
                    put(cObj)
                }
                put("contents", cArray)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$endpointUrl?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val respJson = JSONObject(responseBody)
                    val text = respJson.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext stripEmojis(text)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback
        }

        localSummary(stats, language)
    }

    private fun localAnswer(message: String, language: String): String {
        val strings = com.example.util.getAppStrings(language)
        val q = message.lowercase()
        return when {
            message == strings.promptSegregation || listOf("bin", "sort", "segregat").any { it in q } ->
                strings.greenBinTitle + ": " + strings.greenBinDesc + "\n\n" + strings.blueBinTitle + ": " + strings.blueBinDesc + "\n\n" + strings.redBagTitle + ": " + strings.redBagDesc
            message == strings.promptPickupTimes || listOf("time", "when", "schedule", "pickup").any { it in q } ->
                strings.collectionHistory + ": 7:00–9:30 AM. " + com.example.util.showcaseText(language, "demoSchedule")
            message == strings.promptPayFee || listOf("fee", "bill", "pay", "charge").any { it in q } ->
                strings.catResidential + ": ₹90\n" + strings.catCommercial + ": ₹350\n" + strings.catHotel + ": ₹850\n\n" + com.example.util.showcaseText(language, "demoPayment")
            message == strings.promptHomeCompost || "compost" in q -> com.example.util.showcaseText(language, "compostHelp")
            listOf("report", "photo", "dump", "trash").any { it in q } -> com.example.util.showcaseText(language, "reportIntro") + "\n" + com.example.util.showcaseText(language, "demoNotice")
            else -> strings.aiAssistantSubtitle + "\n\n" + strings.promptPickupTimes + "\n" + strings.promptSegregation + "\n" + strings.promptPayFee + "\n" + strings.promptHomeCompost
        }
    }

    private fun localSummary(stats: MunicipalStats, language: String): String {
            val s = com.example.util.getAppStrings(language)
            fun label(key: String) = com.example.util.showcaseText(language, key)
            return listOf(
                label("coverage") + ": " + stats.collectedToday + " / " + stats.totalHouseholds + " (" + "%.0f%%".format(stats.coveragePercent) + ")",
                s.segregationScore + ": " + "%.0f%%".format(stats.segregationRate),
                label("net") + ": " + "%.1f t".format(stats.wasteRecoveredTons),
                label("recovery") + ": " + "%.0f%%".format(stats.costRecoveryPercent),
                s.filterPending + ": " + stats.openComplaints,
                s.statusResolved + ": " + stats.resolvedComplaints
            ).joinToString("\n\n")
    }

    private fun stripEmojis(text: String): String {
        return text.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()
    }
}
