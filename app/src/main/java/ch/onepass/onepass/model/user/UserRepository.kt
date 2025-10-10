package ch.onepass.onepass.model.user

import com.google.firebase.auth.FirebaseUser

interface UserRepository {
    suspend fun getUser(userId: String): User?
    suspend fun getOrCreateUser(user: FirebaseUser): User
}
