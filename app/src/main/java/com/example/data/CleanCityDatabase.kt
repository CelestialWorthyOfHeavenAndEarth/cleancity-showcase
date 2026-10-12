package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

@Database(entities = [HouseholdEntity::class, PickupLogEntity::class, ComplaintEntity::class,
    PaymentEntity::class, WeighbridgeLogEntity::class, CrewAttendanceEntity::class,
    ViolationFineEntity::class, AnnouncementEntity::class, SeedMarkerEntity::class], version = 4, exportSchema = false)
abstract class CleanCityDatabase : RoomDatabase() {
    abstract fun cleanCityDao(): CleanCityDao
    suspend fun initializeDemo() = withTransaction {
        val dao = cleanCityDao()
        ensureDemoHousehold(dao)
        if (dao.seedApplied(AndhraPradeshRefresh.key) == 0) {
            AndhraPradeshRefresh.apply(dao)
            dao.markSeedApplied(SeedMarkerEntity(AndhraPradeshRefresh.key))
        }
        if (dao.seedApplied("tfi-neighborhood-v1") == 0) {
            populateInitialData(dao)
            dao.markSeedApplied(SeedMarkerEntity("tfi-neighborhood-v1"))
        }
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
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS seed_markers (`key` TEXT NOT NULL, appliedAt INTEGER NOT NULL, PRIMARY KEY(`key`))")
                db.execSQL("UPDATE payments SET upiRef = REPLACE(upiRef, 'DEMO-', 'CC-'), receiptNumber = REPLACE(receiptNumber, 'CC-DEMO-', 'CC-')")
                db.execSQL("UPDATE violations_fines SET violatorName = 'Lotus Market' WHERE violatorName = 'Sample commercial property'")
                db.execSQL("UPDATE pickup_logs SET collectorName = 'Nani' WHERE collectorName = 'Ramesh Kumar'")
                db.execSQL("UPDATE weighbridge_logs SET driverName = 'Ram Charan' WHERE driverName = 'Manjunath'")
                db.execSQL("UPDATE complaints SET assignedInspector = 'Venkatesh' WHERE assignedInspector IN ('Inspector R. Sharma', 'Insp. R. Sharma (Ward 42)')")
                db.execSQL("UPDATE crew_attendance SET workerName = CASE workerName WHEN 'Ramesh Kumar' THEN 'Nani' WHEN 'Manjunath' THEN 'Ram Charan' WHEN 'S. Anitha' THEN 'Sai Pallavi' ELSE workerName END")
            }
        }
        @Suppress("UNUSED_PARAMETER")
        fun getDatabase(context: Context, scope: CoroutineScope): CleanCityDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, CleanCityDatabase::class.java, "clean_city_database")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
        suspend fun ensureDemoHousehold(dao: CleanCityDao) {
            if (dao.getHouseholdByQrCode("BIN-JH-101") == null) {
                val demo = DemoScenario.household()
                dao.insertHouseholdIfMissing(if (dao.householdNow(1) == null) demo else demo.copy(id = 0))
            }
        }
        suspend fun populateInitialData(dao: CleanCityDao) {
            val legacy = mapOf("Mahesh babu" to "Mahesh Babu", "Lakshmi Rao" to "Prabhas", "Green Leaf Café" to "Allu Arjun",
                "Ravi Stores" to "Jr NTR", "Meera & Suresh" to "Ram Charan", "Arjun Reddy" to "Pawan Kalyan")
            CityFixtures.households().forEach { fixture ->
                val existing = dao.getHouseholdByQrCode(fixture.qrCode)
                if (existing == null) dao.insertHouseholdIfMissing(fixture.copy(id = 0))
                else if (legacy[existing.residentName] == fixture.residentName) dao.updateHousehold(existing.copy(residentName = fixture.residentName))
            }
            val homes = CityFixtures.households().mapNotNull { dao.getHouseholdByQrCode(it.qrCode) }
            val now = System.currentTimeMillis()
            CityFixtures.payments(homes, now).forEach { payment ->
                if (dao.paymentForPeriod(payment.householdId, payment.monthYear) == null) dao.insertPayment(payment)
            }
            CityFixtures.pickups(homes, now).forEach { log ->
                val (start, end) = DemoScenario.dayBounds(log.timestamp)
                if (dao.pickupForDay(log.householdId, start, end) == null) dao.insertPickupLog(log)
            }
            CityFixtures.reports(homes, now).forEach { dao.insertComplaint(it) }
            val existingCrew = dao.getCrewAttendance().first().map { it.workerName }.toSet()
            dao.insertCrewAttendance(CityFixtures.crew(now).filter { it.workerName !in existingCrew })
            CityFixtures.weights(now).forEach { dao.insertWeighbridgeLog(it) }
            val titles = dao.getAllAnnouncements().first().map { it.title }.toSet()
            dao.insertAnnouncements(CityFixtures.announcements(now).filter { it.title !in titles })
            CityFixtures.fines(now).forEach { dao.insertViolation(it) }
        }
    }
}
