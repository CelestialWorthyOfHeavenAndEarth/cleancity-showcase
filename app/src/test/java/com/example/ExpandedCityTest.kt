package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.util.HouseholdQr
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpandedCityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun completeCastDatasetHasConnectedHistoryAcrossThreeWards() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CleanCityDatabase::class.java).allowMainThreadQueries().build()
        try {
            db.initializeDemo()
            val dao = db.cleanCityDao()
            val homes = dao.householdsNow()
            assertEquals(24, homes.size)
            assertEquals(CityFixtures.cast.toSet(), homes.map { it.residentName }.toSet())
            assertEquals(3, homes.map { it.ward }.distinct().size)
            assertEquals(24, homes.map { it.qrCode }.distinct().size)
            assertEquals(350, dao.getAllPickupLogs().first().size)
            assertEquals(14, DemoScenario.today(dao.getAllPickupLogs().first()).size)
            assertEquals(128, dao.getAllPayments().first().size)
            assertEquals(30, dao.getAllComplaints().first().size)
            assertEquals(12, dao.getCrewAttendance().first().size)
            assertEquals(18, dao.getAllWeighbridgeLogs().first().size)
            assertEquals(9, dao.getAllViolations().first().size)
            assertEquals(6, dao.getAllAnnouncements().first().size)
            homes.forEach { home ->
                assertTrue(dao.getLogsForHousehold(home.id).first().size >= 14)
                assertTrue(dao.getPaymentsForHousehold(home.id).first().size >= 5)
            }
            assertEquals(setOf("OPEN", "IN_PROGRESS", "ESCALATED", "RESOLVED"), dao.getAllComplaints().first().map { it.status }.toSet())
            db.initializeDemo()
            assertEquals(350, dao.getAllPickupLogs().first().size)
            assertEquals(30, dao.getAllComplaints().first().size)
            assertEquals(128, dao.getAllPayments().first().size)
        } finally { db.close() }
    }

    @Test fun versionThreeUpgradePreservesEditsAndEnrichesOnlyOnce() = runBlocking {
        val name = "city-v3-upgrade.db"
        context.deleteDatabase(name)
        val original = Room.databaseBuilder(context, CleanCityDatabase::class.java, name).allowMainThreadQueries().build()
        original.cleanCityDao().insertHousehold(DemoScenario.household().copy(residentName = "Mahesh babu", address = "Saved address", segregationScore = 61, feeStatus = "PAID"))
        original.cleanCityDao().insertPayment(PaymentEntity(householdId = 1, monthYear = DemoScenario.period(), amount = 90.0,
            upiRef = "EXISTING", receiptNumber = "EXISTING-RECEIPT"))
        original.close()
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use {
            it.execSQL("DROP TABLE seed_markers")
            it.execSQL("DROP TABLE room_master_table")
            it.version = 3
        }
        val upgraded = Room.databaseBuilder(context, CleanCityDatabase::class.java, name)
            .addMigrations(CleanCityDatabase.MIGRATION_3_4).allowMainThreadQueries().build()
        try {
            upgraded.initializeDemo()
            val dao = upgraded.cleanCityDao()
            assertEquals("Mahesh Babu", dao.householdNow(1)!!.residentName)
            assertEquals("Saved address", dao.householdNow(1)!!.address)
            assertEquals(61, dao.householdNow(1)!!.segregationScore)
            assertEquals("PAID", dao.householdNow(1)!!.feeStatus)
            assertEquals("EXISTING-RECEIPT", dao.paymentForPeriod(1, DemoScenario.period())!!.receiptNumber)
            val sizes = listOf(dao.getAllPayments().first().size, dao.getAllComplaints().first().size, dao.getAllWeighbridgeLogs().first().size)
            upgraded.initializeDemo()
            assertEquals(sizes, listOf(dao.getAllPayments().first().size, dao.getAllComplaints().first().size, dao.getAllWeighbridgeLogs().first().size))
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }

    @Test fun realQrImagesRoundTripEveryHouseholdAndProducePrintableTags() {
        val output = File("build/household-tags").apply { mkdirs() }
        val cards = CityFixtures.households().joinToString("\n") { home ->
            val bitmap = HouseholdQr.bitmap(home.qrCode)
            try {
                assertEquals(home.qrCode, HouseholdQr.decode(bitmap))
                assertEquals(android.graphics.Color.WHITE, bitmap.getPixel(0, 0))
                File(output, home.qrCode + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally { bitmap.recycle() }
            "<article><h2>" + home.residentName + "</h2><img src='" + home.qrCode + ".png' alt='Household QR'><strong>" + home.qrCode + "</strong><p>" + home.ward + "</p></article>"
        }
        File(output, "index.html").writeText("""<!doctype html><html lang="en"><meta charset="utf-8"><title>CleanCity household tags</title>
            <style>body{font:15px system-ui;background:#ffffff;color:#166844;margin:32px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:20px}article{border:1px solid #dfe8e1;border-radius:20px;padding:20px;text-align:center;background:white;break-inside:avoid}img{display:block;width:220px;max-width:100%;margin:auto}h2{font-size:20px}strong{display:block;font-size:16px}p{font-size:12px}@media print{body{margin:0}main{grid-template-columns:repeat(3,1fr)}article{padding:12px}img{width:170px}}</style>
            <h1>CleanCity · Household tags</h1><p>Scan with the collector's camera, or import a QR image.</p><main>""" + cards + "</main></html>")
    }

    @Test fun malformedCodesAndNonQrImagesAreRejected() {
        assertNull(HouseholdQr.normalize("https://example.com"))
        assertNull(HouseholdQr.normalize("BIN-" + "A".repeat(100)))
        assertEquals("BIN-JH-101", HouseholdQr.normalize(" bin-jh-101 "))
        val blank = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        blank.eraseColor(android.graphics.Color.WHITE)
        try { assertNull(HouseholdQr.decode(blank)) } finally { blank.recycle() }
    }
}
