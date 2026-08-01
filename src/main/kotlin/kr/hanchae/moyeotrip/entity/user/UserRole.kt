package kr.hanchae.moyeotrip.entity.user

enum class UserRole(
    val description: String,
) {
    ROLE_USER("사용자"),
    ROLE_ADMIN("관리자"),
}
