package ch.onepass.onepass.model.user

/** Enum representing the type of user search to perform. */
enum class UserSearchType {
  /** Search by display name */
  DISPLAY_NAME,

  /** Search by email address */
  EMAIL;

  /** Returns the string value used in the Firebase Cloud Function payload. */
  fun toSearchTypeString(): String {
    return when (this) {
      DISPLAY_NAME -> "NAME"
      EMAIL -> "EMAIL"
    }
  }
}
