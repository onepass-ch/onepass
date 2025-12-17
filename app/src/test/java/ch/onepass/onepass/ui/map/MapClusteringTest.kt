package ch.onepass.onepass.ui.map

import ch.onepass.onepass.model.event.Event
import ch.onepass.onepass.model.event.EventStatus
import ch.onepass.onepass.model.map.Location
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapClusteringTest {

  private val locationA = Location(GeoPoint(46.5, 6.5))
  private val locationB = Location(GeoPoint(46.6, 6.6)) // Distant from A

  private fun createEvent(id: String, location: Location): Event {
    return Event(
        eventId = id,
        title = "Event $id",
        status = EventStatus.PUBLISHED,
        location = location,
        startTime = Timestamp.now(),
        ticketsRemaining = 10)
  }

  @Test
  fun stackSameLocationEvents_groupsIdenticalCoordinates() {
    val event1 = createEvent("1", locationA)
    val event2 = createEvent("2", locationA) // Same location as 1
    val event3 = createEvent("3", locationB)

    val items = MapClustering.stackSameLocationEvents(listOf(event1, event2, event3))

    assertEquals(2, items.size)

    // Check Stack (Event 1 & 2)
    val cluster = items.find { it is MapRenderItem.Cluster } as? MapRenderItem.Cluster
    val single = items.find { it is MapRenderItem.Single } as? MapRenderItem.Single

    assertTrue(cluster != null)
    assertEquals(2, cluster?.events?.size)
    assertTrue(cluster?.events?.contains(event1) == true)
    assertTrue(cluster?.events?.contains(event2) == true)

    // Check Single (Event 3)
    assertTrue(single != null)
    assertEquals("3", single?.event?.eventId)
  }

  @Test
  fun clusterItemsForZoom_highZoom_returnsExactStacks() {
    val event1 = createEvent("1", locationA)
    val event2 = createEvent("2", locationB)
    val items = listOf(MapRenderItem.Single(event1), MapRenderItem.Single(event2))

    // Zoom 16 -> Radius is 0, no clustering by distance
    val result = MapClustering.clusterItemsForZoom(items, 16.0)

    assertEquals(2, result.size)
    assertTrue(result.all { it is MapRenderItem.Single })
  }

  @Test
  fun clusterItemsForZoom_lowZoom_clustersNearbyItems() {
    // Create two locations very close to each other
    val loc1 = Location(GeoPoint(46.5191, 6.5668))
    val loc2 = Location(GeoPoint(46.5192, 6.5669)) // Very close

    val item1 = MapRenderItem.Single(createEvent("1", loc1))
    val item2 = MapRenderItem.Single(createEvent("2", loc2))

    // Zoom 5 -> Large radius, should merge
    val result = MapClustering.clusterItemsForZoom(listOf(item1, item2), 5.0)

    assertEquals(1, result.size)
    assertTrue(result.first() is MapRenderItem.Cluster)
    assertEquals(2, (result.first() as MapRenderItem.Cluster).count)
  }
}
