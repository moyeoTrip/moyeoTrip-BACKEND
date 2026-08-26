package kr.hanchae.moyeotrip.controller.chat.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.GenderRestriction
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "채팅방 생성 결과")
data class CreateChatRoomResponse(
    @field:Schema(description = "생성된 채팅방 ID", example = "101")
    val roomId: Long,
)

@Schema(description = "채팅방 공지 생성 결과")
data class CreateChatRoomNoticeResponse(
    @field:Schema(description = "생성된 공지 ID", example = "44")
    val noticeId: Long,
)

@Schema(description = "채팅방 상세 정보")
data class ChatRoomDetailResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val title: String,
    @field:Schema(description = "채팅방 소개", example = "가을 단풍을 함께 즐길 동행자를 구해요.", nullable = true)
    val description: String?,
    @field:Schema(description = "채팅방 썸네일 이미지 URL", example = "https://cdn.example.com/chat-rooms/101/thumbnail.jpg", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "당일 또는 숙박 여행 유형", example = "OVERNIGHT")
    val tripType: TripType,
    @field:Schema(description = "여행 시작일", example = "2026-09-12", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일 여행이면 null", example = "2026-09-13", nullable = true, type = "string", format = "date")
    val endDate: LocalDate?,
    @field:Schema(description = "참가 신청 마감일", example = "2026-09-09", type = "string", format = "date")
    val recruitmentDeadlineDate: LocalDate,
    @field:Schema(description = "숙박 일수", example = "1")
    val tripNights: Int,
    @field:Schema(description = "여행 일수", example = "2")
    val tripDays: Int,
    @field:Schema(description = "당일 여행 시작 시각. 숙박 여행이면 null", example = "09:00", nullable = true)
    val dayTripStartTime: LocalTime?,
    @field:Schema(description = "당일 여행 종료 시각. 숙박 여행이면 null", example = "18:00", nullable = true)
    val dayTripEndTime: LocalTime?,
    @field:Schema(description = "집합 장소 위도", example = "36.5760", nullable = true)
    val meetingLatitude: Double?,
    @field:Schema(description = "집합 장소 경도", example = "128.9700", nullable = true)
    val meetingLongitude: Double?,
    @field:Schema(description = "집합 장소 이름 또는 상세 안내", example = "안동역 1번 출구 앞", nullable = true)
    val meetingDetails: String?,
    @field:Schema(description = "집합 일시", example = "2026-09-12T08:30:00")
    val meetingDateTime: LocalDateTime,
    @field:Schema(description = "참가비. 무료 또는 미정이면 null", example = "15000", nullable = true)
    val participationFee: Long?,
    @field:Schema(description = "참가 성별 제한", example = "NONE")
    val genderRestriction: GenderRestriction,
    @field:Schema(description = "참가 최소 만 나이. 제한이 없으면 null", example = "20", nullable = true)
    val minimumAge: Int?,
    @field:Schema(description = "참가 최대 만 나이. 제한이 없으면 null", example = "39", nullable = true)
    val maximumAge: Int?,
    @field:Schema(description = "참가 신청 승인 방식. AUTO는 자동 승인, MANUAL은 호스트 승인입니다.", example = "MANUAL")
    val joinApprovalMode: JoinApprovalMode,
    @field:Schema(description = "모집 마감까지 남은 일수. 모집 마감일이 지나면 null", example = "3", nullable = true)
    val recruitmentDDay: Long?,
    @field:Schema(description = "연결된 여행 코스 제목", example = "주왕산 단풍길 코스")
    val courseTitle: String,
    @field:Schema(description = "연결된 여행 코스 태그 목록")
    val tags: List<TravelCourseTagResponse>,
    @field:Schema(description = "호스트 사용자 ID", example = "12")
    val hostId: Long,
    @field:Schema(description = "호스트 프로필 이미지 URL", example = "https://cdn.example.com/profiles/12.png", nullable = true)
    val hostProfileImageUrl: String?,
    @field:Schema(description = "현재 승인된 참가자 수", example = "4")
    val participantCount: Int,
    @field:Schema(description = "여행 확정에 필요한 호스트 포함 최소 출발 인원", example = "3")
    val minimumParticipants: Int,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원", example = "5")
    val maxParticipants: Int,
    @field:Schema(description = "채팅방 여행 상태", example = "RECRUITING")
    val status: ChatRoomStatus,
    @field:Schema(description = "로그인 사용자의 채팅방 찜 여부", example = "true")
    val favorite: Boolean,
    @field:Schema(description = "로그인 사용자가 현재 이 모임에 참가 신청할 수 있는지 여부", example = "true")
    val canApply: Boolean,
    @field:Schema(description = "가장 최근의 상단 고정 공지. 없으면 null", nullable = true)
    val latestPinnedNotice: ChatRoomNoticeResponse?,
    @field:Schema(description = "현재 승인된 참가자 목록")
    val participants: List<ChatParticipantResponse>,
)

