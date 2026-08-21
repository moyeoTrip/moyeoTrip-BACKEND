package kr.hanchae.moyeotrip.controller.user.request

import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.user.Gender
import java.time.LocalDate

data class UpdateProfileRequest(
    @field:Size(max = 300)
    val introduction: String? = null,
    val travelStyleIds: Set<Long> = emptySet(),
    val interestedRegionIds: Set<Long> = emptySet(),
    @field:Past
    val birthDate: LocalDate,
    val gender: Gender,
)
