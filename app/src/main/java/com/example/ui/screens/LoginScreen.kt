package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
    CityPage("CleanCity", cityText("offline"), modifier) {
        item {
            Surface(color = CityColors.forest, shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(cityText("tagline"), style = MaterialTheme.typography.displayLarge, color = CityColors.onForest)
                    NeighborhoodArt()
                    Text(s.appSubtitle, style = MaterialTheme.typography.bodyMedium, color = CityColors.lime)
                }
            }
        }
        item {
            CityButton(cityText("explore"), { viewModel.loginWithDemoCitizen() }, loading = op.busy, icon = Icons.AutoMirrored.Filled.ArrowForward)
            OperationFeedback(viewModel, "auth")
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ mode = "login"; submitted = false; viewModel.clearOperation() }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text(s.loginTab) }
                OutlinedButton({ mode = "signup"; submitted = false; viewModel.clearOperation() }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text(s.signupTab) }
                OutlinedButton({ mode = "staff"; submitted = false }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text(s.staffTab) }
            }
        }
        item {
            TextButton({ showLanguages = true }) { Icon(Icons.Default.Language, null); Spacer(Modifier.width(8.dp)); Text(getAppStrings(language).languageName) }
            DemoNote()
        }
    }
    if (showLanguages) CitySheet(s.preferredLanguageLabel, { showLanguages = false }) {
        DemoScenario.languages.forEach { lang -> TextButton({ viewModel.setLanguage(lang); showLanguages = false }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(getAppStrings(lang).languageName) } }
    }
    if (mode != null) CitySheet(when(mode) { "signup" -> s.citizenSignupTitle; "staff" -> s.staffLoginTitle; else -> s.citizenLoginTitle },
        { if (!op.busy) mode = null }) {
        DemoNote()
        if (mode == "staff") {
            UserRole.entries.filter { it != UserRole.CITIZEN }.forEach { role -> ChoiceChip(roleLabel(role), staff == role, { staff = role }, Modifier.fillMaxWidth()) }
            CityButton(s.enterDashboard, { viewModel.loginAsStaff(staff); mode = null })
        } else {
            if (mode == "signup") {
                CityField(name, { name = it.take(100) }, s.fullNameLabel, error = submitted && name.isBlank())
                CityField(address, { address = it.take(200) }, s.streetAddressLabel, multiline = true, error = submitted && address.isBlank())
                CityField(ward, { ward = it.take(100) }, s.municipalWardLabel, error = submitted && ward.isBlank())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(s.catResidential, s.catCommercial, s.catHotel).forEachIndexed { i, label -> ChoiceChip(label, category == i, { category = i }) }
                }
            }
            CityField(phone, { phone = it.filter(Char::isDigit).take(10) }, s.mobileNumberLabel, keyboard = KeyboardType.Phone, error = submitted && phone.length != 10)
            CityField(otp, { otp = it.filter(Char::isDigit).take(6) }, s.enterOtpLabel, keyboard = KeyboardType.Number, error = submitted && otp != DemoScenario.otp)
            TextButton({ otp = DemoScenario.otp }) { Text(cityText("demo") + " OTP · " + DemoScenario.otp) }
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

