package ch.onepass.onepass.model.map

/** Repository interface for location-based operations. */
interface LocationRepository {
  /**
   * Searches for locations matching the given query.
   *
   * @param query Search string
   * @return List of matching locations
   */
  suspend fun search(query: String): List<Location>

  /**
   * Reverse geocodes coordinates to get location name and details.
   *
   * @param latitude Latitude coordinate
   * @param longitude Longitude coordinate
   * @return Location object with name and region, or null if lookup fails
   */
  suspend fun reverseGeocode(latitude: Double, longitude: Double): Location?
}
