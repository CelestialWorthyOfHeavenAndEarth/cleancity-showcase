package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val locations = listOf("Road 10 · Community entrance", "Road 12 · Neighborhood park", "Road 36 · Market square", "Road 45 · Transit stop")
    CityPage(cityText("vehicle"), "Manjunath · " + DemoScenario.vehicle, modifier) {
        item { AdaptiveColumns(primary = {
            CityPanel(color = CityColors.positiveBg) {
                Icon(Icons.Default.LocalShipping, null, Modifier.size(32.dp), CityColors.primary)
                Text(DemoScenario.vehicle, style = MaterialTheme.typography.headlineLarge)
                Text(DemoScenario.ward, color = CityColors.muted)
                ProgressLine(stops.size / 4f)
                Text(stops.size.toString() + " / 4 · " + cityText("completed"))
                CityButton(cityText("weights"), { viewModel.clearOperation(); attempted = false; showWeight = true }, icon = Icons.Default.Scale)
                OutlinedButton({ showSos = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.WarningAmber, null); Spacer(Modifier.width(8.dp)); Text(if ("sos" in notices) cityText("demoRecorded") else cityText("sos"))
                }
            }
        }, secondary = { RouteIllustration(stops.size, 4) }) }
        item { SectionTitle(cityText("stops")) }
        items(4) { index ->
            CityRecord(locations[index], "1100 L · " + if (index % 2 == 0) s.wetWaste else s.dryWaste, Icons.Default.DeleteOutline) {
                IconButton({ viewModel.completeDriverStop(index) }, enabled = index !in stops) {
                    Icon(if (index in stops) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        if (index in stops) cityText("completed") else cityText("record"), tint = CityColors.primary)
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
        DemoNote()
        CityButton(cityText("confirm"), { viewModel.acknowledgeDemo("sos"); showSos = false })
    }
    if (showWeight) CitySheet(cityText("weights"), { if (!op.busy) showWeight = false }) {
        val grossValue = gross.toDoubleOrNull()
        val tareValue = tare.toDoubleOrNull()
        val valid = grossValue != null && tareValue != null && grossValue.isFinite() && tareValue.isFinite() && tareValue > 0 && grossValue > tareValue && vehicle.isNotBlank() && plant.isNotBlank()
        CityField(vehicle, { vehicle = it.take(30) }, cityText("vehicle"))
        CityField(gross, { gross = it.take(12) }, cityText("gross"), keyboard = KeyboardType.Decimal, error = attempted && !valid)
        CityField(tare, { tare = it.take(12) }, cityText("tare"), keyboard = KeyboardType.Decimal, error = attempted && !valid)
        CityField(plant, { plant = it.take(100) }, cityText("plant"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(s.wetWaste, s.dryWaste, s.sanitaryWaste).forEachIndexed { i, label -> ChoiceChip(label, waste == i, { waste = i }) }
        }
        if (valid) StatusBadge(cityText("net") + " · " + "%.1f".format(grossValue!! - tareValue!!) + " kg")
        if (attempted && !valid) Text(cityText("checkFields"), color = CityColors.error)
        OperationFeedback(viewModel, "weight") { showWeight = false; gross = ""; tare = "" }
        CityButton(cityText("record"), {
            attempted = true
            if (valid) viewModel.submitWeighbridgeEntry(vehicle, "Manjunath", listOf("Wet waste", "Dry recyclable", "Hazardous")[waste], grossValue!!, tareValue!!, plant)
        }, loading = op.busy)
    }
}

