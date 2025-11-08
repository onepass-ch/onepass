package ch.onepass.onepass.model.staff

data class StaffSearchResult(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null
)
