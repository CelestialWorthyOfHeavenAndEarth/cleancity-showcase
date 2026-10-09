package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.ui.PreviewCityModel
import com.example.ui.UserRole
import com.example.ui.components.RoleHeaderBar
import com.example.ui.screens.*
import com.example.ui.theme.CleanCityTheme
import com.example.ui.theme.CityColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// Host-rendered Compose views, not screenshots from an emulator or a running APK.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShowcaseRenderTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var rootView: View

    @Composable
    private fun Scene(index: Int, model: PreviewCityModel) {
        val view = LocalView.current
        SideEffect { rootView = view }
        Surface(Modifier.fillMaxSize(), color = CityColors.canvas) {
            Column {
                if (index != 0) {
                    val role = when(index) { 6 -> UserRole.COLLECTOR; 7 -> UserRole.DRIVER; 8 -> UserRole.SUPERVISOR; 9 -> UserRole.MUNICIPAL_ADMIN; else -> UserRole.CITIZEN }
                    RoleHeaderBar(role, "English", {}, {}, residentName = model.registeredHousehold.value?.residentName)
                }
                Box(Modifier.weight(1f)) {
                    when (index) {
                        0 -> LoginScreen(model)
                        1 -> CitizenScreen(model, model.registeredHousehold.value, model.announcements.value, model.citizenReports.value, model.citizenPayments.value)
                        2 -> ReportWasteScreen(model, model.citizenReports.value)
                        3 -> SegregationGuideTab(model.registeredHousehold.value)
                        4 -> AiChatbotScreen(model)
                        5 -> BillsPaymentTab(model.registeredHousehold.value, model.citizenPayments.value, { _, _ -> }, {})
                        6 -> CollectorScreen(model, model.households.value, model.allPickupLogs.value)
                        7 -> DriverScreen(model, model.weighbridgeLogs.value)
                        8 -> SupervisorScreen(model, model.allComplaints.value, model.crewAttendance.value)
                        9 -> AdminScreen(model, model.households.value, model.violations.value)
                    }
                }
            }
        }
    }
    private fun snapshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.runOnIdle {
            Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888).also { rootView.draw(Canvas(it)) }
        }
        check(bitmap.width > 0 && bitmap.height > 0)
        // Assert that this is an actual rendered view, not a blank capture.
        val samples = (1..9).flatMap { x -> (1..9).map { y -> bitmap.getPixel(bitmap.width*x/10, bitmap.height*y/10) } }
        check(samples.toSet().size > 3) { "Blank render: " + name }
        val output = File("build/showcase-renders").apply { mkdirs() }
        File(output, name + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun render(indices: List<Int>, themes: List<Boolean>, fontScale: Float = 1f, suffix: String = "") {
        val screen = mutableStateOf(indices.first())
        val dark = mutableStateOf(themes.first())
        val model = PreviewCityModel()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CleanCityTheme(darkTheme = dark.value) { Scene(screen.value, model) }
            }
        }
        for (night in themes) for (index in indices) {
            compose.runOnIdle { dark.value = night; screen.value = index }
            compose.waitForIdle()
            if (index == 0) compose.onNodeWithText("Explore demo").assertIsDisplayed()
            snapshot("screen-" + index + (if (night) "-dark" else "-light") + suffix)
        }
    }
    @Test fun majorScreensRenderInBothThemes() = render((0..9).toList(), listOf(false, true))
    @Test fun largeTextOperationalScreensRender() = render(listOf(1, 2, 6, 7, 8, 9), listOf(false), 1.5f, "-large-text")
    @Test @Config(qualifiers = "w1000dp-h900dp-land-mdpi")
    fun wideOperationalScreensRender() = render(listOf(1, 7, 9), listOf(false), suffix = "-wide")

    @Test fun simulatedScanAndWeighbridgeFormsAreReachableAndValidateInput() {
        val screen = mutableStateOf(6)
        val model = PreviewCityModel()
        compose.setContent { CleanCityTheme(darkTheme = false) { Scene(screen.value, model) } }
        compose.onNodeWithText("Simulate a scan").performScrollTo().performClick()
        compose.onNodeWithText("BIN-JH-101").performClick()
        compose.onNodeWithText("Segregation quality").assertIsDisplayed()
        compose.onNodeWithText("Save record").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { screen.value = 7 }
        compose.onAllNodesWithText("Weighbridge").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Gross weight (kg)").assertExists()
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.onNodeWithText("Check the details and try again.").assertExists()
    }
}

