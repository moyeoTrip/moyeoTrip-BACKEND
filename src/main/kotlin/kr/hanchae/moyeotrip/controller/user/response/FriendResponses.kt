package kr.hanchae.moyeotrip.controller.user.response

import java.time.LocalDateTime

data class FriendUserResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val introduction: String?,
)

data class FriendRequestResponse(
    val requestId: Long,
    val user: FriendUserResponse,
    val requestedAt: LocalDateTime,
)

data class FriendRequestListResponse(
    val totalCount: Int,
    val requests: List<FriendRequestResponse>,
)

data class FriendResponse(
    val friendshipId: Long,
    val user: FriendUserResponse,
    val lastActive: String?,
)

data class FriendListResponse(
    val totalCount: Int,
    val friends: List<FriendResponse>,
)
