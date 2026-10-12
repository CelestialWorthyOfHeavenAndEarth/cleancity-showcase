package com.example.ui

import com.example.ai.ChatMessage
import com.example.ai.MunicipalStats
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Debug-only static state: never constructs a database, AndroidViewModel or network client.
class PreviewCityModel : CityModel {
    private val now = System.currentTimeMillis()
    private val homes = CityFixtures.households()
    private val home = homes.first()
    private val reports = CityFixtures.reports(homes, now).mapIndexed { i, row -> row.copy(id = i + 1) }
    private val pickups = CityFixtures.pickups(homes, now).mapIndexed { i, row -> row.copy(id = i + 1) }.sortedByDescending { it.timestamp }
    private val payments = CityFixtures.payments(homes, now).mapIndexed { i, row -> row.copy(id = i + 1) }.sortedByDescending { it.paidAt }
    private val weights = CityFixtures.weights(now).mapIndexed { i, row -> row.copy(id = i + 1) }
    override val currentRole: StateFlow<UserRole> = MutableStateFlow(UserRole.CITIZEN)
    override val selectedHouseholdId: StateFlow<Int> = MutableStateFlow(1)
    override val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(true)
    override val selectedLanguage: StateFlow<String> = MutableStateFlow("English")
    override val appearance: StateFlow<Appearance> = MutableStateFlow(Appearance.LIGHT)
    override val households: StateFlow<List<HouseholdEntity>> = MutableStateFlow(homes)
    override val allPickupLogs: StateFlow<List<PickupLogEntity>> = MutableStateFlow(pickups)
    override val allComplaints: StateFlow<List<ComplaintEntity>> = MutableStateFlow(reports)
    override val activeComplaints: StateFlow<List<ComplaintEntity>> = MutableStateFlow(reports.filter { it.status != "RESOLVED" })
    override val allPayments: StateFlow<List<PaymentEntity>> = MutableStateFlow(payments)
    override val weighbridgeLogs: StateFlow<List<WeighbridgeLogEntity>> = MutableStateFlow(weights)
    override val crewAttendance: StateFlow<List<CrewAttendanceEntity>> = MutableStateFlow(CityFixtures.crew(now).mapIndexed { i, row -> row.copy(id = i + 1) })
    override val violations: StateFlow<List<ViolationFineEntity>> = MutableStateFlow(CityFixtures.fines(now).mapIndexed { i, row -> row.copy(id = i + 1) })
    override val announcements: StateFlow<List<AnnouncementEntity>> = MutableStateFlow(CityFixtures.announcements(now).mapIndexed { i, row -> row.copy(id = i + 1) })
    override val registeredHousehold: StateFlow<HouseholdEntity?> = MutableStateFlow(home)
    override val citizenPayments: StateFlow<List<PaymentEntity>> = MutableStateFlow(payments.filter { it.householdId == home.id })
    override val citizenReports: StateFlow<List<ComplaintEntity>> = MutableStateFlow(reports.filter { it.householdId == home.id })
    override val citizenPickups: StateFlow<List<PickupLogEntity>> = MutableStateFlow(pickups.filter { it.householdId == home.id })
    override val operation: StateFlow<OperationState> = MutableStateFlow(OperationState())
    override val operations: StateFlow<Map<String, OperationState>> = MutableStateFlow(emptyMap())
    override val dataReady: StateFlow<Boolean> = MutableStateFlow(true)
    override val statusBanner: StateFlow<String?> = MutableStateFlow(null)
    override val completedDriverStops: StateFlow<IntArray> = MutableStateFlow(intArrayOf(0,1))
    override val demoNotices: StateFlow<ArrayList<String>> = MutableStateFlow(arrayListOf())
    override val chatMessages: StateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    override val isChatLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val chatError: StateFlow<String?> = MutableStateFlow(null)
    override val chatConfigured: StateFlow<Boolean> = MutableStateFlow(true)
    override val aiSummary: StateFlow<String?> = MutableStateFlow(null)
    override val isSummaryLoading: StateFlow<Boolean> = MutableStateFlow(false)
    private val today = DemoScenario.today(pickups)
    private val collected = today.count { it.status == "COLLECTED" }
    override val stats: StateFlow<MunicipalStats> = MutableStateFlow(MunicipalStats(homes.size, collected,
        collected * 100.0 / homes.size, today.count { it.status == "COLLECTED" && it.segregationQuality == "GOOD" } * 100.0 / today.size,
        weights.filter { it.timestamp >= DemoScenario.dayBounds(now).first }.sumOf { it.netWeightKg } / 1000,
        payments.filter { it.monthYear == DemoScenario.period() }.sumOf { it.amount } * 100 / homes.sumOf { it.monthlyFee },
        reports.count { it.status != "RESOLVED" }, reports.count { it.status == "RESOLVED" }))
    override fun initialize() {}
    override fun clearOperation() {}
    override fun consumeOperation(key: String) {}
    override fun selectHousehold(id: Int) {}
    override fun setRole(role: UserRole) {}
    override fun setLanguage(language: String) {}
    override fun setAppearance(value: Appearance) {}
    override fun clearStatusBanner() {}
    override fun logout() {}
    override fun loginWithDemoCitizen() {}
    override fun loginExistingCitizen(phone: String) {}
    override fun loginAsStaff(role: UserRole) {}
    override fun registerAndLogin(residentName: String, phone: String, ward: String, address: String, category: String, preferredLanguage: String) {}
    override fun submitWasteReport(title: String, category: String, description: String, address: String, latitude: Double, longitude: Double, photoPath: String?) {}
    override fun markWasteReportResolved(id: Int) {}
    override fun payMonthlyFee(householdId: Int, amount: Double) {}
    override fun markCollection(household: HouseholdEntity, status: String, quality: String, reason: String?) {}
    override fun findHouseholdByQr(qrCode: String): HouseholdEntity? = households.value.firstOrNull { it.qrCode == qrCode }
    override fun submitWeighbridgeEntry(vehicleNo: String, driver: String, wasteType: String, grossKg: Double, tareKg: Double, plant: String) {}
    override fun resolveComplaint(complaintId: Int, proofPhoto: String?) {}
    override fun escalateComplaint(complaintId: Int) {}
    override fun toggleCrewPpe(member: CrewAttendanceEntity) {}
    override fun issueSpotFine(violator: String, location: String, type: String, amount: Double) {}
    override fun completeDriverStop(id: Int) {}
    override fun acknowledgeDemo(key: String) {}
    override fun sendChatMessage(userText: String) {}
    override fun retryChat() {}
    override fun configureChatKey(key: String) {}
    override fun removeChatKey() {}
    override fun generateExecutiveSummary() {}
}
