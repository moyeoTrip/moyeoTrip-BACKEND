package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.ErrorResponse
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import kr.hanchae.moyeotrip.utils.jwt.isBearerToken
import kr.hanchae.moyeotrip.utils.jwt.removeBearer
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtFilter(
    private val jwtUtil: JwtUtil,
    private val customUserDetailService: CustomUserDetailService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val bearerToken = request.getHeader(AUTHORIZATION)
        if (bearerToken.isNullOrBlank() || !bearerToken.isBearerToken()) {
            filterChain.doFilter(request, response)
            return
        }

        val token = bearerToken.removeBearer()
        if (jwtUtil.validateToken(jwtUtil.accessKey, token)) {
            val securityUser =
                runCatching {
                    val userId = jwtUtil.getUserId(jwtUtil.accessKey, token)
                    customUserDetailService.loadUserById(userId) as CustomUserDto
                }.getOrNull()
            if (securityUser != null) {
                val authentication = UsernamePasswordAuthenticationToken(securityUser, null, securityUser.authorities)
                SecurityContextHolder.getContext().authentication = authentication

                val profileError = profileError(request, securityUser)
                if (profileError != null) {
                    writeError(response, profileError)
                    return
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun profileError(
        request: HttpServletRequest,
        user: CustomUserDto,
    ): ErrorCode? {
        if (isProfileImageEndpoint(request)) {
            return when {
                user.signupState == SignupState.USER_INFO_REQUIRED -> ErrorCode.USER_INFO_REQUIRED
                user.signupState == SignupState.SIGNUP_COMPLETE || user.hasProfileImage ->
                    ErrorCode.PROFILE_IMAGE_ALREADY_SELECTED
                else -> null
            }
        }
        if (isProfileCheckExcluded(request)) return null
        return when {
            user.signupState == SignupState.USER_INFO_REQUIRED -> ErrorCode.USER_INFO_REQUIRED
            user.signupState != SignupState.SIGNUP_COMPLETE || !user.hasProfileImage -> ErrorCode.PROFILE_IMAGE_REQUIRED
            else -> null
        }
    }

    private fun isProfileCheckExcluded(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        if (request.method == HttpMethod.OPTIONS.name()) return true
        if (PROFILE_CHECK_EXCLUDED_PATHS.contains(path)) return true
        if (PROFILE_CHECK_EXCLUDED_PREFIXES.any { path == it || path.startsWith("$it/") }) return true
        return false
    }

    private fun isProfileImageEndpoint(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return ProfileImageEndpoint.entries.any { it.matches(request.method, path) }
    }

    private fun writeError(
        response: HttpServletResponse,
        errorCode: ErrorCode,
    ) {
        response.status = errorCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, ErrorResponse.of(errorCode, errorCode.errorMessage))
    }

    private enum class ProfileImageEndpoint(
        private val method: HttpMethod,
        private val path: String,
    ) {
        GENERATE(HttpMethod.POST, "/api/v1/users/me/profile-images"),
        LIST(HttpMethod.GET, "/api/v1/users/me/profile-images"),
        SELECT(HttpMethod.PUT, "/api/v1/users/me/profile-image"),
        ;

        fun matches(
            requestMethod: String,
            requestPath: String,
        ): Boolean = method.name() == requestMethod && path == requestPath
    }

    companion object {
        private val PROFILE_CHECK_EXCLUDED_PATHS =
            setOf(
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
                "/api/v1/auth/refresh",
                "/api/v1/auth/nickname-candidates",
                "/api/v1/auth/firebase/kakao/custom-token",
                "/api/v1/auth/firebase/kakao/authorization-code/custom-token",
            )
        private val PROFILE_CHECK_EXCLUDED_PREFIXES =
            listOf(
                "/actuator/health",
                "/swagger-ui",
                "/api-docs",
                "/api/v1/test",
                "/api/v1/terms",
            )
    }
}
