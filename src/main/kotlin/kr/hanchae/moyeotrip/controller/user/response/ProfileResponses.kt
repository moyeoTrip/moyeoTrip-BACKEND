package kr.hanchae.moyeotrip.controller.user.response

import kr.hanchae.moyeotrip.entity.user.Gender
import java.time.LocalDate

data class MyProfileResponse(
    val nickname: String,
    val profileImageUrl: String?,
    val introduction: String?,
    val travelStyles: List<TravelStyleResponse>,
    val interestedRegions: List<InterestedRegionResponse>,
    val birthDate: LocalDate?,
    val gender: Gender,
)

data class ProfileOptionsResponse(
    val travelStyles: List<TravelStyleResponse>,
    val interestedRegions: List<InterestedRegionResponse>,
)

data class TravelStyleResponse(
    val id: Long,
    val label: String,
)

data class InterestedRegionResponse(
    val id: Long,
    val signguName: String,
)
