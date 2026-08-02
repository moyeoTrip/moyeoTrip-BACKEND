package kr.hanchae.moyeotrip.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthAPISpecTest {
    @Test
    fun `인증 컨트롤러는 Swagger 스펙 인터페이스를 구현한다`() {
        assertTrue(AuthAPISpec::class.java.isAssignableFrom(AuthController::class.java))
    }

    @Test
    fun `모든 인증 API에 operation과 응답 문서가 존재한다`() {
        val methods = AuthAPISpec::class.java.declaredMethods

        assertEquals(16, methods.size)
        methods.forEach { method ->
            assertTrue(method.isAnnotationPresent(Operation::class.java), "${method.name}에 @Operation이 없습니다.")
            assertTrue(method.isAnnotationPresent(ApiResponses::class.java), "${method.name}에 @ApiResponses가 없습니다.")
        }
    }

    @Test
    fun `인증 스펙에 태그 설명이 존재한다`() {
        val tag = AuthAPISpec::class.java.getAnnotation(Tag::class.java)

        assertTrue(tag.name.isNotBlank())
        assertTrue(tag.description.isNotBlank())
    }
}
