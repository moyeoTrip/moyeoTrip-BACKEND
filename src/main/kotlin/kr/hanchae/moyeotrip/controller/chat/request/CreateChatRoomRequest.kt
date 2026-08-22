package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.chat.GenderRestriction
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val CREATE_OVERNIGHT_CUSTOM_CHAT_ROOM_REQUEST_EXAMPLE =
    """{"title":"주왕산 & 주산지 힐링 트레킹","description":"가을 단풍을 함께 즐길 동행자를 구해요.","maxParticipants":5,"tripType":"OVERNIGHT","startDate":"2026-09-12","endDate":"2026-09-13","recruitmentDeadlineDate":"2026-09-09","meetingLatitude":36.576,"meetingLongitude":128.97,"meetingDetails":"안동역 1번 출구 앞","meetingDateTime":"2026-09-12T08:30:00","participationFee":15000,"genderRestriction":"NONE","minimumAge":20,"maximumAge":39,"joinApprovalMode":"MANUAL","courseType":"CUSTOM","customCourse":{"title":"주왕산 단풍길 코스","description":"천천히 걷는 단풍 트레킹 코스","places":[{"contentId":126508,"dayNumber":1,"sequence":1,"visitTime":"09:00"},{"contentId":126508,"dayNumber":1,"sequence":2,"visitTime":"14:00"},{"contentId":126508,"dayNumber":2,"sequence":1,"visitTime":"09:00"},{"contentId":126508,"dayNumber":2,"sequence":2,"visitTime":"14:00"}],"tagIds":[1,4]}}"""

@Schema(
    description = "여행 채팅방 생성 요청. 썸네일은 multipart thumbnail 파트로 선택해 전송합니다.",
    example = CREATE_OVERNIGHT_CUSTOM_CHAT_ROOM_REQUEST_EXAMPLE,
)
data class CreateChatRoomRequest(
    @field:Schema(description = "채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Schema(description = "모임 소개", example = "가을 단풍을 함께 즐길 동행자를 구해요.", nullable = true)
    @field:Size(max = 500)
    val description: String? = null,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원", example = "5", minimum = "3", maximum = "12")
    @field:Min(3)
    @field:Max(12)
    val maxParticipants: Int,
    @field:Schema(description = "당일 또는 숙박 여행 유형", example = "OVERNIGHT")
    val tripType: TripType,
    @field:Schema(description = "여행 시작일", example = "2026-09-12", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일 여행이면 생략합니다.", example = "2026-09-13", nullable = true, type = "string", format = "date")
    val endDate: LocalDate? = null,
    @field:Schema(description = "참가 신청 마감일", example = "2026-09-09", type = "string", format = "date")
    val recruitmentDeadlineDate: LocalDate,
    @field:Schema(description = "당일 여행 시작 시각. 당일 여행일 때 사용합니다.", example = "09:00", nullable = true)
    val dayTripStartTime: LocalTime? = null,
    @field:Schema(description = "당일 여행 종료 시각. 당일 여행일 때 사용합니다.", example = "18:00", nullable = true)
    val dayTripEndTime: LocalTime? = null,
    @field:Schema(description = "집합 장소 위도", example = "36.5760", nullable = true)
    @field:Min(-90)
    @field:Max(90)
    val meetingLatitude: Double? = null,
    @field:Schema(description = "집합 장소 경도", example = "128.9700", nullable = true)
    @field:Min(-180)
    @field:Max(180)
    val meetingLongitude: Double? = null,
    @field:Schema(description = "집합 장소 이름 또는 상세 안내", example = "안동역 1번 출구 앞", nullable = true)
    @field:Size(max = 500)
    val meetingDetails: String? = null,
    @field:Schema(description = "집합 일시", example = "2026-09-12T08:30:00")
    val meetingDateTime: LocalDateTime,
    @field:Schema(description = "참가비. 무료이면 0 또는 null", example = "15000", nullable = true)
    @field:Min(0)
    val participationFee: Long? = null,
    @field:Schema(description = "참가 성별 제한", example = "NONE")
    val genderRestriction: GenderRestriction,
    @field:Schema(description = "참가 최소 만 나이", example = "20", nullable = true)
    @field:Min(20)
    @field:Max(100)
    val minimumAge: Int? = null,
    @field:Schema(description = "참가 최대 만 나이", example = "39", nullable = true)
    @field:Min(20)
    @field:Max(100)
    val maximumAge: Int? = null,
    @field:Schema(description = "참가 신청 승인 방식. AUTO는 자동 승인, MANUAL은 호스트 승인입니다.", example = "MANUAL")
    val joinApprovalMode: JoinApprovalMode,
    @field:Schema(description = "기존 공개 코스 또는 새 커스텀 코스 유형", example = "CUSTOM")
    val courseType: TravelCourseType,
    @field:Schema(description = "기존 공개 코스 ID. courseType이 PUBLIC일 때 지정합니다.", example = "77", nullable = true)
    val courseId: Long? = null,
    @field:Schema(description = "새 커스텀 코스 정보. courseType이 CUSTOM일 때 지정합니다.", nullable = true)
    @field:Valid
    val customCourse: CreateCustomCourseRequest? = null,
)

@Schema(description = "채팅방에 연결할 새 커스텀 여행 코스 정보")
data class CreateCustomCourseRequest(
    @field:Schema(description = "커스텀 코스 제목", example = "주왕산 단풍길 코스")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Schema(description = "커스텀 코스 소개", example = "천천히 걷는 단풍 트레킹 코스", nullable = true)
    @field:Size(max = 500)
    val description: String? = null,
    @field:Schema(description = "방문 장소 목록. 최소 2개를 입력합니다.")
    @field:Valid
    @field:Size(min = 2)
    val places: List<CustomCoursePlaceRequest>,
    @field:Schema(description = "커스텀 코스에 연결할 여행 코스 태그 ID 목록", example = "[1, 4]")
    val tagIds: Set<Long> = emptySet(),
)

@Schema(description = "커스텀 여행 코스의 방문 장소 요청")
data class CustomCoursePlaceRequest(
    @field:Schema(description = "방문할 TourismContent ID", example = "126508", minimum = "1")
    @field:Min(1)
    val contentId: Long,
    @field:Schema(description = "여행 일차. 1부터 시작합니다.", example = "1", minimum = "1")
    @field:Min(1)
    val dayNumber: Int,
    @field:Schema(description = "같은 일차 안의 방문 순서. 1부터 시작합니다.", example = "2", minimum = "1")
    @field:Min(1)
    val sequence: Int,
    @field:Schema(description = "방문 예정 시각", example = "14:00")
    val visitTime: LocalTime,
)
