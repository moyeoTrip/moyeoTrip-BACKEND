package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val jwtUtil: JwtUtil,
    private val customUserDetailService: CustomUserDetailService,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(
                JwtFilter(jwtUtil, customUserDetailService),
                UsernamePasswordAuthenticationFilter::class.java,
            ).authorizeHttpRequests {
                it
                    .requestMatchers(*PERMITTED_URL_PATTERNS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
            }.build()
}

val PERMITTED_URL_PATTERNS =
    arrayOf(
        "/health",
        "/ready",
        "/swagger-ui/**",
        "/api-docs/**",
        "/api/v1/auth/temporal/**",
        "/api/v1/auth/login/kakao/**",
        "/api/v1/auth/user/kakao",
        "/api/v1/auth/check-nickname",
        "/api/v1/auth/refresh",
        "/api/v1/auth/terms/**",
    )
