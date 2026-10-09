package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val qrCode: String,
    val residentName: String,
    val phone: String,
    val address: String,
    val ward: String,
    val category: String, // "Household", "Commercial Shop", "Hotel / Restaurant"
    val segregationScore: Int = 85,
    val streakDays: Int = 5,
    val monthlyFee: Double = 90.0,
    val feeStatus: String = "PAID", // "PAID", "DUE", "OVERDUE"
    val preferredLanguage: String = "English"
)

@Entity(tableName = "pickup_logs")
data class PickupLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val householdId: Int,
    val householdAddress: String,
    val residentName: String,
    val collectorName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "COLLECTED", "NOT_COLLECTED", "MIXED_WASTE"
    val segregationQuality: String, // "GOOD", "PARTIAL", "MIXED"
    val reasonNotCollected: String? = null,
    val notes: String? = null
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // Stable IDs: "MISSED", "OVERFLOW", "DUMPING", "LITTER"
    val description: String,
    val ward: String,
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUri: String? = null,
    val status: String = "OPEN", // "OPEN", "IN_PROGRESS", "RESOLVED", "ESCALATED"
    val createdAt: Long = System.currentTimeMillis(),
    val slaDeadlineMillis: Long = System.currentTimeMillis() + 86400000L, // 24 hours SLA
    val resolvedAt: Long? = null,
    val assignedInspector: String = "Inspector R. Sharma",
    val resolutionProofPhoto: String? = null,
    val citizenPhone: String = "",
    val householdId: Int? = null
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val householdId: Int,
    val monthYear: String,
    val amount: Double,
    val status: String = "PAID", // "PAID", "PENDING"
    val upiRef: String,
    val paidAt: Long = System.currentTimeMillis(),
    val receiptNumber: String
)

@Entity(tableName = "weighbridge_logs")
data class WeighbridgeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleNumber: String,
    val driverName: String,
    val wasteType: String, // "Wet Waste (Organic)", "Dry Recyclable", "Inert / C&D"
    val grossWeightKg: Double,
    val tareWeightKg: Double,
    val netWeightKg: Double,
    val treatmentPlant: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "crew_attendance")
data class CrewAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerName: String,
    val role: String, // "Collector", "Driver", "Helper"
    val ward: String,
    val date: String,
    val status: String = "PRESENT", // "PRESENT", "ON_ROUTE", "LEAVE"
    val checkInTime: String = "06:30 AM",
    val ppeChecked: Boolean = true
)

@Entity(tableName = "violations_fines")
data class ViolationFineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ward: String,
    val location: String,
    val violatorName: String,
    val violationType: String, // "Commercial Mixed Waste", "Illegal Night Dumping", "Single-Use Plastic"
    val fineAmount: Double,
    val status: String = "ISSUED", // "ISSUED", "PAID"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // "Schedule Change", "Weather Alert", "Compost Campaign", "Advisory"
    val date: String,
    val isUrgent: Boolean = false
)
