package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatSession
import com.example.ai.ChatAssistant
import com.example.ai.GroqCredentials
import com.example.ai.GroqService
import com.example.ai.GeminiService
import com.example.ai.MunicipalStats
import com.example.data.*
import com.example.util.getAppStrings
import com.example.util.showcaseText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserRole(val label: String, val badge: String, val subtitle: String) {
    CITIZEN("Citizen", "Citizen", "Your neighborhood, cared for"),
    COLLECTOR("Collector", "Collector", "One doorstep at a time"),
    DRIVER("Driver", "Driver", "Keeping the city moving"),
    SUPERVISOR("Supervisor", "Supervisor", "Care that reaches every street"),
    MUNICIPAL_ADMIN("Municipal Admin", "Admin", "A clearer picture of your city")
}
data class OperationState(val key: String = "", val busy: Boolean = false, val done: Boolean = false, val error: String? = null)
enum class Appearance { LIGHT, DARK, SYSTEM }

class CleanCityViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application), CityModel {
    private val preferences = application.getSharedPreferences("cleancity_preferences", 0)
    private val database = CleanCityDatabase.getDatabase(application, viewModelScope)
    private val repository = CleanCityRepository(database.cleanCityDao())
    private val geminiService = GeminiService()
    private fun text(key: String) = showcaseText(selectedLanguage.value, key)
    private fun <T> Flow<T>.live(initial: T) = stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    override val currentRole = saved.getStateFlow("role", UserRole.CITIZEN)
    override val selectedHouseholdId = saved.getStateFlow("household", 1)
    override val isLoggedIn = saved.getStateFlow("loggedIn", false)
    override val selectedLanguage = MutableStateFlow(preferences.getString("language", "English") ?: "English")
    override val appearance = MutableStateFlow(runCatching { Appearance.valueOf(preferences.getString("appearance", "LIGHT")!!) }.getOrDefault(Appearance.LIGHT))
    override val households = repository.allHouseholds.live(emptyList())
    override val allPickupLogs = repository.allPickupLogs.live(emptyList())
    override val allComplaints = repository.allComplaints.live(emptyList())
    override val activeComplaints = repository.activeComplaints.live(emptyList())
    override val allPayments = repository.allPayments.live(emptyList())
    override val weighbridgeLogs = repository.allWeighbridgeLogs.live(emptyList())
    override val crewAttendance = repository.crewAttendance.live(emptyList())
    override val violations = repository.allViolations.live(emptyList())
    override val announcements = repository.allAnnouncements.live(emptyList())
    override val registeredHousehold = combine(households, selectedHouseholdId) { list, id -> list.firstOrNull { it.id == id } }.live(null)
    override val citizenPayments = combine(allPayments, selectedHouseholdId) { list, id -> list.filter { it.householdId == id } }.live(emptyList())
    override val citizenReports = combine(allComplaints, selectedHouseholdId) { list, id -> list.filter { it.householdId == id } }.live(emptyList())
    override val citizenPickups = combine(allPickupLogs, selectedHouseholdId) { list, id -> list.filter { it.householdId == id } }.live(emptyList())
    override val operation = MutableStateFlow(OperationState())
    override val operations = MutableStateFlow<Map<String, OperationState>>(emptyMap())
    override val dataReady = MutableStateFlow(false)
    override val statusBanner = MutableStateFlow<String?>(null)
    override val completedDriverStops = saved.getStateFlow("driverStops", intArrayOf())
    override val demoNotices = saved.getStateFlow("notices", arrayListOf<String>())
    private val groqCredentials = GroqCredentials(application)
    override val chatConfigured = MutableStateFlow(groqCredentials.read().isNotBlank())
    private val chat = ChatSession(ChatAssistant { message, language, history ->
        val key = withContext(Dispatchers.IO) { groqCredentials.read() }
        GroqService(key).reply(message, language, history)
    }, viewModelScope) { selectedLanguage.value }
    override val chatMessages = chat.messages
    override val isChatLoading = chat.loading
    override val chatError = chat.error
    override val aiSummary = MutableStateFlow<String?>(null)
    override val isSummaryLoading = MutableStateFlow(false)

