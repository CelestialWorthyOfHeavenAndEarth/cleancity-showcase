package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.HouseholdEntity
import com.example.util.*
import com.example.ui.theme.CityColors
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HouseholdQrCard(home: HouseholdEntity) {
    val context = LocalContext.current
    val bitmap = remember(home.qrCode) { HouseholdQr.bitmap(home.qrCode) }
    var failed by remember { mutableStateOf(false) }
    Surface(color = CityColors.forest, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(home.residentName, style = MaterialTheme.typography.titleLarge, color = CityColors.onForest)
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                Image(bitmap.asImageBitmap(), appStrings().householdQrCode + " · " + home.qrCode,
                    Modifier.fillMaxWidth().widthIn(max = 300.dp).aspectRatio(1f).padding(8.dp))
            }
            Text(home.qrCode, style = MaterialTheme.typography.titleLarge, color = CityColors.lime)
            Text(home.address, style = MaterialTheme.typography.bodySmall, color = CityColors.onForest)
        }
    }
    Text(cityText("qrHint"), style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
    CityButton(cityText("shareQr"), { failed = runCatching { HouseholdQr.share(context, home) }.isFailure }, icon = Icons.Default.Share)
    if (failed) Text(cityText("saveError"), color = CityColors.error)
}

@Composable
fun HouseholdScannerSheet(households: List<HouseholdEntity>, onFound: (HouseholdEntity) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val prompt = cityText("scanHint")
    fun accept(raw: String?) {
        val normalized = raw?.let(HouseholdQr::normalize)
        val home = households.firstOrNull { it.qrCode.equals(normalized, ignoreCase = true) }
        if (home == null) error = true else { error = false; onFound(home) }
    }
    val camera = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) accept(result.contents)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val value = withContext(Dispatchers.IO) { runCatching { HouseholdQr.readImage(context, uri) }.getOrNull() }
            busy = false; accept(value)
        }
    }
    CitySheet(cityText("scanQr"), onDismiss) {
        Surface(color = CityColors.positiveBg, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.QrCodeScanner, null, Modifier.size(64.dp), CityColors.primary)
                Text(prompt, style = MaterialTheme.typography.titleMedium)
            }
        }
        CityButton(cityText("openCamera"), {
            error = false
            runCatching { camera.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(prompt).setBeepEnabled(false).setOrientationLocked(false)) }.onFailure { error = true }
        }, enabled = !busy, icon = Icons.Default.CameraAlt)
        OutlinedButton({ gallery.launch("image/*") }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy) {
            Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text(cityText("readQrImage"))
        }
        HorizontalDivider(color = CityColors.line)
        CityField(code, { code = it.take(80); error = false }, cityText("householdId"), error = error, enabled = !busy)
        if (error) Text(cityText("unknownQr"), color = CityColors.error)
        CityButton(cityText("openHousehold"), { accept(code) }, enabled = code.isNotBlank(), loading = busy, icon = Icons.Default.Home)
    }
}
