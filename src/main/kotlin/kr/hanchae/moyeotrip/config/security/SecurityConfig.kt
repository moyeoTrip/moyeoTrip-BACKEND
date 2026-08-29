package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val jwtUtil: JwtUtil,
    private val customUserDetailService: CustomUserDetailService,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val jwtFilter = JwtFilter(jwtUtil, customUserDetailService, objectMapper)
        return http
            .cors { }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            ).authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/providers", "/api/v1/auth/providers/**")
                    .authenticated()
                    .requestMatchers(*PERMITTED_URL_PATTERNS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }.build()
    }
}

val PERMITTED_URL_PATTERNS =
    arrayOf(
        "/actuator/health",
        "/actuator/health/**",
        "/swagger-ui/**",
        "/api-docs/**",
        "/api/v1/auth/**",
        "/api/v1/test/**",
        "/api/v1/terms/**",
        "/ws/**",
    )
