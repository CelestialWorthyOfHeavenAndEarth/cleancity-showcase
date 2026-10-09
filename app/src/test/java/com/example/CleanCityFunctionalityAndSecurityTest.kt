package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.util.FileStorageHelper
import com.example.util.LocationHelper
import com.example.util.getAppStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CleanCityFunctionalityAndSecurityTest {

    private lateinit var context: Context
    private lateinit var database: CleanCityDatabase
    private lateinit var dao: CleanCityDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CleanCityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.cleanCityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppStringsSupportAllIndianLanguages() {
        val languages = listOf("English", "Telugu", "Hindi", "Kannada", "Tamil", "Malayalam", "Marathi", "Bengali", "Gujarati")
        for (lang in languages) {
            val strings = getAppStrings(lang)
            assertNotNull("Strings for $lang should not be null", strings)
            assertTrue("App name in $lang should not be blank", strings.appName.isNotBlank())
            assertTrue("Role citizen in $lang should not be blank", strings.roleCitizen.isNotBlank())
            assertTrue("Two bin rule description in $lang should not be blank", strings.greenBinDesc.isNotBlank())
        }
    }

    @Test
    fun testFileStorageHelperSafeBitmapSavingAndDirectoryIsolation() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val savedPath = FileStorageHelper.saveBitmap(context, bitmap)

        assertNotNull("Saved path should not be null", savedPath)
        val file = File(savedPath!!)
        assertTrue("Saved image file should exist", file.exists())
        assertTrue("Saved image file should have non-zero size", file.length() > 0)
        // Verify file is saved in private app files directory
        assertTrue("File should reside inside app files directory", file.canonicalPath.startsWith(context.filesDir.canonicalPath))

        // Cleanup
        file.delete()
    }

    @Test
    fun testLocationHelperReturnsSafeDefaultWhenNoPermissionOrGps() = runBlocking {
        // Without runtime location grant, LocationHelper must safely fallback without throwing SecurityException
        val loc = LocationHelper.getCurrentLocation(context)
        assertNotNull(loc)
        assertTrue("Latitude should be valid", loc.latitude in -90.0..90.0)
        assertTrue("Longitude should be valid", loc.longitude in -180.0..180.0)
        assertTrue("Address should not be empty", loc.address.isNotBlank())
    }

    @Test
    fun testDatabaseHouseholdCrudAndSegregationScoreLogic() = runBlocking {
        val household = HouseholdEntity(
            id = 101,
            qrCode = "BIN-TEST-101",
            residentName = "Test Resident",
            phone = "+91 98765 00001",
            address = "123 Test Street",
            ward = "Ward 42",
            category = "Household",
            segregationScore = 80,
            streakDays = 5,
            monthlyFee = 90.0,
            feeStatus = "DUE",
            preferredLanguage = "English"
        )
        dao.insertHousehold(household)

        val retrieved = dao.getHouseholdByQrCode("BIN-TEST-101")
        assertNotNull(retrieved)
        assertEquals("Test Resident", retrieved?.residentName)
        assertEquals("DUE", retrieved?.feeStatus)

        // Record good segregation: streak +1, score +2
        dao.recordGoodSegregation(101)
        val updated = dao.getHouseholdByQrCode("BIN-TEST-101")
        assertEquals(6, updated?.streakDays)
        assertEquals(82, updated?.segregationScore)

        // Mark fee paid
        dao.markFeePaid(101)
        val paidHousehold = dao.getHouseholdByQrCode("BIN-TEST-101")
        assertEquals("PAID", paidHousehold?.feeStatus)
    }

    @Test
    fun testComplaintFilingAndResolution() = runBlocking {
        val complaint = ComplaintEntity(
            id = 201,
            title = "Test Bin Overflow",
            category = "Overflowing Bin",
            description = "Test overflow description",
            ward = "Ward 42",
            address = "Park Corner",
            latitude = 12.9780,
            longitude = 77.6400,
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            slaDeadlineMillis = System.currentTimeMillis() + 86400000L,
            assignedInspector = "Inspector Sharma"
        )
        dao.insertComplaint(complaint)

        val activeList = dao.getActiveComplaints().first()
        assertTrue(activeList.any { it.id == 201 && it.status == "PENDING" })

        // Resolve complaint
        val resolvedAt = System.currentTimeMillis()
        dao.resolveComplaint(201, resolvedAt, "test_proof_photo")

        val activeAfter = dao.getActiveComplaints().first()
        assertFalse("Resolved complaint should not be in active list", activeAfter.any { it.id == 201 })

        val allList = dao.getAllComplaints().first()
        val resolved = allList.firstOrNull { it.id == 201 }
        assertNotNull(resolved)
        assertEquals("RESOLVED", resolved?.status)
        assertEquals("test_proof_photo", resolved?.resolutionProofPhoto)
    }

    @Test
    fun testWeighbridgeAndViolationsIntegrity() = runBlocking {
        val log = WeighbridgeLogEntity(
            id = 301,
            vehicleNumber = "KA-01-AB-1234",
            driverName = "Raju",
            wasteType = "Wet Waste",
            grossWeightKg = 6000.0,
            tareWeightKg = 3000.0,
            netWeightKg = 3000.0,
            treatmentPlant = "Waste Plant 1",
            timestamp = System.currentTimeMillis()
        )
        dao.insertWeighbridgeLog(log)

        val logs = dao.getAllWeighbridgeLogs().first()
        assertTrue(logs.any { it.id == 301 && it.netWeightKg == 3000.0 })

        val violation = ViolationFineEntity(
            id = 401,
            ward = "Ward 42",
            location = "Main Market",
            violatorName = "Illegal Dumper",
            violationType = "Night Dumping",
            fineAmount = 2500.0,
            status = "ISSUED",
            timestamp = System.currentTimeMillis()
        )
        dao.insertViolation(violation)

        val violations = dao.getAllViolations().first()
        assertTrue(violations.any { it.id == 401 && it.fineAmount == 2500.0 })
    }

    @Test
    fun testGeminiServiceChatAssistantProvidesAnswers() = runBlocking {
        val geminiService = com.example.ai.GeminiService()
        val englishReply = geminiService.chatAssistant(
            userMessage = "What is green bin?",
            language = "English",
            history = listOf(com.example.ai.ChatMessage("user", "Hello"))
        )
        assertNotNull(englishReply)
        assertTrue("English reply should not be blank", englishReply.isNotBlank())

        val teluguReply = geminiService.chatAssistant(
            userMessage = "చెత్త సేకరణ సమయం ఎప్పుడు?",
            language = "Telugu",
            history = emptyList()
        )
        assertNotNull(teluguReply)
        assertTrue("Telugu reply should not be blank", teluguReply.isNotBlank())
    }

    @Test
    fun testGeminiServiceExecutiveSummaryGeneratesValidBrief() = runBlocking {
        val geminiService = com.example.ai.GeminiService()
        val stats = com.example.ai.MunicipalStats(
            totalHouseholds = 1400,
            collectedToday = 1320,
            coveragePercent = 94.3,
            segregationRate = 88.5,
            wasteRecoveredTons = 42.0,
            costRecoveryPercent = 89.0,
            openComplaints = 2,
            resolvedComplaints = 18
        )
        val summary = geminiService.generateExecutiveSummary(stats)
        assertNotNull(summary)
        assertTrue("Summary should contain text", summary.isNotBlank())
    }

    @Test
    fun testDemoHouseholdMaheshBabuInJubileeHills() = runBlocking {
        CleanCityDatabase.ensureDemoHousehold(dao)
        val demo = dao.getHouseholdByQrCode("BIN-JH-101")
        assertNotNull(demo)
        assertEquals("Mahesh babu", demo?.residentName)
        assertEquals("Jubilee Hills, Hyderabad, India", demo?.address)
        assertEquals("Ward 104 - Jubilee Hills, Hyderabad", demo?.ward)
        assertEquals("English", demo?.preferredLanguage)
    }
}