@Schema(description = "채팅방에 연결된 여행 코스 요약 정보")
data class TravelCourseResponse(
    @field:Schema(description = "여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "여행 코스 제목", example = "주왕산 단풍길 코스")
    val title: String,
    @field:Schema(description = "커스텀 또는 공개 코스 유형", example = "CUSTOM")
    val type: TravelCourseType,
    @field:Schema(description = "로그인 사용자가 이 코스를 수정할 수 있는지 여부", example = "true")
    val editable: Boolean,
    @field:Schema(description = "방문 순서와 시간을 포함한 장소 목록")
    val places: List<TravelCoursePlaceResponse>,
)

@Schema(description = "채팅방 여행 코스 상세 응답")
data class TravelCourseDetailResponse(
    @field:Schema(description = "연결된 채팅방 여행 일정. 공개 코스 단독 조회면 null", nullable = true)
    val room: TravelCourseRoomResponse?,
    @field:Schema(description = "여행 코스 상세 정보")
    val course: TravelCourseInformationResponse,
)

@Schema(description = "여행 코스에 연결된 채팅방 여행 일정 정보")
data class TravelCourseRoomResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "당일 또는 숙박 여행 유형", example = "OVERNIGHT")
    val tripType: TripType,
    @field:Schema(description = "여행 시작일", example = "2026-09-12", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일 여행이면 null", nullable = true, type = "string", format = "date")
    val endDate: LocalDate?,
    @field:Schema(description = "당일 여행 시작 시각. 숙박 여행이면 null", nullable = true)
    val dayTripStartTime: LocalTime?,
    @field:Schema(description = "당일 여행 종료 시각. 숙박 여행이면 null", nullable = true)
    val dayTripEndTime: LocalTime?,
)

