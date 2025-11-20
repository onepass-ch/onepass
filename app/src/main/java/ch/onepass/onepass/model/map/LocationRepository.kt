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
}
