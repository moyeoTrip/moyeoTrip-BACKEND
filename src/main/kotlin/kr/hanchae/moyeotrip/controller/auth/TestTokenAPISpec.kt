package kr.hanchae.moyeotrip.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse

@Tag(name = "테스트 인증", description = "로컬·개발 환경에서만 사용하는 임시 JWT 발급 API")
interface TestTokenAPISpec {
    @Operation(summary = "테스트 access token 발급", description = "DB에 존재하는 사용자 ID로 서비스 JWT를 발급합니다.")
    fun issueAccessToken(userId: Long): TestAccessTokenResponse
}
