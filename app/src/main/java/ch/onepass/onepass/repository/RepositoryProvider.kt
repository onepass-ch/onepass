package ch.onepass.onepass.repository

import ch.onepass.onepass.model.event.EventRepository
import ch.onepass.onepass.model.event.EventRepositoryFirebase

object RepositoryProvider {
  private val _repository: EventRepository by lazy { EventRepositoryFirebase() }
  var repository: EventRepository = _repository
}
