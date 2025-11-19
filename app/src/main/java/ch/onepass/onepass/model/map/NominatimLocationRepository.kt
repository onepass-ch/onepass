package ch.onepass.onepass.model.map

import android.util.Log
import com.google.firebase.firestore.GeoPoint
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Nominatim-based implementation of LocationRepository for geocoding and location search.
 *
 * Uses OpenStreetMap's Nominatim API to search for locations by name and returns Location objects
 * with GeoPoint coordinates compatible with Firebase.
 *
 * @param client OkHttpClient instance for making HTTP requests
 */
class NominatimLocationRepository(private val client: OkHttpClient = OkHttpClient()) :
    LocationRepository {

  companion object {
    private const val TAG = "NominatimLocationRepo"
    private const val BASE_URL = "https://nominatim.openstreetmap.org"
    private const val USER_AGENT = "OnePass/1.0 (contact@onepass.ch)"
    private const val REFERER = "https://onepass.ch"

    // JSON configuration to be lenient with unknown keys
    private val jsonParser = Json { ignoreUnknownKeys = true }
  }

  /**
   * Parses JSON response from Nominatim API into a list of Location objects.
   *
   * @param body JSON response string from Nominatim
   * @return List of Location objects with GeoPoint coordinates
   */
  private fun parseBody(body: String): List<Location> {
    return try {
      val jsonArray = jsonParser.parseToJsonElement(body).jsonArray

      jsonArray.map { element ->
        val jsonObject = element.jsonObject

        // Nominatim returns lat/lon as Strings, we must parse them to Double
        val lat = jsonObject["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        val lon = jsonObject["lon"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        val displayName = jsonObject["display_name"]?.jsonPrimitive?.contentOrNull ?: ""

        // Extract region (canton) from address if available
        // Using kotlinx.serialization allows safe null checks
        val address = jsonObject["address"]?.jsonObject
        val region =
            address?.get("state")?.jsonPrimitive?.contentOrNull
                ?: address?.get("county")?.jsonPrimitive?.contentOrNull

        Location(coordinates = GeoPoint(lat, lon), name = displayName, region = region)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing JSON response", e)
      emptyList()
    }
  }

  /**
   * Searches for locations matching the given query using Nominatim API.
   *
   * @param query Search string (e.g., "EPFL, Lausanne" or "Geneva")
   * @return List of matching locations, empty list if no results or on error
   */
  override suspend fun search(query: String): List<Location> =
      withContext(Dispatchers.IO) {
        if (query.isBlank()) {
          Log.d(TAG, "Empty query provided")
          return@withContext emptyList()
        }

        // Build URL with query parameters
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("addressdetails", "1") // Include address details for region
                .addQueryParameter("limit", "10") // Limit results
                .build()

        // Create request with proper headers
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .build()

        try {
          val response = client.newCall(request).execute()
          response.use {
            if (!response.isSuccessful) {
              Log.w(TAG, "Nominatim API returned error: ${response.code}")
              return@withContext emptyList()
            }

            val body = response.body?.string()
            if (body != null) {
              Log.d(TAG, "Successfully fetched ${body.length} bytes from Nominatim")
              return@withContext parseBody(body)
            } else {
              Log.w(TAG, "Empty response body from Nominatim")
              return@withContext emptyList()
            }
          }
        } catch (e: IOException) {
          Log.e(TAG, "Network error while searching for location: $query", e)
          return@withContext emptyList()
        } catch (e: Exception) {
          Log.e(TAG, "Unexpected error while searching for location: $query", e)
          return@withContext emptyList()
        }
      }
}
