package kr.hanchae.moyeotrip.controller.user.response

import java.time.LocalDateTime

data class UserBlockResponse(
    val userId: Long,
    val blocked: Boolean,
)

data class BlockedUserResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val blockedAt: LocalDateTime,
)
