package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.Event
import com.mapbox.geojson.Point
import kotlin.math.*

/** Represents an item on the map: either a single event pin or a cluster of events. */
sealed class MapRenderItem {
  /** The geographic point for the annotation. */
  abstract val point: Point
  /** A unique ID for the Mapbox annotation. */
  abstract val id: String

  /**
   * Represents a single event annotation.
   *
   * @property event The event associated with this pin.
   */
  data class Single(val event: Event) : MapRenderItem() {
    override val point: Point
      get() =
          Point.fromLngLat(
              event.location?.coordinates?.longitude ?: 0.0,
              event.location?.coordinates?.latitude ?: 0.0)

    override val id: String = event.eventId
  }

  /**
   * Represents a cluster of multiple events.
   *
   * @property events The list of all events contained within this cluster.
   * @property point The center point of the cluster (average of all contained points).
   */
  data class Cluster(val events: List<Event>, override val point: Point) : MapRenderItem() {
    override val id: String = "cluster_${point.latitude()}_${point.longitude()}"
    /** The number of events in the cluster. */
    val count: Int = events.size
  }
}

/**
 * Utility object containing logic for clustering map annotations (events) based on their location
 * and the current map zoom level.
 */
object MapClustering {

  /**
   * Stacks events that have the exact same latitude and longitude into a single
   * [MapRenderItem.Cluster]. Events with unique locations are converted to [MapRenderItem.Single].
   *
   * @param events The list of all events to process.
   * @return A list of [MapRenderItem]s where same-location events are grouped into clusters.
   */
  fun stackSameLocationEvents(events: List<Event>): List<MapRenderItem> {
    if (events.isEmpty()) return emptyList()

    return events
        .groupBy { it.location?.coordinates } // Group by GeoPoint
        .flatMap { (geoPoint, group) ->
          if (geoPoint == null) return@flatMap emptyList()

          val point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude)

          if (group.size == 1) {
            listOf(MapRenderItem.Single(group.first()))
          } else {
            // Create a cluster for items at the exact same spot (a "stack")
            listOf(MapRenderItem.Cluster(group, point))
          }
        }
  }

  /**
   * Clusters a list of [MapRenderItem]s (already stacked or single) based on the current zoom
   * level. Items closer than a radius determined by the zoom level are merged into a single
   * cluster.
   *
   * @param items The list of [MapRenderItem]s (singles and same-location stacks).
   * @param zoom The current map zoom level.
   * @return A list of clustered [MapRenderItem]s ready for rendering.
   */
  fun clusterItemsForZoom(items: List<MapRenderItem>, zoom: Double): List<MapRenderItem> {
    val radiusMeters =
        when {
          zoom >= 15 -> 0.0 // High zoom: Show exact stacks only
          zoom >= 11 -> 500.0 // Medium zoom: Small clusters
          else -> 5000.0 // Low zoom: Big clusters
        }

    if (radiusMeters <= 0.0) return items

    // Use a distance-based clustering algorithm (Union-Find)
    return clusterByDistance(items, radiusMeters)
  }

  // --- Union-Find Implementation for Distance Clustering ---

  /**
   * Performs distance-based clustering using the Union-Find algorithm.
   *
   * @param items The list of [MapRenderItem]s to cluster.
   * @param radius The maximum distance in meters for two items to be considered for clustering.
   * @return The final list of clustered and single [MapRenderItem]s.
   */
  private fun clusterByDistance(items: List<MapRenderItem>, radius: Double): List<MapRenderItem> {
    val n = items.size
    val parent = IntArray(n) { it } // Stores the parent index for Union-Find set

    // 1. Merge close pins
    for (i in 0 until n) {
      val p1 = items[i].point
      for (j in i + 1 until n) {
        val p2 = items[j].point
        if (distanceMeters(p1, p2) <= radius) {
          union(i, j, parent)
        }
      }
    }

    // 2. Build groups based on root parent
    val groups =
        items.indices
            .groupBy { find(it, parent) }
            .mapValues { (_, indices) -> indices.map { items[it] } }

    // 3. Aggregate groups into RenderItems
    return groups.values.map { group ->
      if (group.size == 1) {
        group.first()
      } else {
        // Flatten all events from the sub-clusters/singles in this group
        val allEvents =
            group.flatMap { item ->
              when (item) {
                is MapRenderItem.Single -> listOf(item.event)
                is MapRenderItem.Cluster -> item.events
              }
            }

        // Calculate average location for the cluster pin point
        val avgLat = group.map { it.point.latitude() }.average()
        val avgLng = group.map { it.point.longitude() }.average()

        MapRenderItem.Cluster(allEvents, Point.fromLngLat(avgLng, avgLat))
      }
    }
  }

  /**
   * Unites the sets containing elements a and b.
   *
   * @param a Index of the first element.
   * @param b Index of the second element.
   * @param parent The parent array.
   */
  private fun union(a: Int, b: Int, parent: IntArray) {
    val ra = find(a, parent)
    val rb = find(b, parent)
    if (ra != rb) parent[rb] = ra
  }

  /**
   * Finds the root parent of element x using path compression.
   *
   * @param x Index of the element.
   * @param parent The parent array.
   * @return The root parent index.
   */
  private fun find(x: Int, parent: IntArray): Int {
    var r = x
    // Find the root
    while (parent[r] != r) r = parent[r]
    var cur = x
    // Path compression
    while (parent[cur] != cur) {
      val p = parent[cur]
      parent[cur] = r
      cur = p
    }
    return r
  }

  /**
   * Calculates the Haversine distance between two geographic points in meters.
   *
   * @param p1 The first point.
   * @param p2 The second point.
   * @return The distance in meters.
   */
  private fun distanceMeters(p1: Point, p2: Point): Double {
    val r = 6371000.0 // Earth radius in meters
    val phi1 = p1.latitude().toRadians()
    val phi2 = p2.latitude().toRadians()
    val dPhi = (p2.latitude() - p1.latitude()).toRadians()
    val dLambda = (p2.longitude() - p1.longitude()).toRadians()
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
  }

  /** Converts a degree value to radians. */
  private fun Double.toRadians(): Double = this * Math.PI / 180.0
}
