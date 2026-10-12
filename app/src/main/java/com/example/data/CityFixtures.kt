package com.example.data

import java.util.Calendar

// Fictional presentation records using a TFI cast, not claims about the people named.
object CityFixtures {
    const val collector = "Nani"
    const val driver = "Ram Charan"
    const val inspector = "Venkatesh"
    val wards = AndhraPradeshLocations.cities.map { it.ward }
    val cast = listOf("Mahesh Babu", "Prabhas", "Allu Arjun", "Jr NTR", "Ram Charan", "Pawan Kalyan",
        "Chiranjeevi", "Nagarjuna", "Venkatesh", "Nani", "Ravi Teja", "Vijay Deverakonda",
        "Rana Daggubati", "Samantha Ruth Prabhu", "Anushka Shetty", "Sai Pallavi", "Rashmika Mandanna",
        "Keerthy Suresh", "Ram Pothineni", "Naga Chaitanya", "Akhil Akkineni", "Sharwanand", "Adivi Sesh", "Sreeleela")
    val routeStops = listOf("Benz Circle · Community entrance", "Benz Circle · Neighborhood park", "Patamata · Market square",
        "Patamata · Transit stop", "Labbipet · Community hall", "Labbipet · Canal walk", "Moghalrajpuram · Central park", "Vijayawada · Recovery point")
    fun households(): List<HouseholdEntity> = cast.mapIndexed { index, name ->
        if (index == 0) DemoScenario.household() else HouseholdEntity(id = index + 1,
            qrCode = "BIN-JH-" + (101 + index), residentName = name, phone = "+91 90000 " + (10101 + index),
            address = "Plot " + (101 + index) + ", " + AndhraPradeshLocations.forCastIndex(index).address,
            ward = AndhraPradeshLocations.forCastIndex(index).ward, category = if (index % 7 == 2) "Hotel / Restaurant" else if (index % 7 == 3) "Commercial Shop" else "Household",
            monthlyFee = if (index % 7 == 2) 850.0 else if (index % 7 == 3) 350.0 else 90.0,
            feeStatus = if (index % 3 == 0) "OVERDUE" else if (index % 3 == 1) "PAID" else "DUE",
            segregationScore = 72 + index * 7 % 27, streakDays = ((if (index in 1..14) 0 else 1)..14).takeWhile { day -> (index + day) % 13 != 4 && (index + day) % 17 != 9 }.size)
    }
    fun daysAgo(days: Int, now: Long): Long = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
    fun payments(homes: List<HouseholdEntity>, now: Long) = homes.flatMap { home ->
        (0..5).filter { it != 0 || home.feeStatus == "PAID" }.map { month ->
            val time = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, -month) }.timeInMillis
            PaymentEntity(householdId = home.id, monthYear = DemoScenario.period(time), amount = home.monthlyFee,
                paidAt = time, upiRef = "CC-" + home.qrCode + "-" + DemoScenario.period(time), receiptNumber = "CC-" + home.id + "-" + DemoScenario.period(time).replace(" ", ""))
        }
    }
    fun pickups(homes: List<HouseholdEntity>, now: Long) = homes.flatMapIndexed { index, home ->
        (0..14).filter { it != 0 || index in 1..14 }.map { day ->
            val mixed = index != 0 && (index + day) % 13 == 4
            val missed = index != 0 && (index + day) % 17 == 9
            PickupLogEntity(householdId = home.id, householdAddress = home.address, residentName = home.residentName,
                collectorName = listOf(collector, "Sharwanand", "Adivi Sesh")[index / 8], timestamp = daysAgo(day, now),
                status = if (missed) "NOT_COLLECTED" else if (mixed) "MIXED_WASTE" else "COLLECTED",
                segregationQuality = if (mixed) "MIXED" else "GOOD", reasonNotCollected = if (missed) "Bin not outside" else null)
        }
    }
    fun reports(homes: List<HouseholdEntity>, now: Long) = (0..29).map { index ->
        val home = homes[if (index < 6) 0 else index % homes.size]
        val location = AndhraPradeshLocations.forHousehold(home)
        val status = listOf("OPEN", "IN_PROGRESS", "ESCALATED", "RESOLVED", "RESOLVED", "OPEN")[index % 6]
        val time = now - (index + 1) * 4 * 3_600_000L
        ComplaintEntity(title = listOf("Park bin needs attention", "Morning pickup missed", "Waste beside the market", "Footpath needs a clean", "Collection point needs attention")[index % 5],
            category = listOf("OVERFLOW", "MISSED", "DUMPING", "LITTER", "OVERFLOW")[index % 5],
            description = listOf("The bin is full near the park entrance. Please arrange an extra collection.",
                "Our bins were outside before the morning collection window.", "Bags have collected beside the market loading area.",
                "Loose paper and packaging are scattered along the footpath.", "The collection point needs follow-up inspection.")[index % 5],
            ward = home.ward, address = home.address, latitude = location?.let { it.latitude + index * 0.0001 } ?: 0.0,
            longitude = location?.let { it.longitude + index * 0.0001 } ?: 0.0, householdId = home.id, citizenPhone = home.phone,
            status = status, createdAt = time, slaDeadlineMillis = time + 86_400_000,
            resolvedAt = if (status == "RESOLVED") time + 6 * 3_600_000 else null, assignedInspector = inspector,
            resolutionProofPhoto = if (status == "RESOLVED") "note:Area cleared and segregated bins checked during field inspection." else null)
    }
    fun crew(now: Long) = listOf("Nani", "Ram Charan", "Sai Pallavi", "Sharwanand", "Rana Daggubati", "Sreeleela",
        "Adivi Sesh", "Ram Pothineni", "Keerthy Suresh", "Ravi Teja", "Naga Chaitanya", "Anushka Shetty").mapIndexed { index, name ->
        CrewAttendanceEntity(workerName = name, role = listOf("Collector", "Driver", "Helper")[index % 3], ward = wards[(index / 3) % wards.size],
            date = DemoScenario.date(now), status = if (index == 10) "LEAVE" else if (index % 4 == 0) "PRESENT" else "ON_ROUTE",
            checkInTime = if (index % 2 == 0) "06:30 AM" else "06:45 AM", ppeChecked = index !in listOf(3, 7, 10))
    }
    fun weights(now: Long) = (0..17).map { index ->
        val net = 2400.0 + index % 5 * 350
        WeighbridgeLogEntity(vehicleNumber = listOf(DemoScenario.vehicle, "AP-39-EA-2042", "AP-39-EA-2043")[index % 3],
            driverName = listOf(driver, "Rana Daggubati", "Ram Pothineni")[index % 3], wasteType = if (index % 2 == 0) "Wet waste" else "Dry recyclable",
            grossWeightKg = 4100 + net, tareWeightKg = 4100.0, netWeightKg = net,
            treatmentPlant = listOf("Vijayawada recovery centre", "Rajahmundry sorting centre", "Guntur recovery centre")[index % 3], timestamp = daysAgo(index / 3, now))
    }
    fun announcements(now: Long) = listOf(
        "A little sorting. A cleaner neighborhood." to "Keep food scraps in the green bin and clean recyclables in the blue bin.",
        "Your morning collection" to "Place bins outside between 7:00 and 9:30 AM. Keep access clear for the collection crew.",
        "Batteries need a separate bag" to "Keep batteries and electronics out of household bins. Use the ward collection point.",
        "Compost at home" to "Layer fruit and vegetable scraps with dry leaves. Keep your compost airy and lightly moist.",
        "Weekend cleanup walk" to "Join the neighborhood cleanup at the park entrance on Saturday morning.",
        "Keep your household tag visible" to "Place the QR tag near your collection point so your pickup can be recorded quickly."
    ).mapIndexed { i, item -> AnnouncementEntity(title = item.first, content = item.second,
        category = listOf("Community", "Collection", "Sorting")[i % 3], date = DemoScenario.date(daysAgo(i, now))) }
    fun fines(now: Long) = (0..8).map { index -> ViolationFineEntity(ward = wards[index % wards.size],
        location = listOf("Market square", "Community lane", "Transit plaza")[index % 3] + ", " + AndhraPradeshLocations.forCastIndex(index).city,
        violatorName = listOf("Lotus Market", "Silver Screen Café", "Green Avenue Stores", "Parkside Kitchen", "Sunrise Plaza", "Lotus Food Court", "Central Arcade", "Lakeside Kiosk", "Garden Lane Retail")[index],
        violationType = listOf("Mixed waste", "Uncovered waste container", "Waste outside collection point")[index % 3],
        fineAmount = listOf(500.0, 1000.0, 1500.0)[index % 3], status = if (index % 3 == 0) "PAID" else "ISSUED", timestamp = daysAgo(index, now)) }
}
