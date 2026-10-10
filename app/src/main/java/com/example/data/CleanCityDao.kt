package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanCityDao {
    @Query("SELECT COUNT(*) FROM seed_markers WHERE `key` = :key")
    suspend fun seedApplied(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeedApplied(marker: SeedMarkerEntity)

    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    suspend fun householdNow(id: Int): HouseholdEntity?

    @Query("SELECT * FROM households")
    suspend fun householdsNow(): List<HouseholdEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHouseholdIfMissing(household: HouseholdEntity): Long

    @Query("SELECT * FROM complaints WHERE householdId = :id ORDER BY createdAt DESC")
    fun reportsForHousehold(id: Int): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM payments WHERE householdId = :id AND monthYear = :period AND status = 'PAID' LIMIT 1")
    suspend fun paymentForPeriod(id: Int, period: String): PaymentEntity?

    @Transaction
    suspend fun payOnce(payment: PaymentEntity): Long {
        paymentForPeriod(payment.householdId, payment.monthYear)?.let { return it.id.toLong() }
        check(householdNow(payment.householdId) != null)
        val id = insertPayment(payment)
        markFeePaid(payment.householdId)
        return id
    }

    @Query("SELECT * FROM pickup_logs WHERE householdId = :id AND timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC LIMIT 1")
    suspend fun pickupForDay(id: Int, start: Long, end: Long): PickupLogEntity?

    @Transaction
    suspend fun recordPickupOnce(log: PickupLogEntity, start: Long, end: Long): Long {
        pickupForDay(log.householdId, start, end)?.let { return it.id.toLong() }
        check(householdNow(log.householdId) != null)
        val id = insertPickupLog(log)
        if (log.status == "COLLECTED" && log.segregationQuality == "GOOD") recordGoodSegregation(log.householdId)
        if (log.status == "MIXED_WASTE" || (log.status == "COLLECTED" && log.segregationQuality == "MIXED")) recordMixedSegregation(log.householdId)
        return id
    }

    // --- Households ---
    @Query("SELECT * FROM households ORDER BY id ASC")
    fun getAllHouseholds(): Flow<List<HouseholdEntity>>

    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    fun getHouseholdById(id: Int): Flow<HouseholdEntity?>

    @Query("SELECT * FROM households WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getHouseholdByQrCode(qrCode: String): HouseholdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouseholds(households: List<HouseholdEntity>)

    @Update
    suspend fun updateHousehold(household: HouseholdEntity)

    @Query("UPDATE households SET feeStatus = 'PAID' WHERE id = :id")
    suspend fun markFeePaid(id: Int)

    @Query("UPDATE households SET streakDays = streakDays + 1, segregationScore = MIN(100, segregationScore + 2) WHERE id = :id")
    suspend fun recordGoodSegregation(id: Int)

    @Query("UPDATE households SET streakDays = 0, segregationScore = MAX(40, segregationScore - 5) WHERE id = :id")
    suspend fun recordMixedSegregation(id: Int)

    // --- Pickup Logs ---
    @Query("SELECT * FROM pickup_logs ORDER BY timestamp DESC")
    fun getAllPickupLogs(): Flow<List<PickupLogEntity>>

    @Query("SELECT * FROM pickup_logs WHERE householdId = :householdId ORDER BY timestamp DESC")
    fun getLogsForHousehold(householdId: Int): Flow<List<PickupLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPickupLog(log: PickupLogEntity): Long

    // --- Complaints ---
    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE status != 'RESOLVED' ORDER BY slaDeadlineMillis ASC")
    fun getActiveComplaints(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity): Long

    @Update
    suspend fun updateComplaint(complaint: ComplaintEntity)

    @Query("UPDATE complaints SET status = 'RESOLVED', resolvedAt = :resolvedAt, resolutionProofPhoto = :proofPhoto WHERE id = :id")
    suspend fun resolveComplaint(id: Int, resolvedAt: Long = System.currentTimeMillis(), proofPhoto: String? = null)

    @Query("UPDATE complaints SET status = 'ESCALATED' WHERE id = :id")
    suspend fun escalateComplaint(id: Int)

    @Query("UPDATE complaints SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    // --- Payments ---
    @Query("SELECT * FROM payments WHERE householdId = :householdId ORDER BY paidAt DESC")
    fun getPaymentsForHousehold(householdId: Int): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY paidAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    // --- Weighbridge Logs ---
    @Query("SELECT * FROM weighbridge_logs ORDER BY timestamp DESC")
    fun getAllWeighbridgeLogs(): Flow<List<WeighbridgeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeighbridgeLog(log: WeighbridgeLogEntity): Long

    // --- Crew Attendance ---
    @Query("SELECT * FROM crew_attendance ORDER BY id ASC")
    fun getCrewAttendance(): Flow<List<CrewAttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrewAttendance(crew: List<CrewAttendanceEntity>)

    @Update
    suspend fun updateCrewMember(member: CrewAttendanceEntity)

    // --- Violations & Fines ---
    @Query("SELECT * FROM violations_fines ORDER BY timestamp DESC")
    fun getAllViolations(): Flow<List<ViolationFineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolation(violation: ViolationFineEntity): Long

    // --- Announcements ---
    @Query("SELECT * FROM announcements ORDER BY id DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)
}