@Schema(description = "채팅방에서 사용하는 여행 코스 정보")
data class TravelCourseInformationResponse(
    @field:Schema(description = "여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "여행 코스 제목", example = "주왕산 단풍길 코스")
    val title: String,
    @field:Schema(description = "여행 코스 소개", nullable = true)
    val description: String?,
    @field:Schema(description = "커스텀 또는 공개 코스 유형", example = "PUBLIC")
    val type: TravelCourseType,
    @field:Schema(description = "예상 전체 여행 시간", example = "6시간 30분")
    val travelTime: String,
    @field:Schema(description = "전체 이동 거리(km)", example = "12.4")
    val distanceKm: Double,
    @field:Schema(description = "완료 여행 사용자 평점 평균. 평가가 없으면 null", example = "4.5", nullable = true)
    val averageRating: Double?,
    @field:Schema(description = "평가 건수", example = "12")
    val ratingCount: Long,
    @field:Schema(description = "연결된 여행 코스 태그 목록")
    val tags: List<TravelCourseTagResponse>,
    @field:Schema(description = "여행 코스 대표 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "방문 순서와 시간을 포함한 장소 목록")
    val places: List<TravelCoursePlaceResponse>,
)

@Schema(description = "공개 여행 코스 상세 정보")
data class PublicTravelCourseDetailResponse(
    @field:Schema(description = "공개 여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "공개 여행 코스 제목", example = "주왕산 단풍길 코스")
    val title: String,
    @field:Schema(description = "공개 여행 코스 소개", nullable = true)
    val description: String?,
    @field:Schema(description = "작성자 표시를 허용한 경우의 닉네임. 비공개면 null", nullable = true)
    val creatorNickname: String?,
    @field:Schema(description = "작성자가 이 코스를 여행한 시작일. 공개 설정에 따라 null", nullable = true, type = "string", format = "date")
    val creatorTravelStartDate: LocalDate?,
    @field:Schema(description = "작성자가 이 코스를 여행한 종료일. 당일 여행 또는 비공개면 null", nullable = true, type = "string", format = "date")
    val creatorTravelEndDate: LocalDate?,
    @field:Schema(description = "이 코스로 생성된 채팅방 수", example = "8")
    val chatRoomCount: Long,
    @field:Schema(description = "예상 전체 여행 시간", example = "6시간 30분")
    val travelTime: String,
    @field:Schema(description = "전체 이동 거리(km)", example = "12.4")
    val distanceKm: Double,
    @field:Schema(description = "완료 여행 사용자 평점 평균. 평가가 없으면 null", nullable = true)
    val averageRating: Double?,
    @field:Schema(description = "평가 건수", example = "12")
    val ratingCount: Long,
    @field:Schema(description = "연결된 여행 코스 태그 목록")
    val tags: List<TravelCourseTagResponse>,
    @field:Schema(description = "여행 코스 대표 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "방문 순서와 시간을 포함한 장소 목록")
    val places: List<TravelCoursePlaceResponse>,
)

@Schema(description = "여행 코스 방문 장소와 일정 정보")
data class TravelCoursePlaceResponse(
    @field:Schema(description = "TourismContent ID", example = "126508")
    val contentId: Long,
    @field:Schema(description = "여행 일차. 1부터 시작합니다.", example = "1")
    val dayNumber: Int,
    @field:Schema(description = "같은 일차 안의 방문 순서. 1부터 시작합니다.", example = "2")
    val sequence: Int,
    @field:Schema(description = "방문 예정 시각. 시간 미지정이면 null", example = "14:00", nullable = true)
    val visitTime: LocalTime?,
    @field:Schema(description = "관광 장소명", example = "주산지")
    val title: String,
    @field:Schema(description = "관광 장소 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "관광 장소 위도", example = "36.3742")
    val latitude: Double,
    @field:Schema(description = "관광 장소 경도", example = "129.1641")
    val longitude: Double,
)

@Schema(description = "채팅방 참가자 요약 정보")
data class ChatParticipantResponse(
    @field:Schema(description = "참가자 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "참가자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
)

@Schema(description = "채팅방 동행자 목록과 인원 정보")
data class ChatRoomMemberListResponse(
    @field:Schema(description = "현재 승인된 참가자 수", example = "4")
    val participantCount: Int,
    @field:Schema(description = "여행 확정에 필요한 호스트 포함 최소 출발 인원", example = "3")
    val minimumParticipants: Int,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원", example = "5")
    val maxParticipants: Int,
    @field:Schema(description = "승인 후 대기열에 있는 사용자 수", example = "2")
    val waitlistCount: Int,
    @field:Schema(description = "현재 참가자와 승인 대기열 사용자 목록")
    val members: List<ChatRoomMemberResponse>,
)

@Schema(description = "채팅방 동행자 상세 정보")
data class ChatRoomMemberResponse(
    @field:Schema(description = "사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "사용자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "완료한 여행 횟수", example = "3")
    val completedTripCount: Int,
    @field:Schema(description = "채팅방 호스트 여부", example = "false")
    val host: Boolean,
    @field:Schema(description = "로그인 사용자 본인 여부", example = "true")
    val me: Boolean,
)

@Schema(description = "로그인 사용자의 채팅방 강퇴 이력")
data class ChatRoomKickHistoryResponse(
    @field:Schema(description = "강퇴 이력 ID", example = "44")
    val kickHistoryId: Long,
    @field:Schema(description = "강퇴된 채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "강퇴된 채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val roomTitle: String,
    @field:Schema(description = "호스트가 기록한 강퇴 사유")
    val reason: String,
    @field:Schema(description = "강퇴 처리 일시", example = "2026-09-10T12:00:00")
    val kickedAt: LocalDateTime,
)

@Schema(description = "채팅방 참가 신청 처리 결과")
data class JoinChatRoomResponse(
    @field:Schema(description = "참가 신청한 채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "즉시 참가, 대기열 이동 또는 호스트 승인 대기 결과", example = "PENDING_APPROVAL")
    val result: JoinResult,
    @field:Schema(description = "생성된 참가 신청 ID. 즉시 참가한 경우 null", example = "30", nullable = true)
    val applicationId: Long? = null,
    @field:Schema(description = "생성된 참가 신청 상태. 즉시 참가한 경우 null", example = "PENDING", nullable = true)
    val applicationStatus: JoinApplicationStatus? = null,
)

@Schema(
    description = "참가 신청 처리 결과. JOINED=즉시 참가, WAITLISTED=승인됐지만 정원 초과로 대기열, PENDING_APPROVAL=호스트 승인 대기",
    allowableValues = ["JOINED", "WAITLISTED", "PENDING_APPROVAL"],
)
enum class JoinResult { JOINED, WAITLISTED, PENDING_APPROVAL }

@Schema(description = "호스트가 조회하는 참가 신청 정보")
data class JoinApplicationResponse(
    @field:Schema(description = "참가 신청 ID", example = "30")
    val applicationId: Long,
    @field:Schema(description = "신청자가 작성한 자기소개", nullable = true)
    val applicationMessage: String,
    @field:Schema(description = "신청자 프로필 정보")
    val applicant: ApplicantProfileResponse,
    @field:Schema(description = "참가 신청 일시", example = "2026-09-01T12:30:00")
    val appliedAt: LocalDateTime,
)

@Schema(description = "참가 신청자의 프로필 요약 정보")
data class ApplicantProfileResponse(
    @field:Schema(description = "신청자 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "신청자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "신청자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "신청자 성별. 미입력 사용자면 null", example = "F", nullable = true)
    val gender: Gender?,
    @field:Schema(description = "신청자의 만 나이. 생년월일 미입력 사용자면 null", example = "28", nullable = true)
    val age: Int?,
    @field:Schema(description = "다른 사용자가 남긴 평균 매너 점수. 평가가 없으면 null", example = "4.8", nullable = true)
    val mannerRating: Double?,
    @field:Schema(description = "신청자가 완료한 여행 횟수", example = "3")
    val completedTripCount: Int,
)

@Schema(description = "참가 신청 승인 처리 결과")
data class ApproveJoinApplicationResponse(
    @field:Schema(description = "승인한 참가 신청 ID", example = "30")
    val applicationId: Long,
    @field:Schema(description = "즉시 참가 또는 승인 대기열 이동 결과", example = "WAITLISTED")
    val result: ApprovalResult,
    @field:Schema(description = "승인 대기열 순번. 즉시 참가했으면 null", example = "2", nullable = true)
    val waitlistPosition: Int?,
)

@Schema(
    description = "호스트의 참가 신청 승인 결과. JOINED=정원 내 참가 완료, WAITLISTED=정원 초과로 승인 대기열 이동",
    allowableValues = ["JOINED", "WAITLISTED"],
)
enum class ApprovalResult { JOINED, WAITLISTED }

@Schema(description = "채팅방 나가기 또는 대기 취소 결과")
data class LeaveChatRoomResponse(
    @field:Schema(description = "나가기 또는 신청 취소한 채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "나가기, 호스트 퇴장 취소, 신청 취소 또는 대기 취소 결과", example = "LEFT")
    val result: LeaveResult,
    @field:Schema(description = "참가자 퇴장으로 대기열에서 자동 승격된 사용자 ID. 없으면 null", example = "33", nullable = true)
    val promotedUserId: Long?,
)

@Schema(
    description =
        "채팅방 나가기 처리 결과. LEFT=참가자 퇴장, HOST_LEFT_AND_ROOM_CANCELLED=호스트 퇴장으로 모임 취소, " +
            "APPLICATION_CANCELLED=승인 대기 신청 취소, WAITLIST_CANCELLED=대기열 취소",
    allowableValues = ["LEFT", "HOST_LEFT_AND_ROOM_CANCELLED", "APPLICATION_CANCELLED", "WAITLIST_CANCELLED"],
)
enum class LeaveResult { LEFT, HOST_LEFT_AND_ROOM_CANCELLED, APPLICATION_CANCELLED, WAITLIST_CANCELLED }

@Schema(description = "채팅 메시지 및 공유 카드 정보")
data class ChatMessageResponse(
    @field:Schema(description = "채팅 메시지 ID", example = "1024")
    val messageId: Long,
    @field:Schema(description = "일반·시스템·사진·장소·위치·투표·정산 메모 메시지 유형", example = "TEXT")
    val type: ChatMessageType,
    @field:Schema(description = "발신자 사용자 ID. 시스템 메시지면 null", example = "12", nullable = true)
    val senderId: Long?,
    @field:Schema(description = "발신자 닉네임 또는 시스템 메시지 표시명", example = "따스한 사슴 3492")
    val senderNickname: String,
    @field:Schema(description = "메시지 본문 또는 카드 설명", example = "주왕산 3폭포에 도착했어요!")
    val content: String,
    @field:Schema(description = "메시지 전송 일시", example = "2026-09-12T13:30:00")
    val createdAt: LocalDateTime,
    @field:Schema(description = "공유한 사진 URL. 사진 메시지가 아니면 null", nullable = true)
    val imageUrl: String? = null,
    @field:Schema(description = "공유한 관광 장소 카드. 장소 메시지가 아니면 null", nullable = true)
    val tourismContent: SharedTourismContentResponse? = null,
    @field:Schema(description = "공유한 집합 위치 카드. 위치 메시지가 아니면 null", nullable = true)
    val location: SharedLocationResponse? = null,
    @field:Schema(description = "투표 카드. 투표 메시지가 아니면 null", nullable = true)
    val poll: ChatPollResponse? = null,
    @field:Schema(description = "답글을 남긴 원본 메시지 요약. 답글이 아니면 null", nullable = true)
    val replyTo: RepliedChatMessageResponse? = null,
    @field:Schema(description = "메시지에서 멘션된 참가자 목록")
    val mentions: List<MentionedChatUserResponse> = emptyList(),
)

@Schema(description = "채팅 메시지에서 멘션된 사용자 정보")
data class MentionedChatUserResponse(
    @field:Schema(description = "멘션된 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "멘션된 사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
)

@Schema(description = "답글의 원본 메시지 요약 정보")
data class RepliedChatMessageResponse(
    @field:Schema(description = "원본 메시지 ID", example = "1010")
    val messageId: Long,
    @field:Schema(description = "원본 메시지 발신자 닉네임", example = "따스한 사슴 3492")
    val senderNickname: String,
    @field:Schema(description = "원본 메시지 본문")
    val content: String,
)

@Schema(description = "채팅으로 공유한 관광 장소 카드 정보")
data class SharedTourismContentResponse(
    @field:Schema(description = "공유한 TourismContent ID", example = "126508")
    val contentId: Long,
    @field:Schema(description = "관광 장소명", example = "주산지")
    val title: String,
    @field:Schema(description = "관광 장소 주소", nullable = true)
    val address: String?,
    @field:Schema(description = "관광 장소 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "관광 장소 위도. 좌표 미제공이면 null", nullable = true)
    val latitude: Double?,
    @field:Schema(description = "관광 장소 경도. 좌표 미제공이면 null", nullable = true)
    val longitude: Double?,
)

@Schema(description = "채팅으로 공유한 집합 위치 정보")
data class SharedLocationResponse(
    @field:Schema(description = "호스트가 등록한 집합 장소 위도", example = "36.5760")
    val latitude: Double,
    @field:Schema(description = "호스트가 등록한 집합 장소 경도", example = "128.9700")
    val longitude: Double,
    @field:Schema(description = "호스트가 등록한 집합 장소 이름 또는 상세 안내", example = "안동역 1번 출구 앞", nullable = true)
    val name: String?,
)

@Schema(description = "채팅 투표 카드 정보")
data class ChatPollResponse(
    @field:Schema(description = "투표 질문", example = "점심 메뉴는 무엇으로 할까요?")
    val question: String,
    @field:Schema(description = "익명 투표 여부", example = "true")
    val anonymous: Boolean,
    @field:Schema(description = "전체 투표 수", example = "4")
    val totalVoteCount: Int,
    @field:Schema(description = "투표 선택지 목록")
    val options: List<ChatPollOptionResponse>,
)

@Schema(description = "채팅 투표 선택지와 내 투표 정보")
data class ChatPollOptionResponse(
    @field:Schema(description = "투표 선택지 ID", example = "23")
    val optionId: Long,
    @field:Schema(description = "투표 선택지 내용", example = "한식")
    val text: String,
    @field:Schema(description = "이 선택지를 고른 참가자 수", example = "2")
    val voteCount: Int,
    @field:Schema(description = "로그인 사용자가 이 선택지를 골랐는지 여부", example = "false")
    val votedByMe: Boolean,
    @field:Schema(description = "실명 투표일 때만 표시되는 투표자 닉네임 목록. 익명 투표면 null", nullable = true)
    val voterNicknames: List<String>?,
)

@Schema(description = "실시간 채팅 투표 결과 갱신 이벤트")
data class ChatPollUpdatedResponse(
    @field:Schema(description = "투표 메시지 ID", example = "501")
    val messageId: Long,
    @field:Schema(description = "전체 투표 수", example = "4")
    val totalVoteCount: Int,
    @field:Schema(description = "선택지별 최신 투표 결과")
    val options: List<ChatPollUpdatedOptionResponse>,
)

@Schema(description = "실시간으로 갱신된 투표 선택지 결과")
data class ChatPollUpdatedOptionResponse(
    @field:Schema(description = "투표 선택지 ID", example = "23")
    val optionId: Long,
    @field:Schema(description = "이 선택지를 고른 참가자 수", example = "2")
    val voteCount: Int,
    @field:Schema(description = "실명 투표일 때만 표시되는 투표자 닉네임 목록. 익명 투표면 null", nullable = true)
    val voterNicknames: List<String>?,
)

@Schema(description = "채팅 메시지 커서 조회 응답")
data class ChatMessagePageResponse(
    @field:Schema(description = "조회한 메시지 목록. 오래된 메시지부터 반환합니다.")
    val messages: List<ChatMessageResponse>,
    @field:Schema(description = "다음 페이지 조회에 사용할 가장 오래된 메시지 ID. 다음 페이지가 없으면 null", example = "1000", nullable = true)
    val nextId: Long?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)

@Schema(description = "여행 당일 현재 로드맵 진행 상태")
data class CurrentTravelRoadmapResponse(
    @field:Schema(description = "현재 여행 로드맵을 표시할 수 있는 상태인지 여부", example = "true")
    val active: Boolean,
    @field:Schema(description = "여행 당일 일차. 여행 당일이 아니면 null", example = "1", nullable = true)
    val dayNumber: Int?,
    @field:Schema(description = "전체 여행 일수", example = "2")
    val totalDays: Int,
    @field:Schema(description = "현재 시각에 여행 중이어야 하는 장소. 없으면 null", nullable = true)
    val currentPlace: TravelRoadmapPlaceResponse?,
    @field:Schema(description = "현재 장소 다음에 방문할 일정. 없으면 null", nullable = true)
    val nextPlace: TravelRoadmapPlaceResponse?,
    @field:Schema(description = "당일 전체 방문 장소와 진행 상태 목록")
    val places: List<TravelRoadmapPlaceResponse>,
)

@Schema(description = "여행 로드맵의 방문 장소 진행 정보")
data class TravelRoadmapPlaceResponse(
    @field:Schema(description = "TourismContent ID", example = "126508")
    val contentId: Long,
    @field:Schema(description = "당일 방문 순서", example = "2")
    val sequence: Int,
    @field:Schema(description = "관광 장소명", example = "주산지")
    val title: String,
    @field:Schema(description = "관광 장소 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "관광 장소 위도. 좌표 미제공이면 null", nullable = true)
    val latitude: Double?,
    @field:Schema(description = "관광 장소 경도. 좌표 미제공이면 null", nullable = true)
    val longitude: Double?,
    @field:Schema(description = "방문 예정 일시. 시간 미지정이면 null", example = "2026-09-12T14:00:00", nullable = true)
    val scheduledAt: LocalDateTime?,
    @field:Schema(description = "완료, 현재, 예정 진행 상태", example = "CURRENT")
    val progress: TravelRoadmapProgress,
)

@Schema(
    description = "로드맵 장소 진행 상태. COMPLETED=방문 시각이 지난 장소, CURRENT=현재 여행 중인 장소, UPCOMING=다음 방문 예정 장소",
    allowableValues = ["COMPLETED", "CURRENT", "UPCOMING"],
)
enum class TravelRoadmapProgress {
    COMPLETED,
    CURRENT,
    UPCOMING,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "내 채팅방 목록의 채팅방 요약 정보")
data class MyChatRoomSummaryResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "연결된 여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val title: String,
    @field:Schema(description = "채팅방 소개", nullable = true)
    val description: String?,
    @field:Schema(description = "여행 시작일", example = "2026-09-12", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일 여행이면 null", nullable = true, type = "string", format = "date")
    val endDate: LocalDate?,
    @field:Schema(description = "채팅방 상세와 메시지에 접근할 수 있는지 여부. 종료 후 2주가 지나면 false", example = "true")
    val chatAvailable: Boolean,
    @field:Schema(description = "채팅방 썸네일 URL", nullable = true)
    val thumbnail: String? = null,
    @field:Schema(description = "진행 중인 채팅방 상태. 지난 여행 요약이면 null", nullable = true)
    val status: ChatRoomStatus? = null,
    @field:Schema(description = "모집 마감까지 남은 일수. 지난 여행이면 null", nullable = true)
    val recruitmentDDay: Long? = null,
    @field:Schema(description = "여행 종료 여부", example = "false")
    val ended: Boolean,
    @field:Schema(description = "호스트가 완료한 커스텀 코스를 공개할 수 있는지 여부", example = "false")
    val coursePublicationAvailable: Boolean,
    @field:Schema(description = "현재 승인된 참가자 수. 지난 여행 요약이면 null", nullable = true)
    val participantCount: Int? = null,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원. 지난 여행 요약이면 null", nullable = true)
    val maxParticipants: Int? = null,
    @field:Schema(description = "로그인 사용자의 읽지 않은 메시지 수. 채팅 접근 불가면 null", nullable = true)
    val unreadMessageCount: Long? = null,
    @field:Schema(description = "최근 메시지 요약. 채팅 접근 불가 또는 메시지가 없으면 null", nullable = true)
    val latestMessage: LatestChatMessageResponse? = null,
)

@Schema(description = "모임 검색 결과 채팅방 정보")
data class SearchChatRoomResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val title: String,
    @field:Schema(description = "채팅방 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "채팅방 여행 상태", example = "RECRUITING")
    val status: ChatRoomStatus,
    @field:Schema(description = "로그인 사용자의 찜 여부", example = "false")
    val favorite: Boolean,
    @field:Schema(description = "현재 승인된 참가자 수", example = "4")
    val participantCount: Int,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원", example = "5")
    val maxParticipants: Int,
    @field:Schema(description = "여행 코스 태그 목록")
    val tags: List<TravelCourseTagResponse>,
)

@Schema(description = "지도 반경 내 모집 중인 채팅방 정보")
data class MapChatRoomResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val title: String,
    @field:Schema(description = "채팅방 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "채팅방 여행 상태", example = "RECRUITING")
    val status: ChatRoomStatus,
    @field:Schema(description = "로그인 사용자의 찜 여부", example = "false")
    val favorite: Boolean,
    @field:Schema(description = "현재 승인된 참가자 수", example = "2")
    val participantCount: Int,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원", example = "5")
    val maxParticipants: Int,
    @field:Schema(description = "여행 코스 태그 목록")
    val tags: List<TravelCourseTagResponse>,
    @field:Schema(description = "집합 장소 위도", example = "36.5760")
    val meetingLatitude: Double,
    @field:Schema(description = "집합 장소 경도", example = "128.9700")
    val meetingLongitude: Double,
    @field:Schema(description = "집합 장소 이름 또는 상세 안내", nullable = true)
    val meetingDetails: String?,
    @field:Schema(description = "요청 좌표로부터 집합 장소까지의 직선거리(m)", example = "842")
    val distanceMeters: Long,
)

@Schema(description = "호스트 승인 또는 대기열 대기 중인 채팅방 정보")
data class MyWaitingChatRoomResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "채팅방 제목", example = "주왕산 & 주산지 힐링 트레킹")
    val title: String,
    @field:Schema(description = "채팅방 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "호스트 승인 대기 또는 승인 후 대기열 상태", example = "WAITLISTED")
    val applicationStatus: JoinApplicationStatus,
    @field:Schema(description = "승인 후 대기열 순번. 호스트 승인 대기면 null", example = "2", nullable = true)
    val waitlistPosition: Int?,
    @field:Schema(description = "당일 또는 숙박 여행 유형", example = "OVERNIGHT")
    val tripType: TripType,
    @field:Schema(description = "여행 시작일", example = "2026-09-12", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일 여행이면 null", nullable = true, type = "string", format = "date")
    val endDate: LocalDate?,
    @field:Schema(description = "숙박 일수", example = "1")
    val tripNights: Int,
    @field:Schema(description = "여행 일수", example = "2")
    val tripDays: Int,
    @field:Schema(description = "당일 여행 시작 시각. 숙박 여행이면 null", nullable = true)
    val dayTripStartTime: LocalTime?,
    @field:Schema(description = "당일 여행 종료 시각. 숙박 여행이면 null", nullable = true)
    val dayTripEndTime: LocalTime?,
    @field:Schema(description = "집합 일시", example = "2026-09-12T08:30:00")
    val meetingDateTime: LocalDateTime,
    @field:Schema(description = "집합 장소 위도", nullable = true)
    val meetingLatitude: Double?,
    @field:Schema(description = "집합 장소 경도", nullable = true)
    val meetingLongitude: Double?,
    @field:Schema(description = "집합 장소 이름 또는 상세 안내", nullable = true)
    val meetingDetails: String?,
    @field:Schema(description = "현재 승인된 참가자 수", example = "4")
    val participantCount: Int,
    @field:Schema(description = "호스트를 포함한 최대 참가 인원", example = "5")
    val maxParticipants: Int,
)

@Schema(description = "채팅방 찜 토글 결과")
data class ChatRoomFavoriteResponse(
    @field:Schema(description = "변경 후 로그인 사용자의 채팅방 찜 여부", example = "true")
    val favorite: Boolean,
)

@Schema(description = "채팅방 최근 메시지 요약")
data class LatestChatMessageResponse(
    @field:Schema(description = "최근 메시지 유형", example = "TEXT")
    val type: ChatMessageType,
    @field:Schema(description = "최근 메시지 발신자 닉네임", example = "따스한 사슴 3492")
    val senderNickname: String,
    @field:Schema(description = "최근 메시지 본문 또는 카드 설명")
    val content: String,
    @field:Schema(description = "최근 메시지 전송 일시", example = "2026-09-12T13:30:00")
    val sentAt: LocalDateTime,
)

@Schema(description = "채팅방 공지 정보")
data class ChatRoomNoticeResponse(
    @field:Schema(description = "공지 ID", example = "12")
    val noticeId: Long,
    @field:Schema(description = "공지 내용. 삭제된 공지는 null", nullable = true)
    val content: String?,
    @field:Schema(description = "상단 고정 공지 여부", example = "true")
    val pinned: Boolean,
    @field:Schema(description = "공지 작성자 닉네임", example = "따스한 사슴 3492")
    val authorNickname: String,
    @field:Schema(description = "공지 등록 일시", example = "2026-09-01T12:00:00")
    val createdAt: LocalDateTime,
)

@Schema(description = "고정·일반 채팅방 공지 이력")
data class ChatRoomNoticeHistoryResponse(
    @field:Schema(description = "생성일 내림차순의 고정 공지 목록")
    val pinnedNotices: List<ChatRoomNoticeResponse>,
    @field:Schema(description = "생성일 내림차순의 일반 공지 목록")
    val unpinnedNotices: List<ChatRoomNoticeResponse>,
)
