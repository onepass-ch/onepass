package ch.onepass.onepass.repository

import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase
import ch.onepass.onepass.model.storage.StorageRepository
import ch.onepass.onepass.model.storage.StorageRepositoryFirebase
import ch.onepass.onepass.model.map.LocationRepository
import ch.onepass.onepass.model.map.NominatimLocationRepository

object RepositoryProvider {
  private val _eventRepository: EventRepository by lazy { EventRepositoryFirebase() }
  var eventRepository: EventRepository = _eventRepository

  private val _storageRepository: StorageRepository by lazy { StorageRepositoryFirebase() }
  var storageRepository: StorageRepository = _storageRepository
  private val _locationRepository: LocationRepository by lazy { NominatimLocationRepository() }
  var locationRepository: LocationRepository = _locationRepository
}
