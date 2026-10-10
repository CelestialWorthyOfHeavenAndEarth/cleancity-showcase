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
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var showCode by rememberSaveable { mutableStateOf(false) }
    var earnings by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("COLLECTED") }
    var quality by rememberSaveable { mutableStateOf("GOOD") }
    var reason by rememberSaveable { mutableIntStateOf(0) }
    fun select(id: Int) { viewModel.clearOperation(); selectedId = id; status = "COLLECTED"; quality = "GOOD"; reason = 0; showCode = false }
    CityDetailHost(selectedId, { selectedId = null }, modifier, canClose = !op.busy, list = {
        CityPage(cityText("route"), CityFixtures.collector + " · " + DemoScenario.date()) {
            item {
                AdaptiveColumns(primary = {
                    CityPanel(color = CityColors.positiveBg) {
                        StatusBadge(cityText("today"))
                        MotionNumber(recorded.size, " / " + households.size, MaterialTheme.typography.displayLarge)
                        Text(cityText("stops") + " · " + cityText("completed"), color = CityColors.muted)
                        ProgressLine(if (households.isEmpty()) 0f else recorded.size.toFloat()/households.size)
                        CityButton(cityText("scanQr"), { scanner = true }, icon = Icons.Default.QrCodeScanner)
                    }
                }, secondary = {
                    MetricPair(collected.toString(), cityText("collected"), (today.size - collected).toString(), cityText("exceptions"))
                    CityOutlineButton({ earnings = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Default.AccountBalanceWallet, null); Spacer(Modifier.width(8.dp)); Text(cityText("earnings")) }
                })
            }
            item {
                SectionTitle(cityText("stops"))
                CityField(query, { query = it }, cityText("searchHomes"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(s.filterAll, cityText("remaining"), cityText("completed")).forEachIndexed { i, label -> ChoiceChip(label, filter == i, { filter = i }) }
                }
            }
            if (households.isEmpty()) item { EmptyState(cityText("noRecords")) }
            val visible = households.filter { home ->
                (home.residentName.contains(query, true) || home.address.contains(query, true)) &&
                    (filter == 0 || (filter == 1 && home.id !in recorded) || (filter == 2 && home.id in recorded))
            }
            if (visible.isEmpty() && households.isNotEmpty()) item { EmptyState(cityText("noRecords")) }
            items(visible, key = { it.id }) { home ->
                val log = today.firstOrNull { it.householdId == home.id }
                PersonRecord(home.residentName, home.address, { select(home.id) }, Modifier.animateItem().cityShared("household-" + home.id)) {
                    if (log == null) Icon(Icons.Default.ChevronRight, cityText("record"), tint = CityColors.primary)
                    else Icon(if (log.status == "COLLECTED") Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null,
                        tint = if (log.status == "COLLECTED") CityColors.primary else CityColors.warning)
                }
            }
        }
    }, detail = { id ->
        households.firstOrNull { it.id == id }?.let { household ->
            val existing = today.firstOrNull { it.householdId == household.id }
            CityDetailPage("household-" + household.id, household.residentName, { if (!op.busy) selectedId = null }) {
                Text(household.address, color = CityColors.muted)
                OperationFeedback(viewModel, "collection") { selectedId = null }
                TextButton({ showCode = !showCode }) { Icon(Icons.Default.QrCode2, null); Spacer(Modifier.width(8.dp)); Text(s.householdQrCode) }
                if (showCode) HouseholdQrCard(household)
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
                    CityButton(cityText("record"), {
                        viewModel.markCollection(household, status, if (status == "COLLECTED") quality else "MIXED",
                            if (status == "NOT_COLLECTED") reasons[reason] else null)
                    }, loading = op.busy)
                }
            }
        }
    })
    if (scanner) HouseholdScannerSheet(households, { home -> scanner = false; select(home.id) }, { scanner = false })
    if (earnings) CitySheet(cityText("earnings"), { earnings = false }) {
        Text("₹730", style = MaterialTheme.typography.displayLarge)
        CityRecord(s.roleCollector, "₹450", Icons.Default.Badge)
        CityRecord(s.segregationScore, "₹180", Icons.Default.Eco)
        CityRecord(cityText("completed"), "₹100", Icons.Default.CheckCircle)
    }
}
