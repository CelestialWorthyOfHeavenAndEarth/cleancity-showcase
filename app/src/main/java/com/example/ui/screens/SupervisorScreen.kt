package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
                        0 -> CityPage(s.roleSupervisor, DemoScenario.ward) {
                            item { AdaptiveColumns(primary = { RouteIllustration(crew.count { it.status == "ON_ROUTE" }, crew.size) }, secondary = {
                                MetricPair(complaints.count { it.status != "RESOLVED" }.toString(), s.filterPending,
                                    crew.count { it.ppeChecked }.toString() + "/" + crew.size, cityText("safety"))
                                CityButton(cityText("reports"), { tab = 1 }, icon = Icons.AutoMirrored.Filled.Assignment)
                            }) }
                            if ("sos" in notices) item { CityPanel(color = CityColors.warningBg) { Text(cityText("sos"), style = MaterialTheme.typography.titleMedium); Text(DemoScenario.vehicle); ContextNote(cityText("demoRecorded")) } }
                            item { SectionTitle(cityText("crew")) }
                            items(crew, key = { it.id }) { member -> PersonRecord(member.workerName, member.role + " · " + member.checkInTime) }
                        }
                        1 -> CityPage(cityText("reports"), cityText("sla") + " · 24 h") {
                            item {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Column {
                                    PersonRecord(member.workerName, member.role + " · " + member.checkInTime)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cityText("safety"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                        Switch(member.ppeChecked, { viewModel.toggleCrewPpe(member) }, enabled = !op.busy)
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

