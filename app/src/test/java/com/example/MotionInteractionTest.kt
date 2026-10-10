package com.example

import android.provider.Settings
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.ui.CityModel
import com.example.ui.OperationState
import com.example.ui.PreviewCityModel
import com.example.ui.components.*
import com.example.ui.screens.DailyPickupTab
import com.example.ui.theme.CleanCityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-port-xxhdpi")
class MotionInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun homeKeepsFullNameAndQuickActionsAboveTheFold() {
        val model = PreviewCityModel()
        var selected = ""
        compose.setContent {
            CleanCityTheme(darkTheme = false) {
                DailyPickupTab(model.registeredHousehold.value, emptyList(), { selected = "qr" },
                    { selected = "report" }, { selected = "bills" }, model.citizenPickups.value)
            }
        }
        compose.onNodeWithText("Mahesh Babu").assertIsDisplayed()
        compose.onNodeWithText("Mahesh", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Report").assertIsDisplayed().performClick()
        assertEquals("report", selected)
        compose.onNodeWithText("Bills").assertIsDisplayed().performClick()
        assertEquals("bills", selected)
        compose.onNodeWithText("Household QR").assertIsDisplayed().performClick()
        assertEquals("qr", selected)
    }

    @Test fun predictiveBackCancellationRetainsDraftAndCompletionRestoresListPosition() {
        val selected = mutableStateOf<Int?>(null)
        compose.setContent {
            CleanCityTheme(darkTheme = false) {
                CityDetailHost(selected.value, { selected.value = null }, list = {
                    LazyColumn(Modifier.fillMaxSize().testTag("stops")) {
                        items(30) { index ->
                            CityButton("Stop " + index, { selected.value = index }, Modifier.cityShared("stop-" + index))
                        }
                    }
                }, detail = { id ->
                    var draft by rememberSaveable { mutableStateOf("") }
                    CityDetailPage("stop-" + id, "Household " + id, { selected.value = null }) {
                        CityField(draft, { draft = it }, "Inspection note", Modifier.testTag("draft"))
                    }
                })
            }
        }
        compose.onNodeWithTag("stops").performScrollToIndex(18)
        compose.onNodeWithText("Stop 18").performClick()
        compose.onNodeWithTag("draft").performTextInput("Keep this inspection note")
        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.runOnIdle {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 400f, .55f, BackEventCompat.EDGE_LEFT))
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        compose.waitForIdle()
        compose.onNodeWithTag("draft").assertTextContains("Keep this inspection note")
        assertEquals(18, selected.value)
        compose.runOnIdle {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(150f, 400f, .8f, BackEventCompat.EDGE_LEFT))
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag("draft").assertDoesNotExist()
        compose.onNodeWithText("Stop 18").assertIsDisplayed().performClick()
        compose.onNodeWithTag("draft").assertTextContains("Keep this inspection note")
    }

    @Test fun confirmationFeedbackOccursOnlyOnceAfterSaveSucceeds() {
        val operationStates = MutableStateFlow<Map<String, OperationState>>(emptyMap())
        val model = object : CityModel by PreviewCityModel() {
            override val operations = operationStates
            override fun consumeOperation(key: String) { operationStates.value -= key }
        }
        val feedback = mutableListOf<CityFeedback>()
        var completed = 0
        compose.setContent {
            CleanCityTheme(darkTheme = false) {
                CompositionLocalProvider(LocalCityFeedback provides { feedback.add(it) }) {
                    OperationFeedback(model, "collection") { completed++ }
                }
            }
        }
        compose.runOnIdle { operationStates.value = mapOf("collection" to OperationState("collection", busy = true)) }
        compose.waitForIdle()
        assertTrue(feedback.isEmpty())
        compose.runOnIdle { operationStates.value = mapOf("collection" to OperationState("collection", error = "Could not save")) }
        compose.onNodeWithText("Could not save").assertExists()
        assertTrue(feedback.isEmpty())
        compose.runOnIdle { operationStates.value = mapOf("collection" to OperationState("collection", done = true)) }
        compose.waitForIdle()
        assertEquals(listOf(CityFeedback.CONFIRM), feedback)
        assertEquals(1, completed)
        compose.runOnIdle { operationStates.value = mapOf("weight" to OperationState("weight", busy = true)) }
        compose.waitForIdle()
        assertEquals(1, feedback.size)
    }

    @Test fun disabledSystemAnimationsShowEntrancesAndNavigationImmediately() {
        val resolver = compose.activity.contentResolver
        val previous = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            val page = mutableIntStateOf(0)
            var enabled = true
            compose.setContent {
                CleanCityTheme(darkTheme = false) {
                    enabled = LocalCityMotion.current
                    CityTransition(page.intValue) { id ->
                        Arrival(3) { CityButton("Page " + id, { page.intValue++ }) }
                    }
                }
            }
            compose.onNodeWithText("Page 0").assertIsDisplayed()
            assertFalse(enabled)
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("Page 0").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithText("Page 1").assertIsDisplayed()
            compose.onNodeWithText("Page 0").assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, previous)
        }
    }
}
