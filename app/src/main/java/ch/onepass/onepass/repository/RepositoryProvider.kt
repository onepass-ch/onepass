package ch.onepass.onepass.repository

import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase

object RepositoryProvider {
  private val _eventRepository: EventRepository by lazy { EventRepositoryFirebase() }
  var eventRepository: EventRepository = _eventRepository
}
