package kr.hanchae.moyeotrip.controller.chat.response

import com.fasterxml.jackson.annotation.JsonInclude
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
    val thumbnail: String?,
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
    val recruitmentDDay: Long,
    val hostId: Long,
    val hostProfileImageUrl: String?,
    val participantCount: Int,
    val maxParticipants: Int,
    val status: ChatRoomStatus,
    val favorite: Boolean,
    val latestPinnedNotice: ChatRoomNoticeResponse?,
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

data class PublicTravelCourseDetailResponse(
    val courseId: Long,
    val title: String,
    val description: String?,
    val creatorNickname: String?,
    val creatorTravelStartDate: LocalDate?,
    val creatorTravelEndDate: LocalDate?,
    val chatRoomCount: Long,
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

data class ChatRoomMemberListResponse(
    val participantCount: Int,
    val maxParticipants: Int,
    val waitlistCount: Int,
    val members: List<ChatRoomMemberResponse>,
)

data class ChatRoomMemberResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val completedTripCount: Int,
    val host: Boolean,
    val me: Boolean,
)

data class ChatRoomKickHistoryResponse(
    val kickHistoryId: Long,
    val roomId: Long,
    val roomTitle: String,
    val reason: String,
    val kickedAt: LocalDateTime,
)

data class JoinChatRoomResponse(
    val roomId: Long,
    val result: JoinResult,
)

enum class JoinResult { JOINED, WAITLISTED, PENDING_APPROVAL }

data class JoinEligibilityResponse(
    val canApply: Boolean,
)

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
    val imageUrl: String? = null,
    val tourismContent: SharedTourismContentResponse? = null,
    val location: SharedLocationResponse? = null,
    val poll: ChatPollResponse? = null,
    val replyTo: RepliedChatMessageResponse? = null,
    val mentions: List<MentionedChatUserResponse> = emptyList(),
)

data class MentionedChatUserResponse(
    val userId: Long,
    val nickname: String,
)

data class RepliedChatMessageResponse(
    val messageId: Long,
    val senderNickname: String,
    val content: String,
)

data class SharedTourismContentResponse(
    val contentId: Long,
    val title: String,
    val address: String?,
    val thumbnail: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class SharedLocationResponse(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
)

data class ChatPollResponse(
    val question: String,
    val anonymous: Boolean,
    val totalVoteCount: Int,
    val options: List<ChatPollOptionResponse>,
)

data class ChatPollOptionResponse(
    val optionId: Long,
    val text: String,
    val voteCount: Int,
    val votedByMe: Boolean,
    val voterNicknames: List<String>?,
)

data class ChatMessagePageResponse(
    val messages: List<ChatMessageResponse>,
    val nextId: Long?,
    val hasNext: Boolean,
)

data class CurrentTravelRoadmapResponse(
    val active: Boolean,
    val dayNumber: Int?,
    val totalDays: Int,
    val currentPlace: TravelRoadmapPlaceResponse?,
    val nextPlace: TravelRoadmapPlaceResponse?,
    val places: List<TravelRoadmapPlaceResponse>,
)

data class TravelRoadmapPlaceResponse(
    val contentId: Long,
    val sequence: Int,
    val title: String,
    val thumbnail: String?,
    val latitude: Double?,
    val longitude: Double?,
    val scheduledAt: LocalDateTime?,
    val progress: TravelRoadmapProgress,
)

enum class TravelRoadmapProgress {
    COMPLETED,
    CURRENT,
    UPCOMING,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyChatRoomSummaryResponse(
    val roomId: Long,
    val courseId: Long,
    val title: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val chatAvailable: Boolean,
    val thumbnail: String? = null,
    val status: ChatRoomStatus? = null,
    val recruitmentDDay: Long? = null,
    val ended: Boolean,
    val coursePublicationAvailable: Boolean,
    val participantCount: Int? = null,
    val maxParticipants: Int? = null,
    val unreadMessageCount: Long? = null,
    val latestMessage: LatestChatMessageResponse? = null,
)

data class SearchChatRoomResponse(
    val roomId: Long,
    val title: String,
    val description: String?,
    val thumbnail: String?,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recruitmentDeadlineDate: LocalDate,
    val hostId: Long,
    val participantCount: Int,
    val maxParticipants: Int,
    val courseTitle: String,
    val tags: List<TravelCourseTagResponse>,
)

data class MyWaitingChatRoomResponse(
    val roomId: Long,
    val title: String,
    val thumbnail: String?,
    val applicationStatus: JoinApplicationStatus,
    val waitlistPosition: Int?,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val tripNights: Int,
    val tripDays: Int,
    val dayTripStartTime: LocalTime?,
    val dayTripEndTime: LocalTime?,
    val meetingDateTime: LocalDateTime,
    val meetingLatitude: Double?,
    val meetingLongitude: Double?,
    val meetingDetails: String?,
    val participantCount: Int,
    val maxParticipants: Int,
)

data class ChatRoomFavoriteResponse(
    val favorite: Boolean,
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
    val pinned: Boolean,
    val authorNickname: String,
    val createdAt: LocalDateTime,
)

data class ChatRoomNoticeHistoryResponse(
    val pinnedNotices: List<ChatRoomNoticeResponse>,
    val unpinnedNotices: List<ChatRoomNoticeResponse>,
)
