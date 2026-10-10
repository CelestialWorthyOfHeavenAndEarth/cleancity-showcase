package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.CityModel
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverScreen(viewModel: CityModel, weighbridgeLogs: List<WeighbridgeLogEntity>, modifier: Modifier = Modifier) {
    val s = appStrings()
    val feedback = LocalCityFeedback.current
    val stops by viewModel.completedDriverStops.collectAsStateWithLifecycle()
    val notices by viewModel.demoNotices.collectAsStateWithLifecycle()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    var showWeight by rememberSaveable { mutableStateOf(false) }
    var showSos by rememberSaveable { mutableStateOf(false) }
    var gross by rememberSaveable { mutableStateOf("") }
    var tare by rememberSaveable { mutableStateOf("") }
    var vehicle by rememberSaveable { mutableStateOf(DemoScenario.vehicle) }
    var plant by rememberSaveable { mutableStateOf("City recovery centre") }
    var waste by rememberSaveable { mutableIntStateOf(0) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val locations = CityFixtures.routeStops
    CityPage(cityText("vehicle"), CityFixtures.driver + " · " + DemoScenario.vehicle, modifier) {
        item { AdaptiveColumns(primary = {
            CityPanel(color = CityColors.positiveBg) {
                Icon(Icons.Default.LocalShipping, null, Modifier.size(32.dp), CityColors.primary)
                Text(DemoScenario.vehicle, style = MaterialTheme.typography.headlineLarge)
                Text(DemoScenario.ward, color = CityColors.muted)
                ProgressLine(stops.size.toFloat() / locations.size)
                MotionNumber(stops.size, " / " + locations.size + " · " + cityText("completed"), MaterialTheme.typography.titleMedium)
                CityButton(cityText("weights"), { viewModel.clearOperation(); attempted = false; showWeight = true }, icon = Icons.Default.Scale)
                CityOutlineButton({ showSos = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.WarningAmber, null); Spacer(Modifier.width(8.dp)); Text(if ("sos" in notices) cityText("demoRecorded") else cityText("sos"))
                }
            }
        }, secondary = { RouteIllustration(stops.size, locations.size) }) }
        item { SectionTitle(cityText("stops")) }
        items(locations.size) { index ->
            CityRecord(locations[index], "1100 L · " + if (index % 2 == 0) s.wetWaste else s.dryWaste, Icons.Default.DeleteOutline) {
                IconButton({ viewModel.completeDriverStop(index); feedback(CityFeedback.CONFIRM) }, enabled = index !in stops) {
                    if (index in stops) SuccessMark()
                    else Icon(Icons.Default.AddCircleOutline, cityText("record"), tint = CityColors.primary)
                }
            }
        }
        item { SectionTitle(cityText("weights")) }
        if (weighbridgeLogs.isEmpty()) item { EmptyState(cityText("noRecords")) }
        items(weighbridgeLogs, key = { it.id }) { log ->
            CityRecord(DemoScenario.money(log.netWeightKg).removePrefix("₹") + " kg", log.vehicleNumber + " · " + DemoScenario.timestamp(log.timestamp), Icons.Default.Scale)
        }
    }
    if (showSos) CitySheet(cityText("sos"), { showSos = false }) {
        Text(DemoScenario.vehicle, style = MaterialTheme.typography.headlineMedium)
        Text(DemoScenario.ward)
        ContextNote(cityText("issueHint"))
        CityButton(cityText("confirm"), { viewModel.acknowledgeDemo("sos"); feedback(CityFeedback.CONFIRM); showSos = false })
    }
    if (showWeight) CitySheet(cityText("weights"), { if (!op.busy) showWeight = false }) {
        val grossValue = gross.toDoubleOrNull()
        val tareValue = tare.toDoubleOrNull()
        val valid = grossValue != null && tareValue != null && grossValue.isFinite() && tareValue.isFinite() && tareValue > 0 && grossValue > tareValue && vehicle.isNotBlank() && plant.isNotBlank()
        CityField(vehicle, { vehicle = it.take(30) }, cityText("vehicle"))
        CityField(gross, { gross = it.take(12) }, cityText("gross"), keyboard = KeyboardType.Decimal, error = attempted && !valid)
        CityField(tare, { tare = it.take(12) }, cityText("tare"), keyboard = KeyboardType.Decimal, error = attempted && !valid)
        CityField(plant, { plant = it.take(100) }, cityText("plant"))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(s.wetWaste, s.dryWaste, s.sanitaryWaste).forEachIndexed { i, label -> ChoiceChip(label, waste == i, { waste = i }) }
        }
        if (valid) StatusBadge(cityText("net") + " · " + "%.1f".format(grossValue!! - tareValue!!) + " kg")
        if (attempted && !valid) Text(cityText("checkFields"), color = CityColors.error)
        OperationFeedback(viewModel, "weight") { showWeight = false; gross = ""; tare = "" }
        CityButton(cityText("record"), {
            attempted = true
            if (valid) viewModel.submitWeighbridgeEntry(vehicle, CityFixtures.driver, listOf("Wet waste", "Dry recyclable", "Hazardous")[waste], grossValue!!, tareValue!!, plant)
        }, loading = op.busy)
    }
}

