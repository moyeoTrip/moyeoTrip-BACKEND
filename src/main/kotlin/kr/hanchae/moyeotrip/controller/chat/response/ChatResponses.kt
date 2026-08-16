package kr.hanchae.moyeotrip.controller.chat.response

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

data class ChatRoomDetailResponse(
    val roomId: Long,
    val title: String,
    val description: String?,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recruitmentDeadlineDate: LocalDate,
    val tripNights: Int,
    val tripDays: Int,
    val dayTripStartTime: LocalTime?,
    val dayTripEndTime: LocalTime?,
    val meetingLatitude: Double?,
    val meetingLongitude: Double?,
    val meetingDetails: String?,
    val meetingDateTime: LocalDateTime,
    val participationFee: Long?,
    val genderRestriction: GenderRestriction,
    val minimumAge: Int?,
    val maximumAge: Int?,
    val joinApprovalMode: JoinApprovalMode,
    val dDay: Long,
    val hostId: Long,
    val hostProfileImageUrl: String?,
    val participantCount: Int,
    val maxParticipants: Int,
    val status: ChatRoomStatus,
    val participants: List<ChatParticipantResponse>,
)

data class TravelCourseResponse(
    val courseId: Long,
    val title: String,
    val type: TravelCourseType,
    val editable: Boolean,
    val places: List<TravelCoursePlaceResponse>,
)

data class TravelCourseDetailResponse(
    val room: TravelCourseRoomResponse?,
    val course: TravelCourseInformationResponse,
)

data class TravelCourseRoomResponse(
    val roomId: Long,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val dayTripStartTime: LocalTime?,
    val dayTripEndTime: LocalTime?,
)

data class TravelCourseInformationResponse(
    val courseId: Long,
    val title: String,
    val description: String?,
    val type: TravelCourseType,
    val travelTime: String,
    val distanceKm: Double,
    val averageRating: Double?,
    val ratingCount: Long,
    val tags: List<TravelCourseTagResponse>,
    val thumbnail: String?,
    val places: List<TravelCoursePlaceResponse>,
)

data class TravelCoursePlaceResponse(
    val contentId: Long,
    val dayNumber: Int,
    val sequence: Int,
    val visitTime: LocalTime?,
    val title: String,
    val thumbnail: String?,
    val latitude: Double,
    val longitude: Double,
)

data class ChatParticipantResponse(
    val userId: Long,
    val profileImageUrl: String?,
)

data class JoinChatRoomResponse(
    val roomId: Long,
    val result: JoinResult,
)

enum class JoinResult { JOINED, WAITLISTED, PENDING_APPROVAL }

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
)
