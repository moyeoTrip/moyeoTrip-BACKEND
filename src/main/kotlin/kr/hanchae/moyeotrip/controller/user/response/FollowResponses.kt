package kr.hanchae.moyeotrip.controller.user.response

data class FollowResponse(
    val userId: Long,
    val following: Boolean,
)

data class FollowListResponse(
    val totalCount: Long,
    val users: List<FollowUserResponse>,
)

data class FollowUserResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val introduction: String?,
)
