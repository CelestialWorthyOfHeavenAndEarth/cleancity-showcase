package com.example

import com.example.ai.*
import com.example.data.DemoScenario
import com.example.util.showcaseText
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatSessionTest {
    @Test fun retryKeepsTheQuestionAndHistoryWithoutDuplicateMessages() = runTest {
        var calls = 0
        val histories = mutableListOf<List<ChatMessage>>()
        val session = ChatSession(ChatAssistant { _, _, history ->
            histories += history
            if (++calls == 1) throw ChatFailure(ChatFailureReason.NETWORK)
            "Use the blue bin."
        }, this) { "Telugu" }
        session.send("  Where does paper go?  ")
        runCurrent()
        assertFalse(session.loading.value)
        assertEquals(showcaseText("Telugu", "chatNetworkError"), session.error.value)
        assertEquals(1, session.messages.value.size)
        session.retry()
        runCurrent()
        assertEquals(2, session.messages.value.size)
        assertEquals("Where does paper go?", session.messages.value.first().text)
        assertEquals(histories[0], histories[1])
        assertNull(session.error.value)
        session.retry()
        runCurrent()
        assertEquals(2, calls)
    }

    @Test fun rapidSendsAreGuardedAndClearingCancelsAnInFlightReply() = runTest {
        var calls = 0
        var cancelled = false
        val session = ChatSession(ChatAssistant { _, _, _ ->
            calls++
            try { awaitCancellation() } finally { cancelled = true }
        }, this) { "English" }
        session.send("First question")
        session.send("Accidental double tap")
        runCurrent()
        assertEquals(1, calls)
        assertEquals(1, session.messages.value.size)
        assertTrue(session.loading.value)
        session.clear()
        runCurrent()
        assertTrue(cancelled)
        assertTrue(session.messages.value.isEmpty())
        assertNull(session.error.value)
        assertFalse(session.loading.value)
        session.retry()
        runCurrent()
        assertEquals(1, calls)
    }

    @Test fun newTurnsKeepContextAndAllNineLanguagesHaveOnlineAndFailureCopy() = runTest {
        var received = emptyList<ChatMessage>()
        val session = ChatSession(ChatAssistant { _, _, history -> received = history; "Answer" }, this) { "English" }
        session.send("First")
        runCurrent()
        session.send("Second")
        runCurrent()
        assertEquals(listOf("user", "model"), received.map { it.sender })
        assertEquals(listOf("First", "Answer"), received.map { it.text })
        for (language in DemoScenario.languages) for (key in listOf("onlineAssistant", "chatPrivacy", "chatNetworkError", "chatKeyError", "chatRateError", "chatServiceError", "chatConnect", "chatConnection", "chatKeyHelp", "chatSaveKey", "chatRemoveKey")) {
            assertTrue(showcaseText(language, key).isNotBlank())
        }
    }
}
