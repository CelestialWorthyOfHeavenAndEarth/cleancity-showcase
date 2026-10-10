package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: CleanCityViewModel = viewModel()
            val appearance by model.appearance.collectAsStateWithLifecycle()
            val dark = appearance == Appearance.DARK || (appearance == Appearance.SYSTEM && isSystemInDarkTheme())
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
            }
            CleanCityTheme(darkTheme = dark) { CleanCityApp(model) }
        }
    }
}
@Composable
fun CleanCityApp(viewModel: CleanCityViewModel = viewModel()) {
    val loggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val ready by viewModel.dataReady.collectAsStateWithLifecycle()
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val language by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val household by viewModel.registeredHousehold.collectAsStateWithLifecycle()
    val homes by viewModel.households.collectAsStateWithLifecycle()
    val pickups by viewModel.allPickupLogs.collectAsStateWithLifecycle()
    val reports by viewModel.allComplaints.collectAsStateWithLifecycle()
    val payments by viewModel.citizenPayments.collectAsStateWithLifecycle()
    val citizenReports by viewModel.citizenReports.collectAsStateWithLifecycle()
    val weights by viewModel.weighbridgeLogs.collectAsStateWithLifecycle()
    val crew by viewModel.crewAttendance.collectAsStateWithLifecycle()
    val fines by viewModel.violations.collectAsStateWithLifecycle()
    val notices by viewModel.announcements.collectAsStateWithLifecycle()
    val message by viewModel.statusBanner.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val stateHolder = rememberSaveableStateHolder()
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearStatusBanner() }
    }
    BackHandler(loggedIn && role != UserRole.CITIZEN) { viewModel.setRole(UserRole.CITIZEN) }
    CompositionLocalProvider(LocalAppStrings provides getAppStrings(language)) {
        Scaffold(containerColor = CityColors.canvas, contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 1120.dp).fillMaxSize()) {
                    if (!ready) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                if (operation.error == null) CircularProgressIndicator()
                                else { Text(operation.error!!); CityButton(cityText("retry"), { viewModel.initialize() }) }
                            }
                        }
                    } else if (!loggedIn) LoginScreen(viewModel)
                    else {
                        RoleHeaderBar(role, language, viewModel::setRole, viewModel::setLanguage,
                            residentName = household?.residentName, onLogout = viewModel::logout, wardName = household?.ward,
                            appearance = appearance, onAppearance = viewModel::setAppearance,
                            households = homes, onHouseholdSelected = viewModel::selectHousehold)
                        HorizontalDivider(color = CityColors.line)
                        Crossfade(role, Modifier.weight(1f), tween(220), label = "Role") { active ->
                            stateHolder.SaveableStateProvider(if (active == UserRole.CITIZEN) active.name + household?.id else active.name) {
                                when (active) {
                                    UserRole.CITIZEN -> CitizenScreen(viewModel, household, notices, citizenReports, payments)
                                    UserRole.COLLECTOR -> CollectorScreen(viewModel, homes, pickups)
                                    UserRole.DRIVER -> DriverScreen(viewModel, weights)
                                    UserRole.SUPERVISOR -> SupervisorScreen(viewModel, reports, crew)
                                    UserRole.MUNICIPAL_ADMIN -> AdminScreen(viewModel, homes, fines)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

