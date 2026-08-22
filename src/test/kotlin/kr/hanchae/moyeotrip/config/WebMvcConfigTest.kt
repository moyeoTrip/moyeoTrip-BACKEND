package kr.hanchae.moyeotrip.config

import kr.hanchae.moyeotrip.utils.LoginUserResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.method.support.HandlerMethodArgumentResolver

class WebMvcConfigTest {
    @Test
    fun `LoginUserId 리졸버를 MVC 인자 리졸버에 등록한다`() {
        val loginUserResolver = LoginUserResolver()
        val resolvers = mutableListOf<HandlerMethodArgumentResolver>()

        WebMvcConfig(loginUserResolver).addArgumentResolvers(resolvers)

        assertEquals(listOf(loginUserResolver), resolvers)
    }
}
