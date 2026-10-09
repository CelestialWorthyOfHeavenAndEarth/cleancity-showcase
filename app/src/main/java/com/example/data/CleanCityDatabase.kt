package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(entities = [HouseholdEntity::class, PickupLogEntity::class, ComplaintEntity::class,
    PaymentEntity::class, WeighbridgeLogEntity::class, CrewAttendanceEntity::class,
    ViolationFineEntity::class, AnnouncementEntity::class], version = 3, exportSchema = false)
abstract class CleanCityDatabase : RoomDatabase() {
    abstract fun cleanCityDao(): CleanCityDao
    suspend fun initializeDemo() = withTransaction {
        val dao = cleanCityDao()
        if (dao.householdsNow().isEmpty()) populateInitialData(dao) else ensureDemoHousehold(dao)
    }
    companion object {
        @Volatile private var INSTANCE: CleanCityDatabase? = null
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE complaints ADD COLUMN householdId INTEGER")
                db.execSQL("UPDATE complaints SET status = 'OPEN' WHERE status = 'PENDING'")
                val households = mutableMapOf<String, MutableList<Int>>()
                db.query("SELECT id, phone FROM households").use { c ->
                    while (c.moveToNext()) {
                        val phone = c.getString(1).filter { it.isDigit() }.takeLast(10)
                        if (phone.length == 10) households.getOrPut(phone) { mutableListOf() }.add(c.getInt(0))
                    }
                }
                db.query("SELECT id, citizenPhone FROM complaints").use { c ->
                    while (c.moveToNext()) {
                        val phone = c.getString(1)?.filter { it.isDigit() }?.takeLast(10)
                        val ids = households[phone]
                        if (ids?.size == 1) db.execSQL("UPDATE complaints SET householdId = ? WHERE id = ?", arrayOf(ids.first(), c.getInt(0)))
                    }
                }
            }
        }
        @Suppress("UNUSED_PARAMETER")
        fun getDatabase(context: Context, scope: CoroutineScope): CleanCityDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, CleanCityDatabase::class.java, "clean_city_database")
                    .addMigrations(MIGRATION_2_3).build().also { INSTANCE = it }
            }
        suspend fun ensureDemoHousehold(dao: CleanCityDao) {
            if (dao.getHouseholdByQrCode("BIN-JH-101") == null) {
                val demo = DemoScenario.household()
                dao.insertHouseholdIfMissing(if (dao.householdNow(1) == null) demo else demo.copy(id = 0))
            }
        }
        suspend fun populateInitialData(dao: CleanCityDao) {
            ensureDemoHousehold(dao)
            val demoId = dao.getHouseholdByQrCode("BIN-JH-101")!!.id
            listOf("Lakshmi Rao", "Green Leaf Café", "Ravi Stores", "Meera & Suresh", "Arjun Reddy").forEachIndexed { i, name ->
                dao.insertHouseholdIfMissing(HouseholdEntity(
                    id = i + 2, qrCode = "BIN-JH-" + (102 + i), residentName = name, phone = "+91 98765 " + (10102 + i),
                    address = "Road " + (10 + i) + ", Jubilee Hills", ward = DemoScenario.ward,
                    category = if (i == 1) "Hotel / Restaurant" else if (i == 2) "Commercial Shop" else "Household",
                    monthlyFee = if (i == 1) 850.0 else if (i == 2) 350.0 else 90.0,
                    feeStatus = if (i % 2 == 0) "DUE" else "PAID", segregationScore = 78 + i * 4, streakDays = 3 + i
                ))
            }
            val now = System.currentTimeMillis()
            dao.householdsNow().filter { it.feeStatus == "PAID" }.forEach { home ->
                dao.insertPayment(PaymentEntity(householdId = home.id, monthYear = DemoScenario.period(now),
                    amount = home.monthlyFee, upiRef = "DEMO-SEED-" + home.id, receiptNumber = "CC-SEED-" + home.id))
            }
            val (start, end) = DemoScenario.dayBounds(now)
            dao.householdsNow().filter { it.id in 2..4 }.forEach { home ->
                val mixed = home.id == 4
                dao.recordPickupOnce(PickupLogEntity(householdId = home.id, householdAddress = home.address,
                    residentName = home.residentName, collectorName = "Ramesh Kumar", timestamp = now,
                    status = if (mixed) "MIXED_WASTE" else "COLLECTED", segregationQuality = if (mixed) "MIXED" else "GOOD"), start, end)
            }
            val previous = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }.timeInMillis
            dao.insertPayment(PaymentEntity(householdId = demoId, monthYear = DemoScenario.period(previous), amount = 90.0,
                upiRef = "DEMO-104-001", paidAt = previous, receiptNumber = "CC-DEMO-001"))
            dao.insertComplaint(ComplaintEntity(title = "Community bin needs attention", category = "OVERFLOW",
                description = "The community bin beside the neighborhood park is full.", ward = DemoScenario.ward,
                address = "Road 12, Jubilee Hills", latitude = DemoScenario.latitude, longitude = DemoScenario.longitude,
                householdId = demoId, citizenPhone = DemoScenario.household().phone, createdAt = now - 7200000,
                slaDeadlineMillis = now + 79200000, assignedInspector = "Inspector R. Sharma"))
            dao.insertComplaint(ComplaintEntity(title = "Roadside litter cleared", category = "LITTER",
                description = "Loose waste near the community entrance.", ward = DemoScenario.ward,
                address = "Road 10, Jubilee Hills", latitude = DemoScenario.latitude, longitude = DemoScenario.longitude,
                householdId = demoId, citizenPhone = DemoScenario.household().phone, status = "RESOLVED",
                createdAt = now - 172800000, resolvedAt = now - 100800000, slaDeadlineMillis = now - 86400000,
                assignedInspector = "Inspector R. Sharma", resolutionProofPhoto = "demo:inspection"))
            dao.insertCrewAttendance(listOf(
                CrewAttendanceEntity(workerName = "Ramesh Kumar", role = "Collector", ward = DemoScenario.ward, date = DemoScenario.date(), status = "ON_ROUTE"),
                CrewAttendanceEntity(workerName = "Manjunath", role = "Driver", ward = DemoScenario.ward, date = DemoScenario.date(), status = "ON_ROUTE"),
                CrewAttendanceEntity(workerName = "S. Anitha", role = "Helper", ward = DemoScenario.ward, date = DemoScenario.date(), ppeChecked = false)
            ))
            dao.insertWeighbridgeLog(WeighbridgeLogEntity(vehicleNumber = DemoScenario.vehicle, driverName = "Manjunath",
                wasteType = "Wet waste", grossWeightKg = 8200.0, tareWeightKg = 4100.0, netWeightKg = 4100.0,
                treatmentPlant = "City recovery centre", timestamp = now))
            dao.insertAnnouncements(listOf(
                AnnouncementEntity(title = "A little sorting. A cleaner neighborhood.", content = "Keep wet waste and dry recyclables separate before the morning collection.", category = "Advisory", date = DemoScenario.date()),
                AnnouncementEntity(title = "Your morning collection", content = "Place your bins outside between 7:00 and 9:30 AM. Keep batteries and sanitary waste separate.", category = "Schedule", date = DemoScenario.date())
            ))
            dao.insertViolation(ViolationFineEntity(ward = DemoScenario.ward, location = "Road 36, Jubilee Hills",
                violatorName = "Sample commercial property", violationType = "Mixed waste", fineAmount = 1500.0))
        }
    }
}

