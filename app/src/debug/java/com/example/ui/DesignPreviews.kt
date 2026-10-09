package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "Phone · light", widthDp = 360, heightDp = 800)
@Preview(name = "Phone · dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 360, heightDp = 1000, fontScale = 1.5f)
@Preview(name = "Wide", widthDp = 1120, heightDp = 900)
annotation class CityPreviews

@Composable
private fun PreviewFrame(content: @Composable (PreviewCityModel) -> Unit) {
    val model = remember { PreviewCityModel() }
    CleanCityTheme {
        Surface(Modifier.fillMaxSize(), color = CityColors.canvas) { content(model) }
    }
}
@CityPreviews @Composable fun WelcomePreview() = PreviewFrame { LoginScreen(it) }
@CityPreviews @Composable fun CitizenPreview() = PreviewFrame { model ->
    Column {
        RoleHeaderBar(UserRole.CITIZEN, "English", {}, {}, residentName = model.registeredHousehold.value?.residentName)
        CitizenScreen(model, model.registeredHousehold.value, model.announcements.value, model.citizenReports.value, model.citizenPayments.value)
    }
}
@CityPreviews @Composable fun ReportPreview() = PreviewFrame { ReportWasteScreen(it, it.citizenReports.value) }
@CityPreviews @Composable fun SortingPreview() = PreviewFrame { SegregationGuideTab(it.registeredHousehold.value) }
@CityPreviews @Composable fun AssistantPreview() = PreviewFrame { AiChatbotScreen(it) }
@CityPreviews @Composable fun BillsPreview() = PreviewFrame { BillsPaymentTab(it.registeredHousehold.value, it.citizenPayments.value, { _, _ -> }, {}) }
@CityPreviews @Composable fun CollectorPreview() = PreviewFrame { CollectorScreen(it, it.households.value, it.allPickupLogs.value) }
@CityPreviews @Composable fun DriverPreview() = PreviewFrame { DriverScreen(it, it.weighbridgeLogs.value) }
@CityPreviews @Composable fun SupervisorPreview() = PreviewFrame { SupervisorScreen(it, it.allComplaints.value, it.crewAttendance.value) }
@CityPreviews @Composable fun AdminPreview() = PreviewFrame { AdminScreen(it, it.households.value, it.violations.value) }
@CityPreviews @Composable fun FeedbackPreview() = PreviewFrame {
    CityPage("Interface states") {
        item { EmptyState("Nothing here yet") }
        item { CityPanel { CircularProgressIndicator(); Text("Saving your report…") } }
        item { CityPanel { Icon(Icons.Default.CloudOff, null); Text("Your draft is kept."); CityButton("Try again", {}) } }
        item { StatusBadge("Report submitted") }
    }
}

