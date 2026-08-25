package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import java.time.LocalDate

@Schema(description = "로그인 사용자의 프로필 정보")
data class MyProfileResponse(
    @field:Schema(description = "사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "현재 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "자기소개", nullable = true)
    val introduction: String?,
    @field:Schema(description = "선택한 여행 스타일 목록")
    val travelStyles: List<TravelStyleResponse>,
    @field:Schema(description = "선택한 관심 경북 지역 목록")
    val interestedRegions: List<InterestedRegionResponse>,
    @field:Schema(description = "생년월일. 미입력 사용자면 null", nullable = true, type = "string", format = "date")
    val birthDate: LocalDate?,
    @field:Schema(description = "성별", example = "F")
    val gender: Gender,
    @field:Schema(description = "채팅 메시지 알림 수신 방식", example = "ALL")
    val chatNotificationMode: ChatNotificationMode,
    @field:Schema(description = "모집 마감 임박 알림 수신 여부", example = "true")
    val recruitmentDeadlineEnabled: Boolean,
    @field:Schema(description = "친구 요청·피드 반응 등 소셜 활동 알림 수신 여부", example = "true")
    val socialActivityEnabled: Boolean,
    @field:Schema(description = "이벤트·추천 등 마케팅 알림 수신 여부", example = "false")
    val marketingEnabled: Boolean,
)

@Schema(description = "다른 사용자에게 공개하는 프로필 정보")
data class PublicProfileResponse(
    @field:Schema(description = "사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "닉네임 표시 색상", example = "MINT")
    val nicknameColor: NicknameColor,
    @field:Schema(description = "현재 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "자기소개", nullable = true)
    val introduction: String?,
    @field:Schema(description = "선택한 여행 스타일 목록")
    val travelStyles: List<TravelStyleResponse>,
    @field:Schema(description = "선택한 관심 경북 지역 목록")
    val interestedRegions: List<InterestedRegionResponse>,
    @field:Schema(description = "함께 여행한 사용자 평가 평균. 평가가 없으면 null", nullable = true, example = "4.7")
    val mannerRating: Double?,
)

@Schema(description = "프로필 수정 화면의 선택 항목 목록")
data class ProfileOptionsResponse(
    @field:Schema(description = "선택 가능한 여행 스타일 목록")
    val travelStyles: List<TravelStyleResponse>,
    @field:Schema(description = "선택 가능한 관심 경북 지역 목록")
    val interestedRegions: List<InterestedRegionResponse>,
)

@Schema(description = "여행 스타일 선택 항목")
data class TravelStyleResponse(
    @field:Schema(description = "여행 스타일 ID", example = "1")
    val id: Long,
    @field:Schema(description = "화면에 표시할 여행 스타일명", example = "자연")
    val label: String,
)

@Schema(description = "관심 경북 지역 선택 항목")
data class InterestedRegionResponse(
    @field:Schema(description = "관심 지역 ID", example = "1")
    val id: Long,
    @field:Schema(description = "법정동 코드의 시군구명", example = "포항시 남구")
    val signguName: String,
)
