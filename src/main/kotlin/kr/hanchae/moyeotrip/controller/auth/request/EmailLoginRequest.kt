package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailLoginRequest(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Email
    @field:NotBlank(message = "이메일은 필수 입력값입니다.")
    val email: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    val password: String,
)
