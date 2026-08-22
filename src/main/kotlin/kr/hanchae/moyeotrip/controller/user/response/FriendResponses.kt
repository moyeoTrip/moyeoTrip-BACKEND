package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "친구 또는 친구 요청 상대 사용자 정보")
data class FriendUserResponse(
    @field:Schema(description = "상대 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "상대 사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "상대 사용자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "상대 사용자 자기소개", nullable = true)
    val introduction: String?,
)

@Schema(description = "친구 요청 정보")
data class FriendRequestResponse(
    @field:Schema(description = "친구 요청 ID", example = "31")
    val requestId: Long,
    @field:Schema(description = "친구 요청 상대 사용자 정보")
    val user: FriendUserResponse,
    @field:Schema(description = "친구 요청 생성 일시", example = "2026-09-01T12:00:00")
    val requestedAt: LocalDateTime,
)

@Schema(description = "친구 요청 목록 응답")
data class FriendRequestListResponse(
    @field:Schema(description = "친구 요청 수", example = "2")
    val totalCount: Int,
    @field:Schema(description = "친구 요청 목록")
    val requests: List<FriendRequestResponse>,
)

@Schema(description = "친구 관계 정보")
data class FriendResponse(
    @field:Schema(description = "친구 관계 ID", example = "15")
    val friendshipId: Long,
    @field:Schema(description = "친구 사용자 정보")
    val user: FriendUserResponse,
    @field:Schema(description = "친구의 최근 접속 상대 시간. 접속 이력이 없으면 null", example = "1시간 전", nullable = true)
    val lastActive: String?,
)

@Schema(description = "친구 목록 응답")
data class FriendListResponse(
    @field:Schema(description = "친구 수", example = "8")
    val totalCount: Int,
    @field:Schema(description = "친구 목록")
    val friends: List<FriendResponse>,
)
