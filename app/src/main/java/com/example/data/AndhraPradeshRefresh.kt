package com.example.data

import kotlinx.coroutines.flow.first
import kotlin.math.abs

/** One-time content refresh within initializeDemo's transaction; no schema or identity changes. */
internal object AndhraPradeshRefresh {
    const val key = "tfi-andhra-pradesh-v1"
    private val oldWards = listOf("Ward 104 - Jubilee Hills, Hyderabad", "Ward 105 - Film Nagar, Hyderabad", "Ward 106 - Banjara Hills, Hyderabad")
    private fun oldAddress(index: Int) = if (index == 0) "Jubilee Hills, Hyderabad, India" else
        "Plot " + (101 + index) + ", Road " + (10 + index % 8 * 2) + ", " + listOf("Jubilee Hills", "Film Nagar", "Banjara Hills")[index / 8]

    suspend fun apply(dao: CleanCityDao) {
        val fixtures = CityFixtures.households()
        val legacyNames = listOf("Mahesh babu", "Lakshmi Rao", "Green Leaf Café", "Ravi Stores", "Meera & Suresh", "Arjun Reddy")
        val existing = dao.householdsNow().associateBy { it.qrCode }
        val moved = mutableMapOf<Int, Pair<HouseholdEntity, HouseholdEntity>>()
        fixtures.forEachIndexed { index, fixture ->
            val home = existing[fixture.qrCode] ?: return@forEachIndexed
            // Match the built-in identity AND location; never replace a custom address or ward.
            if ((home.residentName.equals(fixture.residentName, ignoreCase = true) || home.residentName == legacyNames.getOrNull(index)) &&
                home.address == oldAddress(index) && home.ward == oldWards[index / 8]) {
                val updated = home.copy(address = fixture.address, ward = fixture.ward)
                dao.updateHousehold(updated)
                moved[home.id] = home to updated
            }
        }
        dao.getAllPickupLogs().first().forEach { log ->
            moved[log.householdId]?.let { (old, updated) ->
                if (log.householdAddress == old.address)
                    dao.updatePickupLog(log.copy(householdAddress = updated.address))
            }
        }
        // Only relocate recognizable seeded reports. Independently entered reports keep their location.
        val legacyHomes = fixtures.mapIndexed { index, fixture ->
            fixture.copy(id = existing[fixture.qrCode]?.id ?: -(index + 1), address = oldAddress(index), ward = oldWards[index / 8])
        }
        val templates = CityFixtures.reports(legacyHomes, 0)
        dao.getAllComplaints().first().forEach { report ->
            val updated = moved[report.householdId]?.second ?: return@forEach
            val index = templates.indices.firstOrNull { index ->
                val expected = templates[index]
                expected.householdId == report.householdId && expected.title == report.title &&
                    expected.description == report.description && expected.category == report.category &&
                    expected.address == report.address && expected.ward == report.ward &&
                    abs(report.latitude - (17.4326 + index * .0001)) < .0000001 &&
                    abs(report.longitude - (78.4071 + index * .0001)) < .0000001
            } ?: return@forEach
            val location = AndhraPradeshLocations.forHousehold(updated) ?: return@forEach
            dao.updateComplaint(report.copy(address = updated.address, ward = updated.ward,
                latitude = location.latitude + index * .0001, longitude = location.longitude + index * .0001))
        }
        val crew = CityFixtures.crew(0)
        dao.getCrewAttendance().first().forEach { member ->
            val index = crew.indexOfFirst { it.workerName == member.workerName && it.role == member.role }
            if (index >= 0 && member.ward == oldWards[(index / 3) % 3])
                dao.updateCrewMember(member.copy(ward = crew[index].ward))
        }
        val weights = CityFixtures.weights(0)
        dao.getAllWeighbridgeLogs().first().forEach { log ->
            val index = listOf("TS-09-EA-2041", "TS-09-EA-2042", "TS-09-EA-2043").indexOf(log.vehicleNumber)
            if (index >= 0 && log.driverName == weights[index].driverName &&
                log.treatmentPlant in listOf("Jubilee Hills recovery centre", "Film Nagar sorting centre"))
                dao.updateWeighbridgeLog(log.copy(vehicleNumber = weights[index].vehicleNumber, treatmentPlant = weights[index].treatmentPlant))
        }
        val fines = CityFixtures.fines(0)
        dao.getAllViolations().first().forEach { fine ->
            val index = fines.indexOfFirst { it.violatorName == fine.violatorName && it.violationType == fine.violationType }
            if (index >= 0 && fine.ward == oldWards[index % 3] &&
                fine.location == listOf("Market square", "Community lane", "Transit plaza")[index % 3])
                dao.updateViolation(fine.copy(ward = fines[index].ward, location = fines[index].location))
        }
    }
}
