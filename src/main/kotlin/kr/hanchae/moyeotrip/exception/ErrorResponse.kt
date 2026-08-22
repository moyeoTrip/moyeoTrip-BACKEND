package kr.hanchae.moyeotrip.exception

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "API 오류 응답")
open class ErrorResponse private constructor(
    @field:Schema(
        description = "애플리케이션 오류 코드. HTTP 상태 코드와 별도로 세부 원인을 식별합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val code: Int,
    @field:Schema(
        description = "사용자 또는 개발자가 확인할 수 있는 오류 설명",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val errorMessage: String,
) {
    companion object {
        fun of(
            errorCode: ErrorCode,
            errorMessage: String?,
        ) = ErrorResponse(errorCode.code, errorMessage ?: errorCode.errorMessage)
    }
}
