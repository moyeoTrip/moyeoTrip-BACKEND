package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사용자 차단 또는 차단 해제 결과")
data class UserBlockResponse(
    @field:Schema(description = "대상 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "변경 후 차단 상태", example = "true")
    val blocked: Boolean,
)

@Schema(description = "차단한 사용자 정보")
data class BlockedUserResponse(
    @field:Schema(description = "차단한 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "차단한 사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "차단한 사용자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "사용자를 차단한 일시", example = "2026-09-01T12:00:00")
    val blockedAt: LocalDateTime,
)
