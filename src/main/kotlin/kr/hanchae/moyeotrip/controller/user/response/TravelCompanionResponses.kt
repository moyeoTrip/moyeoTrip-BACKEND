package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import java.time.LocalDate

@Schema(description = "완료한 여행의 동행자 및 내 평가 정보")
data class TripCompanionResponse(
    @field:Schema(description = "동행자 기록 ID", example = "44")
    val companionRecordId: Long,
    @field:Schema(description = "동행자 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "동행자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "동행자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "다른 사용자가 남긴 평균 매너 점수. 평가가 없으면 null", example = "4.8", nullable = true)
    val mannerRating: Double?,
    @field:Schema(description = "로그인 사용자가 이 동행자에게 남긴 매너 점수. 미평가면 null", example = "5", nullable = true)
    val mannerScore: Int?,
    @field:Schema(description = "로그인 사용자가 이 동행자에게 남긴 한줄평. 미평가면 null", nullable = true)
    val oneLineReview: String?,
    @field:Schema(description = "로그인 사용자의 평가 완료 여부", example = "true")
    val reviewed: Boolean,
)

@Schema(description = "내 여행 도감 조회 응답")
data class TravelDexResponse(
    @field:Schema(description = "도감에 기록된 고유 동행자 수", example = "10")
    val totalCount: Int,
    @field:Schema(description = "동행자별 여행 도감 목록")
    val companions: List<TravelDexCompanionResponse>,
)

@Schema(description = "여행 도감에 기록된 동행자 정보")
data class TravelDexCompanionResponse(
    @field:Schema(description = "동행자 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "동행자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "동행자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "동행자의 평균 매너 점수. 평가가 없으면 null", example = "4.8", nullable = true)
    val mannerRating: Double?,
    @field:Schema(description = "함께 완료한 여행 횟수", example = "3")
    val tripCount: Int,
    @field:Schema(description = "가장 최근에 함께 여행한 날짜", example = "2026-09-12", type = "string", format = "date")
    val latestTripDate: LocalDate,
    @field:Schema(description = "가장 최근에 함께 여행한 채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val latestTripTitle: String,
    @field:Schema(description = "함께한 여행 기록 목록")
    val memories: List<TravelDexMemoryResponse>,
)

@Schema(description = "여행 도감의 동행 여행 기록")
data class TravelDexMemoryResponse(
    @field:Schema(description = "함께 여행한 채팅방 ID", example = "101")
    val chatRoomId: Long,
    @field:Schema(description = "함께 여행한 채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val tripTitle: String,
    @field:Schema(description = "여행 날짜", example = "2026-09-12", type = "string", format = "date")
    val tripDate: LocalDate,
    @field:Schema(description = "로그인 사용자가 남긴 동행자 한줄평. 미평가면 null", nullable = true)
    val oneLineReview: String?,
)

@Schema(description = "다른 사용자에게 남겨진 여행 동행 한줄평")
data class ReceivedTravelReviewResponse(
    @field:Schema(description = "한줄평을 남긴 사용자 ID", example = "15")
    val reviewerId: Long,
    @field:Schema(description = "한줄평을 남긴 사용자 닉네임", example = "따스한 사슴 3492")
    val reviewerNickname: String,
    @field:Schema(description = "한줄평을 남긴 사용자 닉네임 표시 색상", example = "MINT")
    val reviewerNicknameColor: NicknameColor,
    @field:Schema(description = "한줄평을 남긴 사용자 프로필 이미지 URL", nullable = true)
    val reviewerProfileImageUrl: String?,
    @field:Schema(description = "여행 동행 한줄평", example = "약속 시간을 잘 지키는 좋은 동행자예요.")
    val content: String,
)
