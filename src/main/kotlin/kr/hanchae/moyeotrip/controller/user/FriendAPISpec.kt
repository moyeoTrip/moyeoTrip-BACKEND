package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.user.response.FriendListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendResponse

@Tag(name = "친구", description = "친구 요청과 친구 관계 관리 API")
@SecurityRequirement(name = "Authorization")
interface FriendAPISpec {
    @Operation(summary = "친구 요청 보내기", description = "상대에게 친구 요청을 보냅니다. 거절된 요청에도 다시 신청할 수 있습니다.")
    fun sendRequest(
        @Parameter(hidden = true) loginUserId: Long,
        userId: Long,
    ): FriendRequestResponse

    @Operation(summary = "받은 친구 요청 수락", description = "받은 친구 요청을 수락하고 양방향 친구 관계를 생성합니다.")
    fun acceptRequest(
        @Parameter(hidden = true) userId: Long,
        requestId: Long,
    ): FriendResponse

    @Operation(summary = "받은 친구 요청 거절", description = "받은 친구 요청을 삭제합니다. 상대는 이후 다시 친구 요청을 보낼 수 있습니다.")
    fun rejectRequest(
        @Parameter(hidden = true) userId: Long,
        requestId: Long,
    )

    @Operation(summary = "보낸 친구 요청 취소", description = "로그인 사용자가 보낸 대기 중 친구 요청을 취소합니다.")
    fun cancelRequest(
        @Parameter(hidden = true) userId: Long,
        requestId: Long,
    )

    @Operation(summary = "받은 친구 요청 목록", description = "로그인 사용자에게 도착한 대기 중 친구 요청을 최신순으로 반환합니다.")
    fun getReceivedRequests(
        @Parameter(hidden = true) userId: Long,
    ): FriendRequestListResponse

    @Operation(summary = "보낸 친구 요청 목록", description = "로그인 사용자가 보낸 대기 중 친구 요청을 최신순으로 반환합니다.")
    fun getSentRequests(
        @Parameter(hidden = true) userId: Long,
    ): FriendRequestListResponse

    @Operation(summary = "친구 목록 조회", description = "친구 프로필과 최근 접속 정보를 반환합니다.")
    fun getFriends(
        @Parameter(hidden = true) userId: Long,
    ): FriendListResponse

    @Operation(summary = "친구 삭제", description = "지정한 사용자와의 양방향 친구 관계를 삭제합니다.")
    fun deleteFriend(
        @Parameter(hidden = true) userId: Long,
        friendId: Long,
    )
}
