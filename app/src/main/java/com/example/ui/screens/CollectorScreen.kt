package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.CityModel
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectorScreen(viewModel: CityModel, households: List<HouseholdEntity>, pickupLogs: List<PickupLogEntity>, modifier: Modifier = Modifier) {
    val s = appStrings()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    val today = DemoScenario.today(pickupLogs)
    val recorded = today.map { it.householdId }.toSet()
    val collected = today.count { it.status == "COLLECTED" }
    var scanner by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var earnings by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("COLLECTED") }
    var quality by rememberSaveable { mutableStateOf("GOOD") }
    var reason by rememberSaveable { mutableIntStateOf(0) }
    fun select(id: Int) { viewModel.clearOperation(); selectedId = id; status = "COLLECTED"; quality = "GOOD"; reason = 0 }
    CityPage(cityText("route"), "Ramesh Kumar · " + DemoScenario.date(), modifier) {
        item {
            AdaptiveColumns(primary = {
                CityPanel(color = CityColors.positiveBg) {
                    StatusBadge(cityText("today"))
                    Text(recorded.size.toString() + " / " + households.size, style = MaterialTheme.typography.displayLarge)
                    Text(cityText("stops") + " · " + cityText("completed"), color = CityColors.muted)
                    ProgressLine(if (households.isEmpty()) 0f else recorded.size.toFloat()/households.size)
                    CityButton(cityText("simulateScan"), { scanner = true }, icon = Icons.Default.QrCodeScanner)
                }
            }, secondary = {
                MetricPair(collected.toString(), cityText("collected"), (today.size - collected).toString(), cityText("exceptions"))
                OutlinedButton({ earnings = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Default.AccountBalanceWallet, null); Spacer(Modifier.width(8.dp)); Text(cityText("earnings")) }
            })
        }
        item { SectionTitle(cityText("stops")) }
        if (households.isEmpty()) item { EmptyState(cityText("noRecords")) }
        items(households, key = { it.id }) { home ->
            val log = today.firstOrNull { it.householdId == home.id }
            CityRecord(home.residentName, home.address, if (log == null) Icons.Default.Home else Icons.Default.CheckCircle, { select(home.id) }) {
                if (log == null) Icon(Icons.Default.ChevronRight, cityText("record"), tint = CityColors.primary)
                else Icon(if (log.status == "COLLECTED") Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null,
                    tint = if (log.status == "COLLECTED") CityColors.primary else CityColors.warning)
            }
        }
    }
    if (scanner) QrScannerDialog(households, { code ->
        scanner = false
        viewModel.simulateQrScan(code)?.let { select(it.id) }
    }, { scanner = false })
    households.firstOrNull { it.id == selectedId }?.let { household ->
        val existing = today.firstOrNull { it.householdId == household.id }
        CitySheet(household.residentName, { if (!op.busy) selectedId = null }) {
            Text(household.address, color = CityColors.muted)
            if (existing != null) {
                StatusBadge(if (existing.status == "COLLECTED") cityText("collected") else cityText("exceptions"), if (existing.status == "COLLECTED") "positive" else "warning")
                Text(DemoScenario.timestamp(existing.timestamp))
                existing.reasonNotCollected?.let { Text(it) }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("COLLECTED" to cityText("collected"), "MIXED_WASTE" to cityText("mixed"), "NOT_COLLECTED" to cityText("notCollected")).forEach { (key,label) ->
                        ChoiceChip(label, status == key, { status = key })
                    }
                }
                if (status == "COLLECTED") {
                    SectionTitle(cityText("quality"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("GOOD" to cityText("good"), "PARTIAL" to cityText("partial"), "MIXED" to cityText("mixed")).forEach { (key,label) -> ChoiceChip(label, quality == key, { quality = key }) }
                    }
                }
                val reasons = listOf(cityText("absent"), cityText("noWaste"), cityText("binMissing"), cityText("refused"))
                if (status == "NOT_COLLECTED") {
                    SectionTitle(cityText("reason"))
                    reasons.forEachIndexed { i, label -> ChoiceChip(label, reason == i, { reason = i }, Modifier.fillMaxWidth()) }
                }
                OperationFeedback(viewModel, "collection") { selectedId = null }
                CityButton(cityText("record"), {
                    viewModel.markCollection(household, status, if (status == "COLLECTED") quality else "MIXED",
                        if (status == "NOT_COLLECTED") reasons[reason] else null)
                }, loading = op.busy)
            }
        }
    }
    if (earnings) CitySheet(cityText("earnings"), { earnings = false }) {
        Text("₹730", style = MaterialTheme.typography.displayLarge)
        CityRecord(s.roleCollector, "₹450", Icons.Default.Badge)
        CityRecord(s.segregationScore, "₹180", Icons.Default.Eco)
        CityRecord(cityText("completed"), "₹100", Icons.Default.CheckCircle)
        DemoNote()
    }
}
@Composable
fun QrScannerDialog(households: List<HouseholdEntity>, onScanned: (String) -> Unit, onDismiss: () -> Unit) {
    CitySheet(cityText("simulateScan"), onDismiss) {
        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(48.dp), CityColors.primary)
        DemoNote()
        households.forEach { home -> CityRecord(home.qrCode, home.residentName, Icons.Default.Home, { onScanned(home.qrCode) }) { Icon(Icons.Default.ChevronRight, null) } }
        if (households.isEmpty()) EmptyState(cityText("noRecords"))
    }
}

