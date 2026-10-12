package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndhraPradeshRefreshTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val oldWards = listOf("Ward 104 - Jubilee Hills, Hyderabad", "Ward 105 - Film Nagar, Hyderabad", "Ward 106 - Banjara Hills, Hyderabad")
    private fun oldHomes() = CityFixtures.households().mapIndexed { index, home ->
        home.copy(id = home.id + 100, ward = oldWards[index / 8],
            address = if (index == 0) "Jubilee Hills, Hyderabad, India" else
                "Plot " + (101 + index) + ", Road " + (10 + index % 8 * 2) + ", " + listOf("Jubilee Hills", "Film Nagar", "Banjara Hills")[index / 8])
    }

    @Test fun existingVersionFourRecordsMoveWithoutResettingIdentityOrActivity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CleanCityDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.cleanCityDao()
            val homes = oldHomes().map { it.copy(segregationScore = 63, streakDays = 7, feeStatus = "PAID", preferredLanguage = "Telugu") }
            dao.insertHouseholds(homes)
            dao.markSeedApplied(SeedMarkerEntity("tfi-neighborhood-v1"))
            val now = System.currentTimeMillis()
            val pickups = CityFixtures.pickups(homes, now).mapIndexed { i, log -> log.copy(id = i + 1, notes = "Saved collection note") }
            pickups.forEach { dao.insertPickupLog(it) }
            val reports = CityFixtures.reports(homes, now).mapIndexed { i, report ->
                report.copy(id = i + 1, latitude = 17.4326 + i * .0001, longitude = 78.4071 + i * .0001,
                    status = "RESOLVED", resolutionProofPhoto = "note:Saved inspection", resolvedAt = now)
            }
            reports.forEach { dao.insertComplaint(it) }
            val payment = PaymentEntity(id = 1, householdId = homes.first().id, monthYear = DemoScenario.period(), amount = 90.0,
                upiRef = "SAVED-PAYMENT", receiptNumber = "SAVED-RECEIPT")
            dao.insertPayment(payment)
            val crew = CityFixtures.crew(now).mapIndexed { i, member -> member.copy(id = i + 1, ward = oldWards[(i / 3) % 3], ppeChecked = true) }
            dao.insertCrewAttendance(crew)
            val weights = CityFixtures.weights(now).mapIndexed { i, log -> log.copy(id = i + 1,
                vehicleNumber = "TS-09-EA-" + (2041 + i % 3), treatmentPlant = listOf("Jubilee Hills recovery centre", "Film Nagar sorting centre")[i % 2]) }
            weights.forEach { dao.insertWeighbridgeLog(it) }
            val fines = CityFixtures.fines(now).mapIndexed { i, fine -> fine.copy(id = i + 1, ward = oldWards[i % 3],
                location = listOf("Market square", "Community lane", "Transit plaza")[i % 3], status = "PAID", fineAmount = 750.0) }
            fines.forEach { dao.insertViolation(it) }

            db.initializeDemo()
            val after = dao.householdsNow().associateBy { it.id }
            homes.forEachIndexed { i, before ->
                val fixture = CityFixtures.households()[i]
                assertEquals(before.copy(address = fixture.address, ward = fixture.ward), after[before.id])
            }
            assertEquals(24, after.size)
            assertEquals(listOf(payment), dao.getAllPayments().first())
            val afterPickups = dao.getAllPickupLogs().first().associateBy { it.id }
            pickups.forEach { assertEquals(it.copy(householdAddress = after[it.householdId]!!.address), afterPickups[it.id]) }
            val afterReports = dao.getAllComplaints().first().associateBy { it.id }
            reports.forEachIndexed { i, before ->
                val home = after[before.householdId]!!
                val location = AndhraPradeshLocations.forHousehold(home)!!
                assertEquals(before.copy(address = home.address, ward = home.ward,
                    latitude = location.latitude + i * .0001, longitude = location.longitude + i * .0001), afterReports[before.id])
            }
            val afterCrew = dao.getCrewAttendance().first().associateBy { it.id }
            crew.forEachIndexed { i, before -> assertEquals(before.copy(ward = CityFixtures.crew(now)[i].ward), afterCrew[before.id]) }
            val afterWeights = dao.getAllWeighbridgeLogs().first().associateBy { it.id }
            weights.forEachIndexed { i, before ->
                val fixture = CityFixtures.weights(now)[i]
                assertEquals(before.copy(vehicleNumber = fixture.vehicleNumber, treatmentPlant = fixture.treatmentPlant), afterWeights[before.id])
            }
            val afterFines = dao.getAllViolations().first().associateBy { it.id }
            fines.forEachIndexed { i, before ->
                val fixture = CityFixtures.fines(now)[i]
                assertEquals(before.copy(ward = fixture.ward, location = fixture.location), afterFines[before.id])
            }
            db.initializeDemo()
            assertEquals(after, dao.householdsNow().associateBy { it.id })
            assertEquals(afterReports, dao.getAllComplaints().first().associateBy { it.id })
            assertEquals(350, dao.getAllPickupLogs().first().size)
        } finally { db.close() }
    }

    @Test fun customLocationsAndIndependentReportsSurviveAndRefreshRunsOnlyOnce() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CleanCityDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.cleanCityDao()
            val homes = oldHomes().toMutableList()
            homes[1] = homes[1].copy(address = "My saved address")
            homes[2] = homes[2].copy(ward = "My saved ward")
            homes[3] = homes[3].copy(residentName = "My registered household")
            dao.insertHouseholds(homes)
            dao.markSeedApplied(SeedMarkerEntity("tfi-neighborhood-v1"))
            val independent = ComplaintEntity(id = 1, householdId = homes[0].id, title = "My report", description = "My own details",
                category = "OVERFLOW", ward = homes[0].ward, address = homes[0].address, latitude = 17.4326, longitude = 78.4071)
            dao.insertComplaint(independent)
            val snapshot = PickupLogEntity(id = 1, householdId = homes[0].id, householdAddress = "An earlier saved address",
                residentName = homes[0].residentName, collectorName = "Nani", status = "COLLECTED", segregationQuality = "GOOD")
            dao.insertPickupLog(snapshot)
            db.initializeDemo()
            (1..3).forEach { assertEquals(homes[it], dao.householdNow(homes[it].id)) }
            assertEquals(listOf(independent), dao.getAllComplaints().first())
            assertEquals(listOf(snapshot), dao.getAllPickupLogs().first())
            // Even restoring the former built-in location later is a user edit and must not be reapplied.
            dao.updateHousehold(homes[0])
            db.initializeDemo()
            assertEquals(homes[0], dao.householdNow(homes[0].id))
        } finally { db.close() }
    }
}
