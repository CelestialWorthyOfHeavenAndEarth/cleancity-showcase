package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.CityModel
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportWasteScreen(viewModel: CityModel, complaints: List<ComplaintEntity>, modifier: Modifier = Modifier) {
    val s = appStrings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabState = rememberSaveableStateHolder()
    var category by rememberSaveable { mutableIntStateOf(0) }
    var description by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var longitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var mediaBusy by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var success by rememberSaveable { mutableStateOf(false) }
    val photoError = cityText("photoError")
    val locationError = cityText("locationError")
    val categories = listOf(s.catOverflowingBin, s.catStreetLitter, s.catConstructionDebris, s.catDrainClogged)
    val categoryIds = listOf("OVERFLOW", "LITTER", "CONSTRUCTION", "DRAIN")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            mediaBusy = true
            val result = withContext(Dispatchers.IO) { FileStorageHelper.copyUriToInternal(context, uri) }
            if (result != null) { photo = result; error = null } else error = photoError
            mediaBusy = false
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) scope.launch {
            mediaBusy = true
            val result = withContext(Dispatchers.IO) { FileStorageHelper.saveBitmap(context, bitmap) }
            if (result != null) { photo = result; error = null } else error = photoError
            mediaBusy = false
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching { camera.launch(null) }.onFailure { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        else picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val fetchLocation: () -> Unit = {
        scope.launch {
            mediaBusy = true
            try {
                val result = withTimeoutOrNull(10000) { LocationHelper.getCurrentLocation(context) }
                if (result == null || result.isSample) error = locationError
                else { address = result.address; latitude = result.latitude; longitude = result.longitude; error = null }
            } finally { mediaBusy = false }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) fetchLocation() else error = locationError
    }
    val filtered = complaints.filter { when (filter) { "OPEN" -> it.status != "RESOLVED"; "RESOLVED" -> it.status == "RESOLVED"; else -> true } }
    CityDetailHost(detailId, { detailId = null }, modifier, canClose = !op.busy, list = {
        Column(Modifier.fillMaxSize()) {
            WorkspaceTabs(listOf(cityText("newReport"), cityText("reports")), listOf(Icons.Default.AddAPhoto, Icons.AutoMirrored.Filled.FormatListBulleted), tab, { tab = it }, "report_tab")
            CityTransition(tab, Modifier.weight(1f), direction = { from, to -> to.compareTo(from) }) { activeTab ->
                tabState.SaveableStateProvider(activeTab) {
                    if (activeTab == 0) CityPage(cityText("newReport"), cityText("reportIntro")) {
                        item {
                            CityPanel {
                                SectionHeading(1, s.photoEvidenceTitle)
                                Text(cityText("photoOptional"), style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
                                if (photo != null) {
                                    AsyncImage(File(photo!!), s.photoAddedBadge, Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
                                    TextButton({ photo = null }) { Icon(Icons.Default.Close, null); Text(s.changePhoto) }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CityOutlineButton({ cameraPermission.launch(Manifest.permission.CAMERA) }, enabled = !mediaBusy && !op.busy) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text(s.takePhoto) }
                                    CityOutlineButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !mediaBusy && !op.busy) { Text(s.uploadGallery) }
                                }
                            }
                        }
                        item {
                            CityPanel {
                                SectionHeading(2, s.gpsLocationTitle)
                                CityField(address, { address = it.take(200); latitude = 0.0; longitude = 0.0 }, s.streetAddressLabel, multiline = true, error = submitted && address.isBlank())
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton({
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (granted) fetchLocation() else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }, enabled = !mediaBusy && !op.busy) { Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(8.dp)); Text(s.refreshLocation) }
                                    TextButton({
                                        address = viewModel.registeredHousehold.value?.address.orEmpty(); latitude = 0.0; longitude = 0.0; error = null
                                    }, enabled = !op.busy) { Text(cityText("useSample")) }
                                }
                                if (mediaBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                                if (error != null) Text(error!!, color = CityColors.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        item {
                            CityPanel {
                                SectionHeading(3, s.descriptionTitle)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) { categories.forEachIndexed { i, label -> ChoiceChip(label, category == i, { category = i }) } }
                                CityField(description, { description = it.take(1000) }, s.descriptionTitle, multiline = true, error = submitted && description.isBlank(), enabled = !op.busy)
                                if (submitted && (address.isBlank() || description.isBlank())) Text(s.fillAllFieldsError, color = CityColors.error)
                                OperationFeedback(viewModel, "report") {
                                    description = ""; photo = null; submitted = false; success = true; tab = 1
                                }
                                CityButton(s.submitReport, {
                                    submitted = true
                                    if (description.isNotBlank() && address.isNotBlank()) viewModel.submitWasteReport(categories[category], categoryIds[category], description, address, latitude, longitude, photo)
                                }, loading = op.busy, enabled = !mediaBusy, icon = Icons.AutoMirrored.Filled.Send)
                            }
                        }
                    } else CityPage(s.myReportsTitle) {
                        if (success) item { StatusBadge(s.reportSubmittedSuccess) }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("ALL" to s.filterAll, "OPEN" to s.filterPending, "RESOLVED" to s.filterResolved).forEach { (key,label) -> ChoiceChip(label, filter == key, { filter = key }) }
                            }
                        }
                        if (filtered.isEmpty()) item { EmptyState(s.noReports) }
                        items(filtered, key = { it.id }) { report -> WasteReportCard(report, { viewModel.clearOperation(); detailId = report.id }, Modifier.animateItem()) }
                    }
                }
            }
        }
    }, detail = { id ->
        complaints.firstOrNull { it.id == id }?.let { report ->
            ReportDetailScreen(report, { detailId = null }, viewModel, citizen = true)
        }
    })
}
@Composable
fun reportStatus(status: String): String = when(status) {
    "RESOLVED" -> cityText("reportResolved")
    "IN_PROGRESS" -> cityText("reportProgress")
    "ESCALATED" -> cityText("escalated")
    else -> cityText("reportOpen")
}
@Composable
fun WasteReportCard(report: ComplaintEntity, onResolve: () -> Unit, modifier: Modifier = Modifier) {
    CityPanel(modifier.cityShared("report-" + report.id)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#" + report.id.toString().padStart(4, '0'), style = MaterialTheme.typography.labelLarge, color = CityColors.muted)
            StatusBadge(reportStatus(report.status), if (report.status == "RESOLVED") "positive" else "warning")
        }
        Text(report.title, style = MaterialTheme.typography.titleLarge)
        Text(report.address, color = CityColors.muted, style = MaterialTheme.typography.bodySmall)
        Text(DemoScenario.timestamp(report.createdAt), color = CityColors.muted, style = MaterialTheme.typography.bodySmall)
        CityOutlineButton(onResolve, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(cityText("overview")); Icon(Icons.Default.ChevronRight, null) }
    }
}
@Composable
fun ReportDetailScreen(report: ComplaintEntity, onDismiss: () -> Unit, viewModel: CityModel, citizen: Boolean = false) {
    val s = appStrings()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by rememberSaveable(report.id) { mutableStateOf("") }
    var proofPath by rememberSaveable(report.id) { mutableStateOf<String?>(null) }
    var proofBusy by remember { mutableStateOf(false) }
    var proofError by remember { mutableStateOf(false) }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            proofBusy = true
            proofPath = withContext(Dispatchers.IO) { FileStorageHelper.copyUriToInternal(context, uri) }
            proofBusy = false; proofError = proofPath == null
        }
    }
    CityDetailPage("report-" + report.id, "#" + report.id + " · " + reportStatus(report.status), { if (!op.busy) onDismiss() }) {
        OperationFeedback(viewModel, "resolve") { onDismiss() }
        OperationFeedback(viewModel, "escalate") { onDismiss() }
        Text(report.title, style = MaterialTheme.typography.headlineMedium)
        Text(report.description)
        report.photoUri?.takeIf { File(it).exists() }?.let { AsyncImage(File(it), s.photoEvidenceTitle, Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop) }
        CityRecord(report.address, report.ward, Icons.Default.Place)
        CityRecord(cityText("reportOpen"), DemoScenario.timestamp(report.createdAt), Icons.Default.Schedule)
        if (report.status == "RESOLVED") {
            CityRecord(cityText("reportResolved"), report.resolvedAt?.let(DemoScenario::timestamp).orEmpty(), Icons.Default.CheckCircle)
            val proof = report.resolutionProofPhoto
            if (proof != null && !proof.startsWith("demo:") && File(proof).isFile) {
                AsyncImage(File(proof), s.photoEvidenceTitle, Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
            } else if (proof?.startsWith("note:") == true) ContextNote(proof.removePrefix("note:"))
            else ContextNote(cityText("sampleProof"))
        } else {
            CityRecord(cityText("sla"), DemoScenario.timestamp(report.slaDeadlineMillis), Icons.Default.Timer)
            if (!citizen) {
                CityField(note, { note = it.take(1000) }, cityText("inspectNote"), multiline = true)
                CityOutlineButton({ proofPicker.launch("image/*") }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !proofBusy && !op.busy) {
                    Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text(cityText("addProof"))
                }
                proofPath?.let { AsyncImage(File(it), cityText("addProof"), Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Crop) }
                if (proofBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (proofError) Text(cityText("photoError"), color = CityColors.error)
                CityOutlineButton({ viewModel.escalateComplaint(report.id) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !op.busy && report.status != "ESCALATED") { Text(cityText("escalate")) }
            }
            CityButton(s.markResolved, { if (citizen) viewModel.markWasteReportResolved(report.id) else viewModel.resolveComplaint(report.id, proofPath ?: ("note:" + note.trim())) }, loading = op.busy || proofBusy, enabled = citizen || proofPath != null || note.trim().length >= 5)
        }
    }
}

