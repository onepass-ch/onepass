package ch.onepass.onepass.utils

object TimeProviderHolder {
  lateinit var instance: TimeProvider
    private set

  fun initialize(timeProvider: TimeProvider) {
    instance = timeProvider
  }
}
