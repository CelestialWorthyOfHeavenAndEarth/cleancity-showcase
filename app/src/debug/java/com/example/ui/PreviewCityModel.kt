package com.example.ui

import com.example.ai.ChatMessage
import com.example.ai.MunicipalStats
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Debug-only static state: never constructs a database, AndroidViewModel or network client.
class PreviewCityModel : CityModel {
    private val home = DemoScenario.household()
    private val previousMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }.timeInMillis
    private val homes = listOf(home) + listOf("Lakshmi Rao", "Green Leaf Café", "Ravi Stores", "Meera & Suresh", "Arjun Reddy").mapIndexed { index, name ->
        home.copy(id = index + 2, qrCode = "BIN-JH-" + (102 + index), residentName = name,
            address = "Road " + (10 + index) + ", Jubilee Hills", phone = "+91 98765 " + (10102 + index),
            feeStatus = if (index % 2 == 0) "DUE" else "PAID")
    }
    private val pickups = homes.filter { it.id in 2..4 }.map { household ->
        PickupLogEntity(id = household.id, householdId = household.id, householdAddress = household.address,
            residentName = household.residentName, collectorName = "Ramesh Kumar",
            status = if (household.id == 4) "MIXED_WASTE" else "COLLECTED",
            segregationQuality = if (household.id == 4) "MIXED" else "GOOD")
    }
    private val report = ComplaintEntity(id = 1, title = "Community bin needs attention", category = "OVERFLOW", description = "The park bin is full.", ward = DemoScenario.ward, address = "Road 12, Jubilee Hills", householdId = 1)
    private val payment = PaymentEntity(id = 1, householdId = 1, monthYear = DemoScenario.period(previousMonth), paidAt = previousMonth, amount = 90.0, upiRef = "DEMO-104", receiptNumber = "CC-DEMO-104")
    private val payments = listOf(payment) + homes.filter { it.feeStatus == "PAID" }.map {
        PaymentEntity(id = it.id, householdId = it.id, monthYear = DemoScenario.period(), amount = it.monthlyFee,
            upiRef = "DEMO-" + it.id, receiptNumber = "CC-DEMO-" + it.id)
    }
    override val currentRole: StateFlow<UserRole> = MutableStateFlow(UserRole.CITIZEN)
    override val selectedHouseholdId: StateFlow<Int> = MutableStateFlow(1)
    override val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(true)
    override val selectedLanguage: StateFlow<String> = MutableStateFlow("English")
    override val appearance: StateFlow<Appearance> = MutableStateFlow(Appearance.LIGHT)
    override val households: StateFlow<List<HouseholdEntity>> = MutableStateFlow(homes)
    override val allPickupLogs: StateFlow<List<PickupLogEntity>> = MutableStateFlow(pickups)
    override val allComplaints: StateFlow<List<ComplaintEntity>> = MutableStateFlow(listOf(report))
    override val activeComplaints: StateFlow<List<ComplaintEntity>> = MutableStateFlow(listOf(report))
    override val allPayments: StateFlow<List<PaymentEntity>> = MutableStateFlow(payments)
    override val weighbridgeLogs: StateFlow<List<WeighbridgeLogEntity>> = MutableStateFlow(listOf(WeighbridgeLogEntity(id = 1,
        vehicleNumber = DemoScenario.vehicle, driverName = "Manjunath", wasteType = "Wet waste",
        grossWeightKg = 8200.0, tareWeightKg = 4100.0, netWeightKg = 4100.0, treatmentPlant = "City recovery centre")))
    override val crewAttendance: StateFlow<List<CrewAttendanceEntity>> = MutableStateFlow(listOf(CrewAttendanceEntity(id = 1, workerName = "Ramesh Kumar", role = "Collector", ward = DemoScenario.ward, date = DemoScenario.date(), status = "ON_ROUTE")))
    override val violations: StateFlow<List<ViolationFineEntity>> = MutableStateFlow(emptyList())
    override val announcements: StateFlow<List<AnnouncementEntity>> = MutableStateFlow(listOf(AnnouncementEntity(id = 1, title = "A little sorting. A cleaner neighborhood.", content = "Keep wet waste and dry recyclables separate.", category = "Advisory", date = DemoScenario.date())))
    override val registeredHousehold: StateFlow<HouseholdEntity?> = MutableStateFlow(home)
    override val citizenPayments: StateFlow<List<PaymentEntity>> = MutableStateFlow(listOf(payment))
    override val citizenReports: StateFlow<List<ComplaintEntity>> = MutableStateFlow(listOf(report))
    override val citizenPickups: StateFlow<List<PickupLogEntity>> = MutableStateFlow(emptyList())
    override val operation: StateFlow<OperationState> = MutableStateFlow(OperationState())
    override val operations: StateFlow<Map<String, OperationState>> = MutableStateFlow(emptyMap())
    override val dataReady: StateFlow<Boolean> = MutableStateFlow(true)
    override val statusBanner: StateFlow<String?> = MutableStateFlow(null)
    override val completedDriverStops: StateFlow<IntArray> = MutableStateFlow(intArrayOf(0,1))
    override val demoNotices: StateFlow<ArrayList<String>> = MutableStateFlow(arrayListOf())
    override val chatMessages: StateFlow<List<ChatMessage>> = MutableStateFlow(emptyList())
    override val isChatLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val chatError: StateFlow<String?> = MutableStateFlow(null)
    override val aiSummary: StateFlow<String?> = MutableStateFlow(null)
    override val isSummaryLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val stats: StateFlow<MunicipalStats> = MutableStateFlow(MunicipalStats(homes.size, 2, 200.0 / homes.size,
        200.0 / pickups.size, 4.1, payments.filter { it.monthYear == DemoScenario.period() }.sumOf { it.amount } * 100 / homes.sumOf { it.monthlyFee }, 1, 0))
    override fun initialize() {}
    override fun clearOperation() {}
    override fun consumeOperation(key: String) {}
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
    override fun simulateQrScan(qrCode: String): HouseholdEntity? = households.value.firstOrNull { it.qrCode == qrCode }
    override fun submitWeighbridgeEntry(vehicleNo: String, driver: String, wasteType: String, grossKg: Double, tareKg: Double, plant: String) {}
    override fun resolveComplaint(complaintId: Int, proofPhoto: String?) {}
    override fun escalateComplaint(complaintId: Int) {}
    override fun toggleCrewPpe(member: CrewAttendanceEntity) {}
    override fun issueSpotFine(violator: String, location: String, type: String, amount: Double) {}
    override fun completeDriverStop(id: Int) {}
    override fun acknowledgeDemo(key: String) {}
    override fun sendChatMessage(userText: String) {}
    override fun retryChat() {}
    override fun generateExecutiveSummary() {}
}
