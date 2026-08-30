package kr.hanchae.moyeotrip.controller.test

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse
import kr.hanchae.moyeotrip.controller.test.response.TestCompletedChatRoomResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "테스트 지원", description = "프론트엔드 QA 임시 API입니다. 현재 운영 프로필에도 임시로 등록되므로 QA 종료 후 반드시 비활성화해야 합니다.")
interface TestSupportAPISpec {
    @Operation(hidden = true)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "테스트 access token 발급 성공",
                content = [Content(schema = Schema(implementation = TestAccessTokenResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TestSupportSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun issueAccessToken(
        @Parameter(description = "테스트 access token을 발급할 사용자 ID", example = "1") userId: Long,
    ): TestAccessTokenResponse

    @Operation(
        summary = "QA용 여행 완료 처리",
        description = """
            요청 본문이나 현재 채팅방 상태와 관계없이 채팅방을 확정 상태로 바꿉니다.
            여행 시작일과 모집 마감일을 과거로 옮기고, 숙박 여행이면 종료일도 과거로 옮깁니다.
            완료 상태 enum은 없으며, 완료 여부는 확정 상태와 종료일로 판정합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "QA용 여행 완료 처리 성공",
                content = [Content(schema = Schema(implementation = TestCompletedChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TestSupportSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun completeChatRoom(
        @Parameter(description = "완료 처리할 채팅방 ID", example = "101") roomId: Long,
    ): TestCompletedChatRoomResponse
}

private object TestSupportSwaggerExamples {
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
}
