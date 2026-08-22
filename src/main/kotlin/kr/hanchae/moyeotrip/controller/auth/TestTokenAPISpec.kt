package kr.hanchae.moyeotrip.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "테스트 인증", description = "로컬·개발 환경에서만 사용하는 임시 JWT 발급 API")
interface TestTokenAPISpec {
    @Operation(summary = "테스트 access token 발급", description = "DB에 존재하는 사용자 ID로 서비스 JWT를 발급합니다.")
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
                        examples = [ExampleObject(value = TestTokenSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun issueAccessToken(
        @Parameter(description = "테스트 access token을 발급할 사용자 ID", example = "1") userId: Long,
    ): TestAccessTokenResponse
}

private object TestTokenSwaggerExamples {
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
}
