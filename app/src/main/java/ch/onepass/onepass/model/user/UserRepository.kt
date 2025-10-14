package ch.onepass.onepass.model.user

interface UserRepository {
  suspend fun getCurrentUser(): User?

  suspend fun getOrCreateUser(): User?

  suspend fun updateLastLogin(uid: String)
}
