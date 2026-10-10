package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.CityModel
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*

@Composable
fun CitizenScreen(viewModel: CityModel, household: HouseholdEntity?, announcements: List<AnnouncementEntity>,
    complaints: List<ComplaintEntity>, payments: List<PaymentEntity>, modifier: Modifier = Modifier) {
    val s = appStrings()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    val pickups by viewModel.citizenPickups.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showTag by rememberSaveable { mutableStateOf(false) }
    var paymentOpen by rememberSaveable { mutableStateOf(false) }
    var receiptId by rememberSaveable { mutableStateOf<Int?>(null) }
    val stateHolder = rememberSaveableStateHolder()
    BackHandler(tab != 0) { tab = 0 }
    val labels = listOf(cityText("home"), s.tabReportWaste, cityText("sort"), cityText("assistant"), s.tabBillsUpi)
    val icons = listOf(Icons.Default.Home, Icons.Default.AddAPhoto, Icons.Default.Recycling, Icons.Default.AutoAwesome, Icons.AutoMirrored.Filled.ReceiptLong)
    val keyboard = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(modifier.fillMaxSize()) {
        Crossfade(tab, Modifier.weight(1f), tween(220), label = "Citizen destination") { index ->
            stateHolder.SaveableStateProvider(index) {
                when (index) {
                    0 -> DailyPickupTab(household, announcements, { showTag = true }, { tab = 1 }, { tab = 4 }, pickups)
                    1 -> ReportWasteScreen(viewModel, complaints)
                    2 -> SegregationGuideTab(household)
                    3 -> AiChatbotScreen(viewModel)
                    else -> BillsPaymentTab(household, payments, { _, _ -> viewModel.clearOperation(); paymentOpen = true }, { receiptId = it.id })
                }
            }
        }
        if (!keyboard) {
            HorizontalDivider(color = CityColors.line)
            NavigationBar(containerColor = CityColors.surface, tonalElevation = 0.dp, windowInsets = WindowInsets(0,0,0,0)) {
                labels.forEachIndexed { i, label ->
                    NavigationBarItem(tab == i, { tab = i }, { Icon(icons[i], null) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("citizen_tab_" + i))
                }
            }
        }
    }
    if (showTag && household != null) HouseholdQrTagDialog(household) { showTag = false }
    payments.firstOrNull { it.id == receiptId }?.let { payment -> PaymentReceiptDialog(payment, household) { receiptId = null } }
    if (paymentOpen && household != null) CitySheet(cityText("paymentRecord"), { if (!op.busy) paymentOpen = false }) {
        Text(DemoScenario.money(household.monthlyFee), style = MaterialTheme.typography.displayLarge)
        Text(DemoScenario.period() + " · " + household.residentName, color = CityColors.muted)
        ContextNote(cityText("demoPayment"))
        OperationFeedback(viewModel, "payment") { paymentOpen = false }
        CityButton(cityText("confirm"), { viewModel.payMonthlyFee(household.id, household.monthlyFee) }, loading = op.busy)
    }
}
@Composable
fun DailyPickupTab(household: HouseholdEntity?, announcements: List<AnnouncementEntity>, onOpenQrTag: () -> Unit,
    onReportWaste: (() -> Unit)? = null, onViewBills: (() -> Unit)? = null, pickups: List<PickupLogEntity> = emptyList()) {
    val s = appStrings()
    val today = DemoScenario.today(pickups).firstOrNull()
    var allPickups by rememberSaveable { mutableStateOf(false) }
    var allAnnouncements by rememberSaveable { mutableStateOf(false) }
    CityPage(cityText("homeTitle"), household?.residentName.orEmpty() + " · " + cityText("today")) {
        item {
            AdaptiveColumns(primary = {
                Surface(color = CityColors.forest, shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (today == null) cityText("sampleSchedule") else cityText("today"), style = MaterialTheme.typography.labelLarge, color = CityColors.onForest)
                        Text(if (today == null) "7:00 – 9:30 AM" else when(today.status) { "COLLECTED" -> s.truckCollected; else -> cityText("exceptions") },
                            style = MaterialTheme.typography.displayLarge, color = CityColors.onForest)
                        Text(if (today == null) s.wetWaste + " · " + s.dryWaste else DemoScenario.timestamp(today.timestamp),
                            style = MaterialTheme.typography.bodyMedium, color = CityColors.onForest)
                        NeighborhoodArt(Modifier.height(128.dp))
                        Button(onOpenQrTag, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = household != null,
                            colors = ButtonDefaults.buttonColors(containerColor = CityColors.onForest, contentColor = CityColors.forest), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.QrCode2, null); Spacer(Modifier.width(8.dp)); Text(s.householdQrCode)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (onReportWaste != null) OutlinedButton(onReportWaste, Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(s.tabReportWaste)
                    }
                    if (onViewBills != null) OutlinedButton(onViewBills, Modifier.weight(1f).heightIn(min = 52.dp)) { Text(s.tabBillsUpi) }
                }
            }, secondary = {
                CityPanel {
                    Text(household?.address.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(household?.ward.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Metric((household?.segregationScore ?: 0).toString() + "%", s.segregationScore, Modifier.weight(1f), Icons.Default.Eco)
                        Metric((household?.streakDays ?: 0).toString(), s.streakDays, Modifier.weight(1f), Icons.Default.LocalFireDepartment)
                    }
                }
            })
        }
        if (pickups.isNotEmpty()) {
            item { CollectionWeek(pickups) }
            item { SectionTitle(s.collectionHistory, if (allPickups) s.close else s.filterAll, { allPickups = !allPickups }) }
            items(if (allPickups) pickups else pickups.take(3), key = { "pickup-" + it.id }) { log ->
                CityRecord(if (log.status == "COLLECTED") cityText("collected") else cityText("exceptions"),
                    DemoScenario.timestamp(log.timestamp) + " · " + log.collectorName, Icons.Default.Recycling)
            }
        }
        item { SectionTitle(s.announcementsTitle, if (announcements.size > 2) s.filterAll else null, { allAnnouncements = !allAnnouncements }) }
        if (announcements.isEmpty()) item { EmptyState(cityText("noRecords")) }
        items(if (allAnnouncements) announcements else announcements.take(2), key = { "notice-" + it.id }) { notice ->
            CityPanel {
                StatusBadge(notice.category, if (notice.isUrgent) "warning" else "positive")
                Text(notice.title, style = MaterialTheme.typography.titleMedium)
                Text(notice.content, style = MaterialTheme.typography.bodyMedium, color = CityColors.muted)
                Text(notice.date, style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
            }
        }
    }
}
@Composable
fun SegregationGuideTab(household: HouseholdEntity?) {
    val s = appStrings()
    val titles = listOf(s.wetWaste, s.dryWaste, s.sanitaryWaste)
    val details = listOf(s.greenBinDesc, s.blueBinDesc, s.redBagDesc)
    val colors = listOf(CityColors.primary, CityColors.blue, CityColors.error)
    var expanded by rememberSaveable { mutableIntStateOf(0) }
    CityPage(cityText("sort"), cityText("sortIntro")) {
        items(3) { i ->
            Surface(onClick = { expanded = if (expanded == i) -1 else i }, color = CityColors.surface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        BinArt(colors[i])
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("0" + (i + 1), style = MaterialTheme.typography.labelLarge, color = colors[i])
                            Text(titles[i], style = MaterialTheme.typography.titleLarge)
                        }
                        Icon(if (expanded == i) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                    AnimatedVisibility(expanded == i) { Text(details[i], style = MaterialTheme.typography.bodyLarge, color = CityColors.muted) }
                }
            }
        }
        item { CityPanel(color = CityColors.positiveBg) {
            Icon(Icons.Default.Spa, null, tint = CityColors.primary)
            Text(s.promptHomeCompost, style = MaterialTheme.typography.titleMedium)
            Text(cityText("compostHelp"), style = MaterialTheme.typography.bodyMedium)
        } }
    }
}
@Composable
fun BillsPaymentTab(household: HouseholdEntity?, payments: List<PaymentEntity>, onPayFee: (Int, Double) -> Unit, onViewReceipt: (PaymentEntity) -> Unit) {
    val s = appStrings()
    val due = household?.feeStatus != "PAID"
    val scoped = payments.filter { it.householdId == household?.id }
    CityPage(s.tabBillsUpi, s.monthlyFee) {
        item {
            CityPanel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(DemoScenario.period(), style = MaterialTheme.typography.titleMedium)
                    StatusBadge(if (due) s.due else s.paid, if (due) "warning" else "positive")
                }
                Text(DemoScenario.money(if (due) household?.monthlyFee ?: 0.0 else 0.0), style = MaterialTheme.typography.displayLarge)
                Text(s.currentOutstanding, style = MaterialTheme.typography.bodyMedium, color = CityColors.muted)
                if (due) CityButton(cityText("paymentRecord"), { household?.let { onPayFee(it.id, it.monthlyFee) } }, enabled = household != null, icon = Icons.Default.Payment)
                else scoped.firstOrNull()?.let { payment -> CityButton(cityText("viewReceipt"), { onViewReceipt(payment) }, icon = Icons.AutoMirrored.Filled.ReceiptLong) }
            }
        }
        item { SectionTitle(s.transactionHistory) }
        if (scoped.isEmpty()) item { EmptyState(cityText("noRecords"), Icons.AutoMirrored.Filled.ReceiptLong) }
        items(scoped, key = { it.id }) { payment ->
            CityRecord(DemoScenario.money(payment.amount), payment.monthYear + " · " + payment.receiptNumber,
                Icons.AutoMirrored.Filled.ReceiptLong, { onViewReceipt(payment) }) { Icon(Icons.Default.ChevronRight, cityText("viewReceipt")) }
        }
    }
}
@Composable
fun HouseholdQrTagDialog(household: HouseholdEntity, onDismiss: () -> Unit) {
    CitySheet(appStrings().householdQrCode, onDismiss) { HouseholdQrCard(household) }
}

@Composable
fun PaymentReceiptDialog(payment: PaymentEntity, household: HouseholdEntity?, onDismiss: () -> Unit) {
    val s = appStrings()
    CitySheet(cityText("viewReceipt"), onDismiss) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), tint = CityColors.primary)
        Text(cityText("receiptTitle"), style = MaterialTheme.typography.headlineMedium)
        Text(DemoScenario.money(payment.amount), style = MaterialTheme.typography.displayLarge)
        CityPanel {
            Text(household?.residentName.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text(payment.monthYear)
            HorizontalDivider(color = CityColors.line)
            Text(payment.receiptNumber)
            Text(cityText("reference") + ": " + payment.upiRef, style = MaterialTheme.typography.bodySmall)
            Text(DemoScenario.timestamp(payment.paidAt), color = CityColors.muted)
        }
    }
}

