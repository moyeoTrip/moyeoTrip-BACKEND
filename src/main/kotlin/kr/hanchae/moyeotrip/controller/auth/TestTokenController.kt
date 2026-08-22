package kr.hanchae.moyeotrip.controller.auth

import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("default", "local", "dev")
@RestController
@RequestMapping("/api/v1/auth/test-token")
class TestTokenController(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
) : TestTokenAPISpec {
    @PostMapping("/{userId}")
    override fun issueAccessToken(
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
