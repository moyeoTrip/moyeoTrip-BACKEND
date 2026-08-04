package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.config.properties.WebCorsProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.cors.DefaultCorsProcessor

class WebCorsConfigTest {
    private val allowedOrigin = "https://moyeotrip.github.io"
    private val source = WebCorsConfig(WebCorsProperties(listOf(allowedOrigin), 7200)).corsConfigurationSource()

    @Test
    fun `허용된 GitHub Pages origin의 preflight 요청에 CORS 헤더를 응답한다`() {
        val request = preflightRequest(allowedOrigin)
        val response = MockHttpServletResponse()

        val accepted = DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response)

        assertTrue(accepted)
        assertEquals(allowedOrigin, response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        assertEquals("7200", response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))
    }

    @Test
    fun `허용되지 않은 origin의 preflight 요청은 거부한다`() {
        val request = preflightRequest("https://attacker.example")
        val response = MockHttpServletResponse()

        val accepted = DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response)

        assertFalse(accepted)
        assertEquals(403, response.status)
    }

    @Test
    fun `기본 CORS origin은 와일드카드 없이 로컬 Web과 GitHub Pages만 포함한다`() {
        assertTrue(WebCorsProperties.DEFAULT_ALLOWED_ORIGINS.contains("http://localhost:4173"))
        assertTrue(WebCorsProperties.DEFAULT_ALLOWED_ORIGINS.contains(allowedOrigin))
        assertFalse(WebCorsProperties.DEFAULT_ALLOWED_ORIGINS.contains("*"))
    }

    private fun preflightRequest(origin: String): MockHttpServletRequest =
        MockHttpServletRequest("OPTIONS", "/api/v1/auth/login").apply {
            addHeader(HttpHeaders.ORIGIN, origin)
            addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type")
        }
}
