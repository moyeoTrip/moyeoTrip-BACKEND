package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.ProviderType

data class LinkedProvidersResponse(
    @field:ArraySchema(
        arraySchema = Schema(description = "현재 사용자에게 연결된 로그인 제공자 목록", example = "[\"EMAIL\",\"APPLE\",\"KAKAO\"]"),
        schema = Schema(allowableValues = ["EMAIL", "KAKAO", "APPLE"]),
        uniqueItems = true,
    )
    val providers: Set<ProviderType>,
)
