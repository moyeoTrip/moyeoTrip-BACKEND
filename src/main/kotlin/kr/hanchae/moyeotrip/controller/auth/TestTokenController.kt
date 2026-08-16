package kr.hanchae.moyeotrip.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "테스트 인증", description = "로컬·개발 환경에서만 사용하는 임시 JWT 발급 API")
@Profile("default", "local", "dev")
@RestController
@RequestMapping("/api/v1/auth/test-token")
class TestTokenController(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
) {
    @Operation(summary = "테스트 access token 발급", description = "DB에 존재하는 사용자 ID로 서비스 JWT를 발급합니다.")
    @PostMapping("/{userId}")
    fun issueAccessToken(
        @PathVariable userId: Long,
    ): TestAccessTokenResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        val nickname = user.information?.nickname ?: "사용자 ${user.id}"
        return TestAccessTokenResponse(
            userId = user.id,
            nickname = nickname,
            accessToken = jwtUtil.generateAccessToken(user.id, nickname),
        )
    }
}
