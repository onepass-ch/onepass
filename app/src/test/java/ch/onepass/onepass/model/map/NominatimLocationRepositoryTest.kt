package ch.onepass.onepass.model.map

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NominatimLocationRepositoryTest {

  private lateinit var mockClient: OkHttpClient
  private lateinit var mockCall: Call
  private lateinit var repository: NominatimLocationRepository

  @Before
  fun setUp() {
    mockClient = mockk()
    mockCall = mockk()
    repository = NominatimLocationRepository(mockClient)
  }

  @Test
  fun testSearchSuccessfulWithResults() = runTest {
    val jsonResponse =
        """
            [
                {
                    "lat": "46.5197",
                    "lon": "6.6323",
                    "display_name": "EPFL, Lausanne",
                    "address": {
                        "state": "Vaud"
                    }
                }
            ]
        """
            .trimIndent()

    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body?.string() } returns jsonResponse
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("EPFL")

    assertEquals(1, result.size)
    assertEquals("EPFL, Lausanne", result[0].name)
    assertEquals("Vaud", result[0].region)
    assertEquals(46.5197, result[0].coordinates!!.latitude, 0.0001)
    assertEquals(6.6323, result[0].coordinates!!.longitude, 0.0001)
  }

  @Test
  fun testSearchWithMultipleResults() = runTest {
    val jsonResponse =
        """
            [
                {
                    "lat": "47.3769",
                    "lon": "8.5472",
                    "display_name": "Zurich",
                    "address": {"state": "Zurich"}
                },
                {
                    "lat": "47.4474",
                    "lon": "8.5510",
                    "display_name": "Zurich Airport",
                    "address": {"state": "Zurich"}
                }
            ]
        """
            .trimIndent()

    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body?.string() } returns jsonResponse
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("Zurich")

    assertEquals(2, result.size)
    assertEquals("Zurich", result[0].name)
    assertEquals("Zurich Airport", result[1].name)
  }

  @Test
  fun testSearchWithoutRegion() = runTest {
    val jsonResponse =
        """
            [
                {
                    "lat": "45.5",
                    "lon": "10.2",
                    "display_name": "Unknown Location",
                    "address": {}
                }
            ]
        """
            .trimIndent()

    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body?.string() } returns jsonResponse
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("test")

    assertEquals(1, result.size)
    assertNull(result[0].region)
  }

  @Test
  fun testSearchWithCountyInsteadOfState() = runTest {
    val jsonResponse =
        """
            [
                {
                    "lat": "45.5",
                    "lon": "10.2",
                    "display_name": "Some Location",
                    "address": {"county": "Milano"}
                }
            ]
        """
            .trimIndent()

    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body?.string() } returns jsonResponse
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("test")

    assertEquals(1, result.size)
    assertEquals("Milano", result[0].region)
  }

  @Test
  fun testSearchEmptyQuery() = runTest {
    val result = repository.search("")

    assertTrue(result.isEmpty())
  }

  @Test
  fun testSearchBlankQuery() = runTest {
    val result = repository.search("   ")

    assertTrue(result.isEmpty())
  }

  @Test
  fun testSearchUnsuccessfulResponse() = runTest {
    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns false
    every { mockResponse.code } returns 404
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("NonExistent")

    assertTrue(result.isEmpty())
  }

  @Test
  fun testSearchEmptyResponseBody() = runTest {
    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body?.string() } returns null
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("EPFL")

    assertTrue(result.isEmpty())
  }

  @Test
  fun testSearchNetworkError() = runTest {
    every { mockCall.execute() } throws IOException("Network error")
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("EPFL")

    assertTrue(result.isEmpty())
  }

  @Test
  fun testSearchUnexpectedError() = runTest {
    every { mockCall.execute() } throws RuntimeException("Unexpected error")
    every { mockClient.newCall(any()) } returns mockCall

    val result = repository.search("EPFL")

    assertTrue(result.isEmpty())
  }

  @Test
  fun testSearchRequestHasCorrectHeaders() = runTest {
    val mockResponse = mockk<Response>()
    every { mockResponse.isSuccessful } returns true
    every { mockResponse.body?.string() } returns "[]"
    every { mockResponse.close() } just runs
    every { mockCall.execute() } returns mockResponse
    every { mockClient.newCall(any()) } returns mockCall

    repository.search("test")

    verify {
      mockClient.newCall(match { it.header("User-Agent") == "OnePass/1.0 (contact@onepass.ch)" })
    }
  }
}
