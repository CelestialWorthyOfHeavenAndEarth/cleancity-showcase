package com.example.data

// Fictional household locations and ward assignments for the presentation cast.
// Coordinates are approximate city reference points, never an actor's actual residence.
data class CityLocation(val city: String, val neighborhood: String, val wardNumber: Int,
    val latitude: Double, val longitude: Double) {
    val ward get() = "$city · Ward $wardNumber"
    val address get() = "$neighborhood, $city, Andhra Pradesh"
}

object AndhraPradeshLocations {
    const val region = "Andhra Pradesh"
    val cities = listOf(
        CityLocation("Vijayawada", "Benz Circle", 12, 16.5062, 80.6480),
        CityLocation("Rajahmundry", "Danavaipeta", 18, 17.0005, 81.8040),
        CityLocation("Guntur", "Brodipet", 24, 16.3067, 80.4365),
        CityLocation("Anantapur", "Ram Nagar", 16, 14.6819, 77.6006)
    )
    fun forCastIndex(index: Int) = cities[index % cities.size]
    fun forHousehold(home: HouseholdEntity) = cities.firstOrNull {
        home.ward == it.ward && home.address.endsWith(it.address)
    }
}
