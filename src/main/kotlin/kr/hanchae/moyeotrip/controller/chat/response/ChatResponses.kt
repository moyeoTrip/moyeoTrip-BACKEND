package kr.hanchae.moyeotrip.controller.chat.response

import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ChatRoomDetailResponse(
    val roomId: Long,
    val title: String,
    val description: String?,
    val latestNotice: ChatRoomNoticeResponse?,
    val course: TravelCourseResponse,
    val startDate: LocalDate,
    val recruitmentDeadlineDate: LocalDate,
    val tripDays: Int,
    val dayTripStartTime: LocalTime?,
    val dayTripEndTime: LocalTime?,
    val meetingLatitude: Double,
    val meetingLongitude: Double,
    val meetingDateTime: LocalDateTime,
    val participationFee: Long?,
    val dDay: Long,
    val hostId: Long,
    val participantCount: Int,
    val maxParticipants: Int,
    val approvedWaitlistCount: Int,
    val pendingApplicationCount: Int?,
    val status: ChatRoomStatus,
    val participants: List<ChatParticipantResponse>,
    val myState: ChatRoomMyState,
    val myWaitlistPosition: Int?,
)

data class TravelCourseResponse(
    val courseId: Long,
    val title: String,
    val type: TravelCourseType,
    val editable: Boolean,
    val places: List<TravelCoursePlaceResponse>,
)

data class TravelCoursePlaceResponse(
    val sequence: Int,
    val placeName: String,
    val description: String?,
)

data class ChatParticipantResponse(
    val userId: Long,
    val nickname: String,
    val role: ChatParticipantRole,
)

enum class ChatRoomMyState(
    description: String,
) {
    PARTICIPANT("참가자"),
    PENDING_APPROVAL("참가 승인 대기"),
    WAITING("참가 대기열(승인됨)"),
    REJECTED("거절됨"),
    NONE("참가하지 않음"),
}

data class JoinChatRoomResponse(
    val roomId: Long,
    val result: JoinResult,
)

enum class JoinResult { PENDING_APPROVAL }

data class JoinApplicationResponse(
    val applicationId: Long,
    val applicationMessage: String,
    val applicant: ApplicantProfileResponse,
    val appliedAt: LocalDateTime,
)

data class ApplicantProfileResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val gender: Gender?,
    val age: Int?,
    val mannerRating: Double?,
    val completedTripCount: Int,
)

data class ApproveJoinApplicationResponse(
    val applicationId: Long,
    val result: ApprovalResult,
    val waitlistPosition: Int?,
)

enum class ApprovalResult { JOINED, WAITLISTED }

data class LeaveChatRoomResponse(
    val roomId: Long,
    val result: LeaveResult,
    val promotedUserId: Long?,
)

enum class LeaveResult { LEFT, HOST_LEFT_AND_ROOM_CANCELLED, APPLICATION_CANCELLED, WAITLIST_CANCELLED }

data class ChatMessageResponse(
    val messageId: Long,
    val type: ChatMessageType,
    val senderId: Long?,
    val senderNickname: String,
    val content: String,
    val createdAt: LocalDateTime,
)

data class ChatMessagePageResponse(
    val messages: List<ChatMessageResponse>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class MyChatRoomSummaryResponse(
    val roomId: Long,
    val title: String,
    val status: ChatRoomStatus,
    val dDay: Long,
    val unreadMessageCount: Long,
    val latestMessage: LatestChatMessageResponse,
)

data class MyWaitingChatRoomResponse(
    val roomId: Long,
    val title: String,
    val startDate: LocalDate,
    val dDay: Long,
    val roomStatus: ChatRoomStatus,
    val applicationStatus: JoinApplicationStatus,
    val waitlistPosition: Int?,
)

data class LatestChatMessageResponse(
    val type: ChatMessageType,
    val senderNickname: String,
    val content: String,
    val sentAt: LocalDateTime,
)

data class ChatRoomNoticeResponse(
    val noticeId: Long,
    val content: String?,
    val authorNickname: String,
    val createdAt: LocalDateTime,
    val cleared: Boolean,
)
