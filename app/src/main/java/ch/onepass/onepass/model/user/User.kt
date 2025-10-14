package ch.onepass.onepass.model.user

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class Status {
  ACTIVE,
  BANNED,
  DELETED
}

data class User(
    // System fields from Google account
    @get:Exclude val uid: String = "", // uid from google account
    val email: String = "", // default by google account email
    @ServerTimestamp
    val createdAt: Date? = null, // timestamp of the first time when the user log in
    @ServerTimestamp
    val lastLoginAt: Date? = null, // timestamp, null when the user is never logged in
    val status: Status = Status.ACTIVE, // e.g. "active", "banned", "deleted"

    // User custom fields
    val displayName: String = "", // nickname for user custom profile
    val bio: String? = null, // Short description of the user
    val avatarUrl: String? = null, // avatar url link  (firebase storage)
    val coverUrl: String? = null, // profile cover picture url link (firebase storage)
    val phoneE164: String? = null, // e.g. +41998887766
    val country: String? = null // ISO 3166-1 alpha-2: "CH", "US", "CN"
)
