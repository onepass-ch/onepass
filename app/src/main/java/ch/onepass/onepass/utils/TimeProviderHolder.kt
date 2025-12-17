package ch.onepass.onepass.utils

object TimeProviderHolder {
  private lateinit var _instance: TimeProvider

  val instance: TimeProvider
    get() =
        if (::_instance.isInitialized) _instance
        else
            error("TimeProviderHolder not initialized. Call TimeProviderHolder.initialize() first.")

  fun initialize(timeProvider: TimeProvider) {
    _instance = timeProvider
  }
}
