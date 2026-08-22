package kr.hanchae.moyeotrip.controller.user.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.user.Gender
import java.time.LocalDate

@Schema(description = "내 프로필 수정 요청")
data class UpdateProfileRequest(
    @field:Schema(description = "자기소개", example = "느긋한 여행을 좋아해요.", nullable = true)
    @field:Size(max = 300)
    val introduction: String? = null,
    @field:Schema(description = "선택한 여행 스타일 ID 목록", example = "[1, 3]")
    val travelStyleIds: Set<Long> = emptySet(),
    @field:Schema(description = "선택한 관심 경북 지역 ID 목록", example = "[47111, 47170]")
    val interestedRegionIds: Set<Long> = emptySet(),
    @field:Schema(description = "생년월일", example = "1998-04-12", type = "string", format = "date")
    @field:Past
    val birthDate: LocalDate,
    @field:Schema(description = "성별", example = "F")
    val gender: Gender,
)
