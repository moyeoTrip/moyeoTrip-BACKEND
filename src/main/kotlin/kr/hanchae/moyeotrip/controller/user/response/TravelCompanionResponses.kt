package kr.hanchae.moyeotrip.controller.user.response

import java.time.LocalDate

data class TripCompanionResponse(
    val companionRecordId: Long,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val mannerRating: Double?,
    val mannerScore: Int?,
    val oneLineReview: String?,
    val reviewed: Boolean,
)

data class TravelDexResponse(
    val totalCount: Int,
    val companions: List<TravelDexCompanionResponse>,
)

data class TravelDexCompanionResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val mannerRating: Double?,
    val tripCount: Int,
    val latestTripDate: LocalDate,
    val latestTripTitle: String,
    val memories: List<TravelDexMemoryResponse>,
)

data class TravelDexMemoryResponse(
    val chatRoomId: Long,
    val tripTitle: String,
    val tripDate: LocalDate,
    val oneLineReview: String?,
)
