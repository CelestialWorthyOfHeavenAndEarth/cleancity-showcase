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
import com.example.ui.components.LocalCityMotion
import com.example.ui.components.CityPage
import com.example.ui.components.HouseholdQrCard
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
        // Static layout captures use the reduced-motion path: software Canvas cannot draw shared RenderNodes.
        // Motion and predictive Back are exercised separately by MotionInteractionTest.
        CompositionLocalProvider(LocalCityMotion provides false) {
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
    }
    private fun snapshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.runOnIdle {
            Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888).also { output ->
                // Only the activity view is captured here. Dialog sheets are checked through semantics below.
                rootView.draw(Canvas(output))
            }
        }
        check(bitmap.width > 0 && bitmap.height > 0)
        // Assert that this is an actual rendered view, not a blank capture.
        val samples = (1..40).flatMap { x -> (1..40).map { y -> bitmap.getPixel(bitmap.width*x/41, bitmap.height*y/41) } }
        val output = File("build/showcase-renders").apply { mkdirs() }
        File(output, name + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(samples.toSet().size > 8) { "Blank render: " + name }
        bitmap.recycle()
    }
    private fun render(indices: List<Int>, themes: List<Boolean>, fontScale: Float = 1f, suffix: String = "") {
        val screen = mutableStateOf(indices.first())
        val dark = mutableStateOf(themes.first())
        val model = PreviewCityModel()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CleanCityTheme(darkTheme = dark.value) { key(screen.value, dark.value) { Scene(screen.value, model) } }
            }
        }
        for (night in themes) for (index in indices) {
            compose.runOnIdle { dark.value = night; screen.value = index }
            compose.waitForIdle()
            if (index == 0) compose.onNodeWithText("Get started").assertIsDisplayed()
            snapshot("screen-" + index + (if (night) "-dark" else "-light") + suffix)
        }
    }
    @Test fun householdQrCardRendersInBothThemes() {
        val dark = mutableStateOf(false)
        val home = PreviewCityModel().households.value.first()
        compose.setContent {
            val view = LocalView.current
            SideEffect { rootView = view }
            CleanCityTheme(darkTheme = dark.value) {
                Surface(Modifier.fillMaxSize(), color = CityColors.canvas) { CityPage("Household QR") { item { HouseholdQrCard(home) } } }
            }
        }
        snapshot("household-qr-light")
        compose.runOnIdle { dark.value = true }
        snapshot("household-qr-dark")
    }

    @Test fun majorScreensRenderInBothThemes() = render((0..9).toList(), listOf(false, true))
    @Test fun largeTextOperationalScreensRender() = render(listOf(0, 1, 2, 6, 7, 8, 9), listOf(false), 1.5f, "-large-text")
    @Test @Config(qualifiers = "w1000dp-h900dp-land-mdpi")
    fun wideOperationalScreensRender() = render(listOf(1, 7, 9), listOf(false), suffix = "-wide")

    @Test fun expandedOperationalViewsRender() {
        val screen = mutableStateOf(8)
        val model = PreviewCityModel()
        compose.setContent { CleanCityTheme(darkTheme = false) { Scene(screen.value, model) } }
        compose.onNodeWithTag("supervisor_tab_1").performClick()
        snapshot("supervisor-reports")
        compose.onAllNodesWithText("Overview")[1].performScrollTo().performClick()
        snapshot("report-detail")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("supervisor_tab_2").performClick()
        snapshot("supervisor-crew")
        compose.runOnIdle { screen.value = 9 }
        compose.onNodeWithTag("admin_tab_1").performClick()
        snapshot("admin-wards")
        compose.onNodeWithTag("admin_tab_2").performClick()
        snapshot("admin-finance")
        compose.onNodeWithText("Transactions").performScrollTo().performClick()
        snapshot("admin-payments")
    }

    @Test fun householdIdLookupAndWeighbridgeFormsAreReachableAndValidateInput() {
        val screen = mutableStateOf(6)
        val model = PreviewCityModel()
        compose.setContent { CleanCityTheme(darkTheme = false) { Scene(screen.value, model) } }
        compose.onNodeWithText("Scan household QR").performScrollTo().performClick()
        compose.onNodeWithText("Open camera").assertExists()
        compose.onNodeWithText("Read QR from image").assertExists()
        compose.onNodeWithText("Household ID").performTextInput("BIN-UNKNOWN")
        compose.onNodeWithText("Open household").performScrollTo().performClick()
        compose.onNodeWithText("No matching household. Check the QR or ID.").assertExists()
        compose.onNodeWithText("Household ID").performTextReplacement("BIN-JH-101")
        compose.onNodeWithText("Open household").performScrollTo().performClick()
        compose.onNodeWithText("Segregation quality").assertIsDisplayed()
        snapshot("collection-detail")
        compose.onNodeWithText("Save record").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { screen.value = 7 }
        compose.onAllNodesWithText("Weighbridge").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Gross weight (kg)").assertExists()
        compose.onNodeWithText("Save record").performScrollTo().performClick()
        compose.onNodeWithText("Check the details and try again.").assertExists()
    }
}

