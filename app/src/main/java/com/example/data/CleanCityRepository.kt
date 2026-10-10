package com.example.data

import java.util.UUID

class CleanCityRepository(private val dao: CleanCityDao) {
    val allHouseholds = dao.getAllHouseholds()
    val allPickupLogs = dao.getAllPickupLogs()
    val allComplaints = dao.getAllComplaints()
    val activeComplaints = dao.getActiveComplaints()
    val allPayments = dao.getAllPayments()
    val allWeighbridgeLogs = dao.getAllWeighbridgeLogs()
    val crewAttendance = dao.getCrewAttendance()
    val allViolations = dao.getAllViolations()
    val allAnnouncements = dao.getAllAnnouncements()
    fun getHousehold(id: Int) = dao.getHouseholdById(id)
    fun getPayments(id: Int) = dao.getPaymentsForHousehold(id)
    fun getPickupLogs(id: Int) = dao.getLogsForHousehold(id)
    fun getReports(id: Int) = dao.reportsForHousehold(id)
    suspend fun householdsNow() = dao.householdsNow()
    suspend fun registerOrUpdateHousehold(household: HouseholdEntity) = dao.insertHousehold(household)
    suspend fun findHouseholdByQr(qrCode: String) = dao.getHouseholdByQrCode(qrCode)
    suspend fun recordPickup(householdId: Int, address: String, residentName: String, collectorName: String,
        status: String, quality: String, reasonNotCollected: String? = null, notes: String? = null): Long {
        require(status in listOf("COLLECTED", "NOT_COLLECTED", "MIXED_WASTE"))
        require(quality in listOf("GOOD", "PARTIAL", "MIXED"))
        require(status != "NOT_COLLECTED" || !reasonNotCollected.isNullOrBlank())
        val now = System.currentTimeMillis()
        val (start, end) = DemoScenario.dayBounds(now)
        return dao.recordPickupOnce(PickupLogEntity(householdId = householdId, householdAddress = address,
            residentName = residentName, collectorName = collectorName, timestamp = now, status = status,
            segregationQuality = quality, reasonNotCollected = reasonNotCollected, notes = notes), start, end)
    }
    suspend fun submitComplaint(title: String, category: String, description: String, ward: String, address: String,
        latitude: Double, longitude: Double, photoUri: String? = null, citizenPhone: String = "", householdId: Int? = null): Long {
        require(title.isNotBlank() && description.isNotBlank() && address.isNotBlank())
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
        return dao.insertComplaint(ComplaintEntity(title = title.trim().take(150), category = category,
            description = description.trim().take(1000), ward = ward, address = address.trim().take(200), latitude = latitude,
            longitude = longitude, photoUri = photoUri, status = "OPEN", citizenPhone = citizenPhone, householdId = householdId,
            assignedInspector = CityFixtures.inspector))
    }
    suspend fun updateWasteReportStatus(id: Int, status: String) {
        require(status in listOf("OPEN", "IN_PROGRESS", "ESCALATED", "RESOLVED"))
        dao.updateStatus(id, status)
    }
    suspend fun resolveComplaint(complaintId: Int, proofPhoto: String? = null) = dao.resolveComplaint(complaintId, System.currentTimeMillis(), proofPhoto)
    suspend fun escalateComplaint(complaintId: Int) = dao.escalateComplaint(complaintId)
    suspend fun payMonthlyFee(householdId: Int, monthYear: String, amount: Double, upiRef: String): Long {
        require(amount.isFinite() && amount > 0)
        val household = requireNotNull(dao.householdNow(householdId))
        require(kotlin.math.abs(household.monthlyFee - amount) < 0.01)
        return dao.payOnce(PaymentEntity(householdId = householdId, monthYear = monthYear, amount = amount,
            upiRef = upiRef, receiptNumber = "CC-" + UUID.randomUUID().toString().take(8).uppercase()))
    }
    suspend fun logWeighbridge(vehicleNumber: String, driverName: String, wasteType: String, grossKg: Double, tareKg: Double, plant: String): Long {
        require(vehicleNumber.isNotBlank() && driverName.isNotBlank() && plant.isNotBlank())
        require(grossKg.isFinite() && tareKg.isFinite() && tareKg > 0 && grossKg > tareKg)
        return dao.insertWeighbridgeLog(WeighbridgeLogEntity(vehicleNumber = vehicleNumber.trim(), driverName = driverName,
            wasteType = wasteType, grossWeightKg = grossKg, tareWeightKg = tareKg, netWeightKg = grossKg - tareKg, treatmentPlant = plant))
    }
    suspend fun updateCrewMember(member: CrewAttendanceEntity) = dao.updateCrewMember(member)
    suspend fun issueViolation(ward: String, location: String, violatorName: String, violationType: String, fineAmount: Double): Long {
        require(violatorName.isNotBlank() && location.isNotBlank() && violationType.isNotBlank())
        require(fineAmount.isFinite() && fineAmount > 0)
        return dao.insertViolation(ViolationFineEntity(ward = ward, location = location.trim(),
            violatorName = violatorName.trim(), violationType = violationType, fineAmount = fineAmount))
    }
    suspend fun updateHousehold(household: HouseholdEntity) = dao.updateHousehold(household)
}

