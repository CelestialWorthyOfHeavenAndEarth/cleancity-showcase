package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DemoScenario
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(viewModel: CityModel, modifier: Modifier = Modifier) {
    val s = appStrings()
    val language by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val op by viewModel.operation.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf<String?>(null) }
    var phone by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var ward by rememberSaveable { mutableStateOf(DemoScenario.ward) }
    var category by rememberSaveable { mutableIntStateOf(0) }
    var staff by rememberSaveable { mutableStateOf(UserRole.COLLECTOR) }
    var showLanguages by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = CityColors.forest, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Eco, null, Modifier.padding(10.dp).size(24.dp), CityColors.onForest)
                }
                Spacer(Modifier.width(12.dp))
                Text("CleanCity", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton({ showLanguages = true }) {
                    Icon(Icons.Default.Language, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(getAppStrings(language).languageCode)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Arrival {
                    Text(cityText("welcomeHeadline"), style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp, lineHeight = 47.sp), color = CityColors.ink)
                }
                Arrival(1) { Text(cityText("welcomeBody"), color = CityColors.muted, style = MaterialTheme.typography.bodyLarge) }
                Arrival(2) {
                    Surface(color = CityColors.positiveBg, shape = RoundedCornerShape(32.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            NeighborhoodArt(Modifier.heightIn(min = 148.dp), animateArrival = true)
                            Text(cityText("welcomeSteps"), Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                                style = MaterialTheme.typography.labelMedium, color = CityColors.primary)
                        }
                    }
                }
            }
            Arrival(3) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CityButton(cityText("explore"), { viewModel.loginWithDemoCitizen() }, loading = op.busy, icon = Icons.AutoMirrored.Filled.ArrowForward)
                    if (mode == null) OperationFeedback(viewModel, "auth")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton({ mode = "login"; submitted = false; viewModel.clearOperation() }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(s.loginTab) }
                        TextButton({ mode = "signup"; submitted = false; viewModel.clearOperation() }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(s.signupTab) }
                        TextButton({ mode = "staff"; submitted = false }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text(s.staffTab) }
                    }
                }
            }
        }
    }
    if (showLanguages) CitySheet(s.preferredLanguageLabel, { showLanguages = false }) {
        DemoScenario.languages.forEach { lang -> TextButton({ viewModel.setLanguage(lang); showLanguages = false }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(getAppStrings(lang).languageName) } }
    }
    if (mode != null) CitySheet(when(mode) { "signup" -> s.citizenSignupTitle; "staff" -> s.staffLoginTitle; else -> s.citizenLoginTitle },
        { if (!op.busy) mode = null }) {
        if (mode == "staff") {
            UserRole.entries.filter { it != UserRole.CITIZEN }.forEach { role -> ChoiceChip(roleLabel(role), staff == role, { staff = role }, Modifier.fillMaxWidth()) }
            CityButton(s.enterDashboard, { viewModel.loginAsStaff(staff); mode = null })
        } else {
            if (mode == "signup") {
                CityField(name, { name = it.take(100) }, s.fullNameLabel, error = submitted && name.isBlank())
                CityField(address, { address = it.take(200) }, s.streetAddressLabel, multiline = true, error = submitted && address.isBlank())
                CityField(ward, { ward = it.take(100) }, s.municipalWardLabel, error = submitted && ward.isBlank())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(s.catResidential, s.catCommercial, s.catHotel).forEachIndexed { i, label -> ChoiceChip(label, category == i, { category = i }) }
                }
            }
            CityField(phone, { phone = it.filter(Char::isDigit).take(10) }, s.mobileNumberLabel, keyboard = KeyboardType.Phone, error = submitted && phone.length != 10)
            CityField(otp, { otp = it.filter(Char::isDigit).take(6) }, s.enterOtpLabel, keyboard = KeyboardType.Number, error = submitted && otp != DemoScenario.otp)
            TextButton({ otp = DemoScenario.otp }) { Text("OTP · " + DemoScenario.otp) }
            if (submitted && (phone.length != 10 || otp != DemoScenario.otp)) Text(if (phone.length != 10) s.invalidPhoneError else s.invalidOtpError, color = CityColors.error)
            OperationFeedback(viewModel, "auth", { mode = null })
            CityButton(if (mode == "signup") s.registerAndContinue else s.verifyAndLogin, {
                submitted = true
                if (phone.length == 10 && otp == DemoScenario.otp) {
                    if (mode == "signup") {
                        if (name.isNotBlank() && address.isNotBlank() && ward.isNotBlank()) viewModel.registerAndLogin(name, phone, ward, address,
                            listOf("Household", "Commercial Shop", "Hotel / Restaurant")[category], language)
                    } else viewModel.loginExistingCitizen(phone)
                }
            }, loading = op.busy)
        }
    }
}

