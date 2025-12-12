package ch.onepass.onepass.ui.feed

import com.google.firebase.Timestamp
import java.time.Instant

interface TimeProvider {
  fun currentTimestamp(): Timestamp

  fun currentInstant(): Instant
}

class FirebaseTimeProvider : TimeProvider {
  override fun currentTimestamp(): Timestamp = Timestamp.now()

  override fun currentInstant(): Instant = Instant.ofEpochSecond(currentTimestamp().seconds)
}

class MockTimeProvider(private val fixedTime: Timestamp) : TimeProvider {
  override fun currentTimestamp(): Timestamp = fixedTime

  override fun currentInstant(): Instant = Instant.ofEpochSecond(fixedTime.seconds)
}
