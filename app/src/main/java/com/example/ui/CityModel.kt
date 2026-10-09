package com.example.ui

import com.example.ai.ChatMessage
import com.example.ai.MunicipalStats
import com.example.data.*
import kotlinx.coroutines.flow.StateFlow

// The UI consumes state and events. Android/Room stay in CleanCityViewModel;
// debug previews supply the same contract with deterministic in-memory state.
interface CityModel {
    val currentRole: StateFlow<UserRole>
    val selectedHouseholdId: StateFlow<Int>
    val isLoggedIn: StateFlow<Boolean>
    val selectedLanguage: StateFlow<String>
    val appearance: StateFlow<Appearance>
    val households: StateFlow<List<HouseholdEntity>>
    val allPickupLogs: StateFlow<List<PickupLogEntity>>
    val allComplaints: StateFlow<List<ComplaintEntity>>
    val activeComplaints: StateFlow<List<ComplaintEntity>>
    val allPayments: StateFlow<List<PaymentEntity>>
    val weighbridgeLogs: StateFlow<List<WeighbridgeLogEntity>>
    val crewAttendance: StateFlow<List<CrewAttendanceEntity>>
    val violations: StateFlow<List<ViolationFineEntity>>
    val announcements: StateFlow<List<AnnouncementEntity>>
    val registeredHousehold: StateFlow<HouseholdEntity?>
    val citizenPayments: StateFlow<List<PaymentEntity>>
    val citizenReports: StateFlow<List<ComplaintEntity>>
    val citizenPickups: StateFlow<List<PickupLogEntity>>
    val operation: StateFlow<OperationState>
    val operations: StateFlow<Map<String, OperationState>>
    val dataReady: StateFlow<Boolean>
    val statusBanner: StateFlow<String?>
    val completedDriverStops: StateFlow<IntArray>
    val demoNotices: StateFlow<ArrayList<String>>
    val chatMessages: StateFlow<List<ChatMessage>>
    val isChatLoading: StateFlow<Boolean>
    val chatError: StateFlow<String?>
    val aiSummary: StateFlow<String?>
    val isSummaryLoading: StateFlow<Boolean>
    val stats: StateFlow<MunicipalStats>
    fun initialize()
    fun clearOperation()
    fun consumeOperation(key: String)
    fun setRole(role: UserRole)
    fun setLanguage(language: String)
    fun setAppearance(value: Appearance)
    fun clearStatusBanner()
    fun logout()
    fun loginWithDemoCitizen()
    fun loginExistingCitizen(phone: String)
    fun loginAsStaff(role: UserRole)
    fun registerAndLogin(residentName: String, phone: String, ward: String, address: String, category: String, preferredLanguage: String)
    fun submitWasteReport(title: String, category: String, description: String, address: String, latitude: Double, longitude: Double, photoPath: String?)
    fun markWasteReportResolved(id: Int)
    fun payMonthlyFee(householdId: Int, amount: Double)
    fun markCollection(household: HouseholdEntity, status: String, quality: String, reason: String? = null)
    fun simulateQrScan(qrCode: String): HouseholdEntity?
    fun submitWeighbridgeEntry(vehicleNo: String, driver: String, wasteType: String, grossKg: Double, tareKg: Double, plant: String)
    fun resolveComplaint(complaintId: Int, proofPhoto: String? = "demo:inspection")
    fun escalateComplaint(complaintId: Int)
    fun toggleCrewPpe(member: CrewAttendanceEntity)
    fun issueSpotFine(violator: String, location: String, type: String, amount: Double)
    fun completeDriverStop(id: Int)
    fun acknowledgeDemo(key: String)
    fun sendChatMessage(userText: String)
    fun retryChat()
    fun generateExecutiveSummary()
}

