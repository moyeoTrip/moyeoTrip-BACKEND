package kr.hanchae.moyeotrip.controller.user.response

import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.TravelStyle
import java.time.LocalDate

data class MyProfileResponse(
    val nickname: String,
    val profileImageUrl: String?,
    val introduction: String?,
    val travelStyles: Set<TravelStyle>,
    val interestedRegions: List<InterestedRegionResponse>,
    val birthDate: LocalDate?,
    val gender: Gender,
)

data class ProfileOptionsResponse(
    val travelStyles: List<TravelStyle>,
    val interestedRegions: List<InterestedRegionResponse>,
)

data class InterestedRegionResponse(
    val signguCode: String,
    val signguName: String,
)
