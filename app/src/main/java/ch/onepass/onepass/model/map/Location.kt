package ch.onepass.onepass.model.map

import com.google.firebase.firestore.GeoPoint

/**
 * Represents a geographic location with optional coordinates and descriptive metadata.
 *
 * This data class is used to encapsulate location information for events and other geospatial
 * features in the application. It supports integration with Firebase Firestore via [GeoPoint].
 *
 * @property coordinates The geographic coordinates as a Firestore [GeoPoint] (latitude, longitude).
 *   May be `null` if the location is not tied to a specific point on the map.
 * @property name A human-readable name for the location (e.g., "EPFL", "Zurich HB"). Defaults to an
 *   empty string if not provided.
 * @property region An optional administrative region or canton (e.g., "Vaud", "Zurich"). May be
 *   `null` if the region is unknown or not applicable.
 */
data class Location(
    val coordinates: GeoPoint? = null,
    val name: String = "",
    val region: String? = null
)
