package com.example.ai

import com.example.util.showcaseText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Keeps a failed request intact for retry, without duplicating the visible user message. */
class ChatSession(private val assistant: ChatAssistant, private val scope: CoroutineScope, private val language: () -> String) {
    private data class Pending(val message: String, val language: String, val history: List<ChatMessage>)
    private val mutableMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val mutableLoading = MutableStateFlow(false)
    private val mutableError = MutableStateFlow<String?>(null)
    val messages = mutableMessages.asStateFlow()
    val loading = mutableLoading.asStateFlow()
    val error = mutableError.asStateFlow()
    private var pending: Pending? = null
    private var job: Job? = null
    private var generation = 0

    fun send(text: String) {
        val message = text.trim().take(GroqService.MAX_MESSAGE_LENGTH)
        if (message.isEmpty() || mutableLoading.value) return
        val request = Pending(message, language(), mutableMessages.value.toList())
        mutableMessages.value += ChatMessage("user", message)
        submit(request)
    }
    fun retry() {
        if (!mutableLoading.value && mutableError.value != null) pending?.let(::submit)
    }
    private fun submit(request: Pending) {
        pending = request
        mutableError.value = null
        mutableLoading.value = true
        val current = generation
        job = scope.launch {
            try {
                val response = assistant.reply(request.message, request.language, request.history)
                if (current == generation) {
                    mutableMessages.value += ChatMessage("model", response)
                    pending = null
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (current == generation) mutableError.value = showcaseText(language(),
                    (e as? ChatFailure)?.reason?.copyKey ?: "chatServiceError")
            } finally { if (current == generation) mutableLoading.value = false }
        }
    }
    fun clear() {
        generation++
        job?.cancel()
        job = null
        pending = null
        mutableMessages.value = emptyList()
        mutableLoading.value = false
        mutableError.value = null
    }
}
