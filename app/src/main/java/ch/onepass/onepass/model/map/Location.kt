package ch.onepass.onepass.model.map

import com.google.firebase.firestore.GeoPoint

/** Location data class for geographic coordinates and address */
data class Location(
    val coordinates: GeoPoint? = null,
    val name: String = "",
    val region: String? = null
)
