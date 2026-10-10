package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.CityModel
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*

@Composable
fun AdminScreen(viewModel: CityModel, households: List<HouseholdEntity>, violations: List<ViolationFineEntity>, modifier: Modifier = Modifier) {
    val s = appStrings()
    val feedback = LocalCityFeedback.current
    val clipboard = LocalClipboardManager.current
    var showTransactions by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var fineOpen by rememberSaveable { mutableStateOf(false) }
    var reminderId by rememberSaveable { mutableStateOf<Int?>(null) }
    var violator by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var violationType by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val brief by viewModel.aiSummary.collectAsStateWithLifecycle()
    val busyBrief by viewModel.isSummaryLoading.collectAsStateWithLifecycle()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    val pickups by viewModel.allPickupLogs.collectAsStateWithLifecycle()
    val notices by viewModel.demoNotices.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val holder = rememberSaveableStateHolder()
    BackHandler(tab != 0) { tab = 0 }
    Column(modifier.fillMaxSize()) {
        WorkspaceTabs(listOf(cityText("overview"), cityText("wards"), cityText("finance"), cityText("fines")),
            listOf(Icons.Default.Insights, Icons.Default.Map, Icons.Default.AccountBalance, Icons.Default.Gavel), tab, { tab = it }, "admin_tab")
        CityTransition(tab, Modifier.weight(1f), direction = { from, to -> to.compareTo(from) }) { activeTab ->
        holder.SaveableStateProvider(activeTab) {
            when(activeTab) {
                0 -> CityPage(s.roleAdmin, DemoScenario.date()) {
                    item { AdaptiveColumns(primary = {
                        CityPanel(color = CityColors.positiveBg) {
                            StatusBadge(cityText("today"))
                            MotionNumber(stats.coveragePercent.toInt(), "%", MaterialTheme.typography.displayLarge)
                            Text(cityText("coverage"), style = MaterialTheme.typography.titleMedium)
                            ProgressLine((stats.coveragePercent / 100).toFloat())
                            Text(stats.collectedToday.toString() + " / " + stats.totalHouseholds + " · " + cityText("collected"), color = CityColors.muted)
                        }
                        MetricPair("%.0f%%".format(stats.segregationRate), s.segregationScore, "%.1f t".format(stats.wasteRecoveredTons), cityText("net"))
                    }, secondary = {
                        CityPanel {
                            SectionTitle(cityText("brief"))
                            if (brief != null) Text(brief!!, style = MaterialTheme.typography.bodyMedium)
                            else Text(s.roleAdminSubtitle, color = CityColors.muted)
                            CityButton(cityText("brief"), viewModel::generateExecutiveSummary, loading = busyBrief, icon = Icons.Default.AutoAwesome)
                        }
                    }) }
                    item { MetricPair(stats.openComplaints.toString(), s.filterPending, "%.0f%%".format(stats.costRecoveryPercent), cityText("recovery")) }
                }
                1 -> CityPage(cityText("wards"), cityText("coverage")) {
                    if (households.isEmpty()) item { EmptyState(cityText("noRecords")) }
                    items(households.groupBy { it.ward }.entries.toList(), key = { it.key }) { (ward, homes) ->
                        val ids = homes.map { it.id }.toSet()
                        val collected = DemoScenario.today(pickups).count { it.householdId in ids && it.status == "COLLECTED" }
                        CityPanel {
                            Text(ward, style = MaterialTheme.typography.titleLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Metric(collected.toString() + "/" + homes.size, cityText("collected"), Modifier.weight(1f))
                                Metric("%.0f%%".format(homes.map { it.segregationScore }.average()), s.segregationScore, Modifier.weight(1f), Icons.Default.Eco)
                            }
                            ProgressLine(collected.toFloat()/homes.size.coerceAtLeast(1))
                        }
                    }
                }
                2 -> CityPage(cityText("finance"), DemoScenario.period()) {
                    item {
                        val received = payments.filter { it.monthYear == DemoScenario.period() }.distinctBy { it.householdId }.sumOf { it.amount }
                        MetricPair(DemoScenario.money(received), cityText("recovery"), DemoScenario.money(households.filter { it.feeStatus != "PAID" }.sumOf { it.monthlyFee }), s.currentOutstanding)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(s.due, !showTransactions, { showTransactions = false })
                            ChoiceChip(cityText("transactions"), showTransactions, { showTransactions = true })
                        }
                    }
                    if (showTransactions) {
                        items(payments, key = { "payment-" + it.id }) { payment ->
                            CityRecord(households.firstOrNull { it.id == payment.householdId }?.residentName.orEmpty(),
                                payment.monthYear + " · " + payment.receiptNumber, Icons.AutoMirrored.Filled.ReceiptLong) {
                                Text(DemoScenario.money(payment.amount), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    val due = households.filter { it.feeStatus != "PAID" }
                    if (!showTransactions && due.isEmpty()) item { EmptyState(s.paid, Icons.Default.CheckCircle) }
                    items(if (showTransactions) emptyList() else due, key = { it.id }) { home ->
                        CityPanel {
                            PersonRecord(home.residentName, home.address)
                            Text(DemoScenario.money(home.monthlyFee), style = MaterialTheme.typography.titleLarge)
                            CityOutlineButton({ reminderId = home.id }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(if ("reminder-" + home.id in notices) cityText("demoRecorded") else cityText("reminder"))
                            }
                        }
                    }
                }
                else -> CityPage(cityText("fines")) {
                    item { CityButton(cityText("record"), { viewModel.clearOperation(); attempted = false; fineOpen = true }, icon = Icons.Default.Add) }
                    if (violations.isEmpty()) item { EmptyState(cityText("noRecords")) }
                    items(violations, key = { it.id }) { fine ->
                        CityPanel {
                            Text(fine.violatorName, style = MaterialTheme.typography.titleLarge)
                            Text(fine.violationType + " · " + fine.location, color = CityColors.muted)
                            Text(DemoScenario.money(fine.fineAmount), style = MaterialTheme.typography.headlineMedium)
                            StatusBadge(if (fine.status == "PAID") s.paid else s.due, if (fine.status == "PAID") "positive" else "warning")
                        }
                    }
                }
            }
        }
        }
    }
    households.firstOrNull { it.id == reminderId }?.let { home ->
        CitySheet(cityText("reminder"), { reminderId = null }) {
            Text(home.residentName, style = MaterialTheme.typography.titleLarge)
            Text(home.phone)
            Text(s.monthlyFee + " · " + DemoScenario.money(home.monthlyFee))
            val reminderText = home.residentName + " · " + s.monthlyFee + ": " + DemoScenario.money(home.monthlyFee) + " · " + DemoScenario.period()
            CityPanel { Text(reminderText) }
            CityButton(cityText("copyReminder"), {
                clipboard.setText(AnnotatedString(reminderText)); viewModel.acknowledgeDemo("reminder-" + home.id); feedback(CityFeedback.CONFIRM); reminderId = null
            }, icon = Icons.Default.ContentCopy)
        }
    }
    if (fineOpen) CitySheet(cityText("fines"), { if (!op.busy) fineOpen = false }) {
        val value = amount.toDoubleOrNull()
        val valid = violator.isNotBlank() && location.isNotBlank() && violationType.isNotBlank() && value != null && value.isFinite() && value > 0
        CityField(violator, { violator = it.take(100) }, cityText("violator"), error = attempted && violator.isBlank())
        CityField(location, { location = it.take(200) }, s.streetAddressLabel, error = attempted && location.isBlank())
        CityField(violationType, { violationType = it.take(100) }, s.wasteCategoryTitle, error = attempted && violationType.isBlank())
        CityField(amount, { amount = it.take(12) }, cityText("amount"), keyboard = KeyboardType.Decimal, error = attempted && !valid)
        if (attempted && !valid) Text(cityText("checkFields"), color = CityColors.error)
        OperationFeedback(viewModel, "fine") { fineOpen = false; violator = ""; location = ""; violationType = ""; amount = "" }
        CityButton(cityText("record"), {
            attempted = true
            if (valid) viewModel.issueSpotFine(violator, location, violationType, value!!)
        }, loading = op.busy)
    }
}

