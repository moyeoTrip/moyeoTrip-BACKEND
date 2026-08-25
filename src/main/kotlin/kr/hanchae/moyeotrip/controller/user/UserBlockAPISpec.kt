package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.user.response.BlockedUserResponse
import kr.hanchae.moyeotrip.controller.user.response.UserBlockResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "사용자 차단", description = "사용자 차단 및 차단 목록 관리 API")
interface UserBlockAPISpec {
    @Operation(summary = "사용자 차단", description = "상대 사용자를 차단합니다. 서로의 공개 피드와 상대가 포함된 모임 검색 결과에서 제외됩니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사용자 차단 성공",
                content = [Content(schema = Schema(implementation = UserBlockResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "자기 자신은 차단할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserBlockSwaggerExamples.SELF_BLOCK_NOT_ALLOWED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 또는 차단 대상을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = UserBlockSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "차단 대상 사용자 없음", value = UserBlockSwaggerExamples.USER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun block(
        @Parameter(hidden = true) loginUserId: Long,
        @Parameter(description = "차단할 상대 사용자 ID", example = "202")
        userId: Long,
    ): UserBlockResponse

    @Operation(summary = "사용자 차단 해제", description = "지정한 사용자의 차단 관계를 해제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사용자 차단 해제 성공",
                content = [Content(schema = Schema(implementation = UserBlockResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserBlockSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun unblock(
        @Parameter(hidden = true) loginUserId: Long,
        @Parameter(description = "차단 해제할 상대 사용자 ID", example = "202")
        userId: Long,
    ): UserBlockResponse

    @Operation(summary = "차단 사용자 목록", description = "로그인 사용자가 차단한 사용자를 최신 차단순으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "차단 사용자 목록 조회 성공",
                content = [Content(schema = Schema(implementation = BlockedUserResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserBlockSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getBlockedUsers(
        @Parameter(hidden = true) userId: Long,
    ): List<BlockedUserResponse>
}

private object UserBlockSwaggerExamples {
    const val SELF_BLOCK_NOT_ALLOWED = """{"code":40032,"errorMessage":"자기 자신을 차단할 수 없습니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
}