    override val stats = combine(households, allPickupLogs, allComplaints, allPayments, weighbridgeLogs) { homes, pickups, reports, payments, weights ->
        val today = DemoScenario.today(pickups)
        val collected = today.count { it.status == "COLLECTED" }
        val monthlyTotal = homes.sumOf { it.monthlyFee }
        val recovered = payments.filter { it.monthYear == DemoScenario.period() }.distinctBy { it.householdId }.sumOf { it.amount }
        val (start, end) = DemoScenario.dayBounds()
        MunicipalStats(homes.size, collected, if (homes.isEmpty()) 0.0 else collected * 100.0 / homes.size,
            if (today.isEmpty()) 0.0 else today.count { it.status == "COLLECTED" && it.segregationQuality == "GOOD" } * 100.0 / today.size,
            weights.filter { it.timestamp >= start && it.timestamp < end }.sumOf { it.netWeightKg } / 1000.0,
            if (monthlyTotal <= 0) 0.0 else (recovered * 100.0 / monthlyTotal).coerceAtMost(100.0),
            reports.count { it.status != "RESOLVED" }, reports.count { it.status == "RESOLVED" })
    }.live(MunicipalStats(0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0))

    init { initialize() }
    override fun initialize() = action("initialize", announce = false) {
        database.initializeDemo()
        if (repository.householdsNow().none { it.id == selectedHouseholdId.value }) {
            saved["household"] = repository.findHouseholdByQr("BIN-JH-101")!!.id
        }
        dataReady.value = true
    }
    private fun action(key: String, announce: Boolean = true, block: suspend () -> Unit) {
        if (operation.value.busy) return
        operation.value = OperationState(key, busy = true)
        operations.value = operations.value + (key to operation.value)
        viewModelScope.launch {
            try {
                block()
                operation.value = OperationState(key, done = true)
                operations.value = operations.value + (key to operation.value)
                if (announce) statusBanner.value = text("saved")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                operation.value = OperationState(key, error = if (e is IllegalArgumentException) text("checkFields") else text("saveError"))
                operations.value = operations.value + (key to operation.value)
            }
        }
    }
    override fun clearOperation() { if (!operation.value.busy) operation.value = OperationState() }
    override fun consumeOperation(key: String) {
        if (operations.value[key]?.busy == true) return
        operations.value = operations.value - key
        if (operation.value.key == key) clearOperation()
    }
    override fun selectHousehold(id: Int) {
        if (households.value.any { it.id == id }) {
            if (id != selectedHouseholdId.value) chat.clear()
            saved["household"] = id; clearOperation()
        }
    }
    override fun setRole(role: UserRole) { saved["role"] = role; clearOperation() }
    override fun setLanguage(language: String) {
        selectedLanguage.value = language
        preferences.edit().putString("language", language).apply()
    }
    override fun setAppearance(value: Appearance) {
        appearance.value = value
        preferences.edit().putString("appearance", value.name).apply()
    }
    override fun clearStatusBanner() { statusBanner.value = null }
    override fun logout() { chat.clear(); saved["loggedIn"] = false; saved["role"] = UserRole.CITIZEN; clearOperation() }
    override fun loginWithDemoCitizen() = action("auth", announce = false) {
        val household = requireNotNull(repository.findHouseholdByQr("BIN-JH-101"))
        saved["household"] = household.id
        saved["role"] = UserRole.CITIZEN
        saved["loggedIn"] = true
    }
    override fun loginExistingCitizen(phone: String) = action("auth", announce = false) {
        val digits = phone.filter(Char::isDigit).takeLast(10)
        require(digits.length == 10)
        val household = repository.householdsNow().firstOrNull { it.phone.filter(Char::isDigit).takeLast(10) == digits }
            ?: throw IllegalArgumentException()
        saved["household"] = household.id; saved["role"] = UserRole.CITIZEN; saved["loggedIn"] = true
    }
    override fun loginAsStaff(role: UserRole) { clearOperation(); saved["role"] = role; saved["loggedIn"] = true }
    override fun registerAndLogin(residentName: String, phone: String, ward: String, address: String, category: String, preferredLanguage: String) = action("auth", announce = false) {
        require(residentName.isNotBlank() && address.isNotBlank() && ward.isNotBlank() && phone.filter(Char::isDigit).length == 10)
        require(repository.householdsNow().none { it.phone.filter(Char::isDigit).takeLast(10) == phone })
        val fee = when (category) { "Commercial Shop" -> 350.0; "Hotel / Restaurant" -> 850.0; else -> 90.0 }
        val id = repository.registerOrUpdateHousehold(HouseholdEntity(qrCode = "BIN-" + java.util.UUID.randomUUID().toString().take(8),
            residentName = residentName.trim(), phone = phone, ward = ward, address = address.trim(), category = category,
            monthlyFee = fee, feeStatus = "DUE", preferredLanguage = preferredLanguage, segregationScore = 85, streakDays = 0))
        saved["household"] = id.toInt(); setLanguage(preferredLanguage); saved["role"] = UserRole.CITIZEN; saved["loggedIn"] = true
    }
    override fun submitWasteReport(title: String, category: String, description: String, address: String, latitude: Double, longitude: Double, photoPath: String?) = action("report") {
        val household = requireNotNull(registeredHousehold.value)
        repository.submitComplaint(title, category, description, household.ward, address, latitude, longitude,
            photoPath, household.phone, household.id)
    }
    override fun markWasteReportResolved(id: Int) = resolveComplaint(id, "note:Resolution confirmed by resident.")
    override fun payMonthlyFee(householdId: Int, amount: Double) = action("payment") {
        require(householdId == selectedHouseholdId.value)
        repository.payMonthlyFee(householdId, DemoScenario.period(), amount, "CC-" + System.currentTimeMillis())
    }
    override fun markCollection(household: HouseholdEntity, status: String, quality: String, reason: String?) = action("collection") {
        repository.recordPickup(household.id, household.address, household.residentName, CityFixtures.collector, status, quality, reason)
    }
    override fun findHouseholdByQr(qrCode: String): HouseholdEntity? = households.value.firstOrNull { it.qrCode.equals(qrCode.trim(), true) }
    override fun submitWeighbridgeEntry(vehicleNo: String, driver: String, wasteType: String, grossKg: Double, tareKg: Double, plant: String) = action("weight") {
        repository.logWeighbridge(vehicleNo, driver, wasteType, grossKg, tareKg, plant)
    }
    override fun resolveComplaint(complaintId: Int, proofPhoto: String?) = action("resolve") {
        repository.resolveComplaint(complaintId, proofPhoto)
    }
    override fun escalateComplaint(complaintId: Int) = action("escalate") { repository.escalateComplaint(complaintId) }
    override fun toggleCrewPpe(member: CrewAttendanceEntity) = action("ppe") { repository.updateCrewMember(member.copy(ppeChecked = !member.ppeChecked)) }
    override fun issueSpotFine(violator: String, location: String, type: String, amount: Double) = action("fine") {
        repository.issueViolation(registeredHousehold.value?.ward ?: DemoScenario.ward, location, violator, type, amount)
    }
    override fun completeDriverStop(id: Int) {
        saved["driverStops"] = (completedDriverStops.value.toList() + id).distinct().toIntArray()
        statusBanner.value = text("saved")
    }
    override fun acknowledgeDemo(key: String) {
        saved["notices"] = ArrayList((demoNotices.value + key).distinct())
        statusBanner.value = text("demoRecorded")
    }
    override fun sendChatMessage(userText: String) = chat.send(userText)
    override fun retryChat() = chat.retry()
    override fun configureChatKey(key: String) = action("chatKey", announce = false) {
        require(GroqCredentials.valid(key))
        withContext(Dispatchers.IO) { groqCredentials.save(key) }
        chat.clear()
        chatConfigured.value = true
    }
    override fun removeChatKey() = action("chatKey", announce = false) {
        chat.clear()
        withContext(Dispatchers.IO) { groqCredentials.clear() }
        chatConfigured.value = false
    }
    override fun generateExecutiveSummary() {
        if (isSummaryLoading.value) return
        isSummaryLoading.value = true
        viewModelScope.launch {
            try { aiSummary.value = geminiService.generateExecutiveSummary(stats.value, selectedLanguage.value) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { statusBanner.value = text("saveError") }
            finally { isSummaryLoading.value = false }
        }
    }
}

