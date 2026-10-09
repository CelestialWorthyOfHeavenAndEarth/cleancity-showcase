package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DemoScenario {
    const val ward = "Ward 104 - Jubilee Hills, Hyderabad"
    const val address = "Jubilee Hills, Hyderabad, India"
    const val vehicle = "TS-09-EA-2041"
    const val latitude = 17.4326
    const val longitude = 78.4071
    const val otp = "482910"
    val languages = listOf("English", "Telugu", "Hindi", "Kannada", "Tamil", "Malayalam", "Marathi", "Bengali", "Gujarati")
    fun household() = HouseholdEntity(
        id = 1, qrCode = "BIN-JH-101", residentName = "Mahesh babu", phone = "+91 98765 10101",
        address = address, ward = ward, category = "Household",
        segregationScore = 96, streakDays = 14, monthlyFee = 90.0, feeStatus = "DUE"
    )
    fun dayBounds(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }
    fun period(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(now)
    fun date(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now)
    fun timestamp(now: Long): String =
        SimpleDateFormat("dd MMM · h:mm a", Locale.getDefault()).format(now)
    fun money(value: Double): String = "₹" + java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).format(value)
    fun today(logs: List<PickupLogEntity>, now: Long = System.currentTimeMillis()): List<PickupLogEntity> {
        val (start, end) = dayBounds(now)
        return logs.filter { it.timestamp >= start && it.timestamp < end }.sortedByDescending { it.timestamp }.distinctBy { it.householdId }
    }
}

