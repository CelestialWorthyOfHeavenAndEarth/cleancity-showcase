package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiService
import com.example.data.*
import com.example.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShowcaseRegressionTest {
    private lateinit var context: Context
    private lateinit var db: CleanCityDatabase
    private lateinit var dao: CleanCityDao
    private lateinit var repo: CleanCityRepository
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CleanCityDatabase::class.java).allowMainThreadQueries().build()
        dao = db.cleanCityDao(); repo = CleanCityRepository(dao)
    }
    @After fun cleanup() { db.close() }

    @Test fun initializationDoesNotReplaceEditedDataOrDuplicateFixtures() = runBlocking {
        db.initializeDemo()
        val original = dao.householdNow(1)!!
        dao.updateHousehold(original.copy(feeStatus = "PAID", segregationScore = 61, streakDays = 2, residentName = "Edited name"))
        val reportCount = dao.getAllComplaints().first().size
        val paymentCount = dao.getAllPayments().first().size
        db.initializeDemo()
        assertEquals("Edited name", dao.householdNow(1)!!.residentName)
        assertEquals(61, dao.householdNow(1)!!.segregationScore)
        assertEquals("PAID", dao.householdNow(1)!!.feeStatus)
        assertEquals(reportCount, dao.getAllComplaints().first().size)
        assertEquals(paymentCount, dao.getAllPayments().first().size)
    }

    @Test fun demoDoesNotOverwriteAnUnrelatedIdOne() = runBlocking {
        dao.insertHousehold(DemoScenario.household().copy(qrCode = "EXISTING", residentName = "Existing resident"))
        CleanCityDatabase.ensureDemoHousehold(dao)
        assertEquals("Existing resident", dao.householdNow(1)!!.residentName)
        assertNotEquals(1, dao.getHouseholdByQrCode("BIN-JH-101")!!.id)
        CleanCityDatabase.ensureDemoHousehold(dao)
        assertEquals(2, dao.householdsNow().size)
    }

    @Test fun concurrentPaymentsCreateOneReceiptAndUpdateTheObservedHousehold() = runBlocking {
        dao.insertHousehold(DemoScenario.household())
        val ids = (1..8).map { async(Dispatchers.IO) { repo.payMonthlyFee(1, "October 2026", 90.0, "DEMO-" + it) } }.awaitAll()
        assertEquals(1, ids.toSet().size)
        assertEquals(1, dao.getPaymentsForHousehold(1).first().size)
        assertEquals("PAID", dao.getHouseholdById(1).first()!!.feeStatus)
        assertTrue(dao.getPaymentsForHousehold(2).first().isEmpty())
    }

    @Test fun repeatedCollectionDoesNotInflateScoreOrStreak() = runBlocking {
        val home = DemoScenario.household().copy(segregationScore = 80, streakDays = 4)
        dao.insertHousehold(home)
        val first = repo.recordPickup(1, home.address, home.residentName, "Collector", "COLLECTED", "GOOD")
        val repeated = repo.recordPickup(1, home.address, home.residentName, "Collector", "COLLECTED", "GOOD")
        assertEquals(first, repeated)
        assertEquals(1, dao.getLogsForHousehold(1).first().size)
        assertEquals(82, dao.householdNow(1)!!.segregationScore)
        assertEquals(5, dao.householdNow(1)!!.streakDays)
    }

    @Test fun mixedWasteAndMissedPickupRemainDifferentOutcomes() = runBlocking {
        val home = DemoScenario.household().copy(segregationScore = 80)
        dao.insertHousehold(home)
        dao.insertHousehold(home.copy(id = 2, qrCode = "OTHER"))
        repo.recordPickup(1, home.address, home.residentName, "Collector", "MIXED_WASTE", "MIXED")
        repo.recordPickup(2, home.address, home.residentName, "Collector", "NOT_COLLECTED", "MIXED", "Resident absent")
        assertEquals(75, dao.householdNow(1)!!.segregationScore)
        assertEquals(80, dao.householdNow(2)!!.segregationScore)
        assertEquals(0, dao.householdNow(1)!!.streakDays)
    }

    @Test fun dailyMetricsExcludeOtherDaysAndCountEachHouseholdOnce() {
        val (start, end) = DemoScenario.dayBounds()
        fun log(id: Int, time: Long) = PickupLogEntity(householdId = id, householdAddress = "", residentName = "",
            collectorName = "", timestamp = time, status = "COLLECTED", segregationQuality = "GOOD")
        val logs = listOf(log(1, start - 1), log(2, start + 1000), log(2, start + 2000), log(3, end))
        val today = DemoScenario.today(logs, start + 3000)
        assertEquals(1, today.size)
        assertEquals(start + 2000, today.single().timestamp)
    }

    @Test fun reportsAreOwnedAndResolutionReachesTheCitizenQuery() = runBlocking {
        dao.insertHousehold(DemoScenario.household())
        val id = repo.submitComplaint("Overflow", "OVERFLOW", "A full bin", DemoScenario.ward, "Park",
            DemoScenario.latitude, DemoScenario.longitude, householdId = 1).toInt()
        assertEquals("OPEN", repo.getReports(1).first().single().status)
        assertTrue(repo.getReports(2).first().isEmpty())
        repo.escalateComplaint(id)
        assertEquals("ESCALATED", repo.getReports(1).first().single().status)
        repo.resolveComplaint(id, "demo:inspection")
        val closed = repo.getReports(1).first().single()
        assertEquals("RESOLVED", closed.status)
        assertNotNull(closed.resolvedAt)
        assertTrue(repo.activeComplaints.first().isEmpty())
    }

    @Test fun invalidAmountsWeightsAndFormsNeverWriteRecords() = runBlocking {
        dao.insertHousehold(DemoScenario.household())
        for (amount in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0, 10.0)) {
            assertTrue(runCatching { repo.payMonthlyFee(1, "October 2026", amount, "DEMO") }.isFailure)
        }
        for ((gross, tare) in listOf(10.0 to 20.0, 10.0 to 10.0, Double.NaN to 1.0, 100.0 to -1.0)) {
            assertTrue(runCatching { repo.logWeighbridge("TS-09", "Driver", "Wet", gross, tare, "Centre") }.isFailure)
        }
        assertTrue(runCatching { repo.issueViolation("Ward", "", "", "Mixed", 100.0) }.isFailure)
        assertTrue(runCatching { repo.issueViolation("Ward", "Park", "Name", "Mixed", Double.NaN) }.isFailure)
        assertTrue(runCatching { repo.submitComplaint("Report", "LITTER", "", "Ward", "Park", 0.0, 0.0) }.isFailure)
        assertTrue(runCatching { repo.submitComplaint("Report", "LITTER", "Waste", "Ward", "Park", Double.NaN, 0.0) }.isFailure)
        assertTrue(repo.allPayments.first().isEmpty())
        assertTrue(repo.allWeighbridgeLogs.first().isEmpty())
        assertTrue(repo.allViolations.first().isEmpty())
        assertTrue(repo.allComplaints.first().isEmpty())
    }

    @Test fun allLocalizedSuggestionsReceiveSpecificOfflineAnswers() = runBlocking {
        val service = GeminiService()
        for (language in DemoScenario.languages) {
            val s = getAppStrings(language)
            val sorting = service.chatAssistant(s.promptSegregation, language, emptyList())
            assertTrue(sorting.contains(s.greenBinDesc))
            assertTrue(service.chatAssistant(s.promptPickupTimes, language, emptyList()).contains("7:00"))
            assertTrue(service.chatAssistant(s.promptPayFee, language, emptyList()).contains("₹90"))
            assertEquals(showcaseText(language, "compostHelp"), service.chatAssistant(s.promptHomeCompost, language, emptyList()))
        }
    }

    @Test fun locationFailureIsExplicitlyMarkedAsSample() = runBlocking {
        assertTrue(LocationHelper.getCurrentLocation(context).isSample)
    }

    @Test fun migrationPreservesDataAndOnlyBackfillsUnambiguousOwners() = runBlocking {
        val name = "showcase-migration.db"
        context.deleteDatabase(name)
        val original = Room.databaseBuilder(context, CleanCityDatabase::class.java, name).allowMainThreadQueries().build()
        val oldDao = original.cleanCityDao()
        oldDao.insertHousehold(DemoScenario.household())
        oldDao.insertHousehold(DemoScenario.household().copy(id = 2, qrCode = "DUP-A", phone = "9000000000"))
        oldDao.insertHousehold(DemoScenario.household().copy(id = 3, qrCode = "DUP-B", phone = "+91 90000 00000"))
        for ((id, phone) in listOf(1 to "9876510101", 2 to "8111111111", 3 to "9000000000")) {
            oldDao.insertComplaint(ComplaintEntity(id = id, title = "Legacy report", category = "LITTER", description = "Keep me",
                ward = "Old ward", address = "Old address", citizenPhone = phone, status = "PENDING"))
        }
        original.close()
        val sqlite = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        val sql = sqlite.rawQuery("SELECT sql FROM sqlite_master WHERE name = 'complaints'", null).use { it.moveToFirst(); it.getString(0) }
        val ownerColumn = ", " + 96.toChar() + "householdId" + 96.toChar() + " INTEGER"
        assertTrue(sql.contains(ownerColumn))
        sqlite.execSQL("ALTER TABLE complaints RENAME TO snapshot_reports")
        sqlite.execSQL(sql.replace(ownerColumn, ""))
        val columns = sqlite.rawQuery("PRAGMA table_info(complaints)", null).use { c ->
            val names = mutableListOf<String>()
            while (c.moveToNext()) names.add(96.toChar() + c.getString(1) + 96.toChar())
            names.joinToString(",")
        }
        sqlite.execSQL("INSERT INTO complaints (" + columns + ") SELECT " + columns + " FROM snapshot_reports")
        sqlite.execSQL("DROP TABLE snapshot_reports")
        sqlite.execSQL("DROP TABLE room_master_table")
        sqlite.version = 2
        sqlite.close()
        val migrated = Room.databaseBuilder(context, CleanCityDatabase::class.java, name)
            .addMigrations(CleanCityDatabase.MIGRATION_2_3, CleanCityDatabase.MIGRATION_3_4).allowMainThreadQueries().build()
        try {
            val reports = migrated.cleanCityDao().getAllComplaints().first().associateBy { it.id }
            assertEquals(3, reports.size)
            assertEquals(1, reports[1]!!.householdId)
            assertNull(reports[2]!!.householdId)
            assertNull(reports[3]!!.householdId)
            assertEquals("OPEN", reports[1]!!.status)
            assertEquals("Old address", reports[1]!!.address)
            assertEquals("Keep me", reports[1]!!.description)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}

