package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.user.response.BlockedUserResponse
import kr.hanchae.moyeotrip.controller.user.response.UserBlockResponse

@Tag(name = "사용자 차단", description = "사용자 차단 및 차단 목록 관리 API")
@SecurityRequirement(name = "Authorization")
interface UserBlockAPISpec {
    @Operation(summary = "사용자 차단", description = "상대 사용자를 차단합니다. 서로의 공개 피드와 상대가 포함된 모임 검색 결과에서 제외됩니다.")
    fun block(
        @Parameter(hidden = true) loginUserId: Long,
        userId: Long,
    ): UserBlockResponse

    @Operation(summary = "사용자 차단 해제", description = "지정한 사용자의 차단 관계를 해제합니다.")
    fun unblock(
        @Parameter(hidden = true) loginUserId: Long,
        userId: Long,
    ): UserBlockResponse

    @Operation(summary = "차단 사용자 목록", description = "로그인 사용자가 차단한 사용자를 최신 차단순으로 반환합니다.")
    fun getBlockedUsers(
        @Parameter(hidden = true) userId: Long,
    ): List<BlockedUserResponse>
}
