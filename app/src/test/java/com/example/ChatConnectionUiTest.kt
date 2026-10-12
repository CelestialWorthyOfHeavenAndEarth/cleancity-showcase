package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.CityModel
import com.example.ui.PreviewCityModel
import com.example.ui.screens.AiChatbotScreen
import com.example.ui.theme.CleanCityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatConnectionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectionSheetValidatesKeyAndChatWaitsForConfiguration() {
        var saved = ""
        val model = object : CityModel by PreviewCityModel() {
            override val chatConfigured = MutableStateFlow(false)
            override fun configureChatKey(key: String) { saved = key }
        }
        compose.setContent { CleanCityTheme { AiChatbotScreen(model) } }
        compose.onNodeWithContentDescription("Send").assertIsNotEnabled()
        compose.onAllNodesWithText("Connect assistant").onLast().performClick()
        compose.onNodeWithText("Save key").assertIsNotEnabled()
        compose.onNodeWithText("Groq API key").performTextInput("invalid")
        compose.onNodeWithText("Save key").assertIsNotEnabled()
        val key = "gsk_" + "fake_key_for_ui_test_".repeat(2)
        compose.onNodeWithText("Groq API key").performTextReplacement(key)
        compose.onNodeWithText("Save key").performScrollTo().assertIsEnabled().performClick()
        assertEquals(key, saved)
    }
}
