package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
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
fun SupervisorScreen(viewModel: CityModel, complaints: List<ComplaintEntity>, crew: List<CrewAttendanceEntity>, modifier: Modifier = Modifier) {
    val s = appStrings()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var statusFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    val holder = rememberSaveableStateHolder()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    val notices by viewModel.demoNotices.collectAsStateWithLifecycle()
    BackHandler(tab != 0) { tab = 0 }
    CityDetailHost(selectedId, { selectedId = null }, modifier, canClose = !op.busy, list = {
        Column(Modifier.fillMaxSize()) {
            WorkspaceTabs(listOf(cityText("overview"), cityText("reports"), cityText("crew")), listOf(Icons.Default.Map, Icons.AutoMirrored.Filled.Assignment, Icons.Default.Groups), tab, { tab = it }, "supervisor_tab")
            CityTransition(tab, Modifier.weight(1f), direction = { from, to -> to.compareTo(from) }) { activeTab ->
                holder.SaveableStateProvider(activeTab) {
                    when(activeTab) {
                        0 -> CityPage(s.roleSupervisor, AndhraPradeshLocations.region) {
                            item { AdaptiveColumns(primary = { RouteIllustration(crew.count { it.status == "ON_ROUTE" }, crew.size) }, secondary = {
                                MetricPair(complaints.count { it.status != "RESOLVED" }.toString(), s.filterPending,
                                    crew.count { it.ppeChecked }.toString() + "/" + crew.size, cityText("safety"))
                                CityButton(cityText("reports"), { tab = 1 }, icon = Icons.AutoMirrored.Filled.Assignment)
                            }) }
                            if ("sos" in notices) item { CityPanel(color = CityColors.warningBg) { Text(cityText("sos"), style = MaterialTheme.typography.titleMedium); Text(DemoScenario.vehicle); ContextNote(cityText("demoRecorded")) } }
                            item { SectionTitle(cityText("crew")) }
                            items(crew, key = { it.id }) { member ->
                                PersonRecord(member.workerName, member.role + " · " + member.checkInTime) {
                                    StatusBadge(
                                        if (member.ppeChecked) cityText("safety") else cityText("exceptions"),
                                        if (member.ppeChecked) "positive" else "warning"
                                    )
                                }
                            }
                        }
                        1 -> CityPage(cityText("reports"), cityText("sla") + " · 24 h") {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("ALL", "OPEN", "IN_PROGRESS", "ESCALATED", "RESOLVED").forEach { status ->
                                        ChoiceChip(if (status == "ALL") s.filterAll else reportStatus(status), statusFilter == status, { statusFilter = status })
                                    }
                                }
                            }
                            if (complaints.isEmpty()) item { EmptyState(s.noReports) }
                            items(complaints.filter { statusFilter == "ALL" || it.status == statusFilter }.sortedWith(compareBy<ComplaintEntity> { it.status == "RESOLVED" }.thenBy { it.slaDeadlineMillis }), key = { it.id }) { report ->
                                WasteReportCard(report, { viewModel.clearOperation(); selectedId = report.id }, Modifier.animateItem())
                            }
                        }
                        else -> CityPage(cityText("crew"), DemoScenario.date()) {
                            item { OperationFeedback(viewModel, "ppe") }
                            if (crew.isEmpty()) item { EmptyState(cityText("noRecords")) }
                            items(crew, key = { it.id }) { member ->
                                CityPanel {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(color = CityColors.positiveBg, shape = RoundedCornerShape(16.dp)) {
                                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    member.workerName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = CityColors.primary
                                                )
                                            }
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(member.workerName, style = MaterialTheme.typography.titleMedium, color = CityColors.ink)
                                            Text(member.role + " · " + member.checkInTime, style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
                                        }
                                    }
                                    HorizontalDivider(color = CityColors.line)
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                if (member.ppeChecked) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = if (member.ppeChecked) CityColors.primary else CityColors.warning,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                cityText("safety"),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = CityColors.ink
                                            )
                                        }
                                        Switch(
                                            checked = member.ppeChecked,
                                            onCheckedChange = { viewModel.toggleCrewPpe(member) },
                                            enabled = !op.busy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }, detail = { id ->
        complaints.firstOrNull { it.id == id }?.let { ReportDetailScreen(it, { selectedId = null }, viewModel) }
    })
}

