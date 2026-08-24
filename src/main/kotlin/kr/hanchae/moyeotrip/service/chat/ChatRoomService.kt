package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatPollRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateCustomCourseRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateSettlementMemoRequest
import kr.hanchae.moyeotrip.controller.chat.request.CustomCoursePlaceRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.MyChatRoomFilter
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.request.ShareTourismContentRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateMeetingInfoRequest
import kr.hanchae.moyeotrip.controller.chat.response.ApplicantProfileResponse
import kr.hanchae.moyeotrip.controller.chat.response.ApprovalResult
import kr.hanchae.moyeotrip.controller.chat.response.ApproveJoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessagePageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatParticipantResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollOptionResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedOptionResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomFavoriteResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomKickHistoryResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomMemberListResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomMemberResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomNoticeHistoryResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomNoticeResponse
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.CurrentTravelRoadmapResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinEligibilityResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinResult
import kr.hanchae.moyeotrip.controller.chat.response.LatestChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveResult
import kr.hanchae.moyeotrip.controller.chat.response.MentionedChatUserResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyChatRoomSummaryResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyWaitingChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.PublicTravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.RepliedChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.SearchChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.SharedLocationResponse
import kr.hanchae.moyeotrip.controller.chat.response.SharedTourismContentResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseInformationResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCoursePlaceResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelRoadmapPlaceResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelRoadmapProgress
import kr.hanchae.moyeotrip.controller.tour.request.UpdateTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatPollOption
import kr.hanchae.moyeotrip.entity.chat.ChatPollVote
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomFavorite
import kr.hanchae.moyeotrip.entity.chat.ChatRoomJoinApplication
import kr.hanchae.moyeotrip.entity.chat.ChatRoomKickHistory
import kr.hanchae.moyeotrip.entity.chat.ChatRoomNotice
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.GenderRestriction
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCoursePlace
import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatPollOptionRepository
import kr.hanchae.moyeotrip.repository.ChatPollVoteRepository
import kr.hanchae.moyeotrip.repository.ChatRoomFavoriteRepository
import kr.hanchae.moyeotrip.repository.ChatRoomJoinApplicationRepository
import kr.hanchae.moyeotrip.repository.ChatRoomKickHistoryRepository
import kr.hanchae.moyeotrip.repository.ChatRoomNoticeRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TravelCoursePlaceRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRatingRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class ChatRoomService(
    private val roomRepository: ChatRoomRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val applicationRepository: ChatRoomJoinApplicationRepository,
    private val messageRepository: ChatMessageRepository,
    private val pollOptionRepository: ChatPollOptionRepository,
    private val pollVoteRepository: ChatPollVoteRepository,
    private val courseRepository: TravelCourseRepository,
    private val coursePlaceRepository: TravelCoursePlaceRepository,
    private val courseTagRepository: TravelCourseTagRepository,
    private val courseRatingRepository: TravelCourseRatingRepository,
    private val roomFavoriteRepository: ChatRoomFavoriteRepository,
    private val kickHistoryRepository: ChatRoomKickHistoryRepository,
    private val tourismContentRepository: TourismContentRepository,
    private val userRepository: UserRepository,
    private val userBlockRepository: UserBlockRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val noticeRepository: ChatRoomNoticeRepository,
    private val notificationService: NotificationService,
    private val realtimeMessagingService: RealtimeMessagingService,
) {
    @Transactional
    fun createRoom(
        userId: Long,
        request: CreateChatRoomRequest,
        thumbnail: MultipartFile? = null,
    ): CreateChatRoomResponse {
        validateTripSchedule(request)
        validateAgeRestriction(request)
        val host = findUser(userId)
        val course = resolveCourse(host, request)
        val thumbnailUrl =
            thumbnail
                ?.takeUnless(MultipartFile::isEmpty)
                ?.let(::uploadChatRoomThumbnail)
        val room =
            roomRepository.saveAndFlush(
                ChatRoom(
                    host = host,
                    course = course,
                    roomTitle = request.title.trim(),
                    description = request.description?.trim()?.takeIf(String::isNotEmpty),
                    thumbnail = thumbnailUrl,
                    maxParticipants = request.maxParticipants,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    recruitmentDeadlineDate = request.recruitmentDeadlineDate,
                    dayTripStartTime = request.dayTripStartTime,
                    dayTripEndTime = request.dayTripEndTime,
                    meetingLatitude = request.meetingLatitude,
                    meetingLongitude = request.meetingLongitude,
                    meetingDetails = request.meetingDetails?.trim()?.takeIf(String::isNotEmpty),
                    meetingDateTime = request.meetingDateTime,
                    participationFee = request.participationFee,
                    genderRestriction = request.genderRestriction,
                    minimumAge = request.minimumAge,
                    maximumAge = request.maximumAge,
                    joinApprovalMode = request.joinApprovalMode,
                ),
            )
        val hostParticipant =
            participantRepository.saveAndFlush(ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST))
        val openingMessage = saveSystemMessage(room, "${host.nickname()}님이 모임을 개설했어요.")
        hostParticipant.readThrough(openingMessage.id)
        notificationService.notifyRoomCreated(room)
        return CreateChatRoomResponse(roomId = room.id)
    }

    @Transactional(readOnly = true)
    fun getRoom(
        userId: Long,
        roomId: Long,
    ): ChatRoomDetailResponse =
        findRoom(roomId).toDetail(
            favorite = roomFavoriteRepository.existsByUserIdAndChatRoomId(userId, roomId),
        )

    @Transactional
    fun toggleRoomFavorite(
        userId: Long,
        roomId: Long,
    ): ChatRoomFavoriteResponse {
        val room = findRoomForUpdate(roomId)
        val existingFavorite = roomFavoriteRepository.findByUserIdAndChatRoomId(userId, roomId)
        if (existingFavorite != null) {
            roomFavoriteRepository.delete(existingFavorite)
            return ChatRoomFavoriteResponse(favorite = false)
        }
        roomFavoriteRepository.save(ChatRoomFavorite(user = findUser(userId), chatRoom = room))
        return ChatRoomFavoriteResponse(favorite = true)
    }

    @Transactional(readOnly = true)
    fun getMyRooms(
        userId: Long,
        filter: MyChatRoomFilter = MyChatRoomFilter.ALL,
    ): List<MyChatRoomSummaryResponse> =
        participantRepository
            .findAllByUserId(userId)
            .filter { it.chatRoom.matches(filter) }
            .map { it.toMySummary() }
            .sortedWith(
                compareByDescending<MyChatRoomSummaryResponse> { it.endDate ?: it.startDate }
                    .thenByDescending { it.latestMessage?.sentAt ?: LocalDateTime.MIN },
            )

    @Transactional(readOnly = true)
    fun searchRooms(
        userId: Long,
        keyword: String?,
        limit: Int,
    ): List<SearchChatRoomResponse> {
        val blockedUserIds = userBlockRepository.findRelatedUserIds(userId).ifEmpty { listOf(NO_USER_ID) }
        val rooms =
            roomRepository
                .searchRooms(
                    userId = userId,
                    blockedUserIds = blockedUserIds,
                    keyword = keyword?.trim()?.takeIf(String::isNotEmpty),
                    today = LocalDate.now(),
                    pageable = PageRequest.of(0, limit.coerceIn(1, MAX_DISCOVER_ROOM_LIMIT)),
                )
        val favoriteRoomIds =
            rooms
                .map(ChatRoom::id)
                .takeIf(List<Long>::isNotEmpty)
                ?.let { roomFavoriteRepository.findChatRoomIdsByUserIdAndChatRoomIdIn(userId, it) }
                .orEmpty()
        return rooms.map { room ->
            SearchChatRoomResponse(
                roomId = room.id,
                title = room.roomTitle,
                description = room.description,
                thumbnail = room.thumbnail,
                tripType = room.tripType,
                startDate = room.startDate,
                endDate = room.endDate,
                dayTripStartTime = room.dayTripStartTime,
                dayTripEndTime = room.dayTripEndTime,
                recruitmentDeadlineDate = room.recruitmentDeadlineDate,
                recruitmentDDay = room.recruitmentDDay(),
                status = room.status,
                favorite = room.id in favoriteRoomIds,
                meetingLatitude = room.meetingLatitude,
                meetingLongitude = room.meetingLongitude,
                meetingDetails = room.meetingDetails,
                meetingDateTime = room.meetingDateTime,
                hostId = room.host.id,
                participantCount = participantRepository.countByChatRoomId(room.id).toInt(),
                maxParticipants = room.maxParticipants,
                courseTitle = room.course.title,
                tags =
                    room.course.tags
                        .sortedBy { it.id }
                        .map { TravelCourseTagResponse(it.id, it.name) },
            )
        }
    }

    @Transactional(readOnly = true)
    fun getMyWaitingRooms(userId: Long): List<MyWaitingChatRoomResponse> =
        applicationRepository
            .findAllByUserIdAndStatusInOrderByCreatedDateTimeDesc(
                userId,
                ACTIVE_APPLICATION_STATUSES,
            ).filter { !it.chatRoom.hasEnded() }
            .map { application ->
                val room = application.chatRoom
                val waitlistPosition =
                    if (application.status == JoinApplicationStatus.WAITLISTED) {
                        applicationRepository
                            .findAllByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(room.id, JoinApplicationStatus.WAITLISTED)
                            .indexOfFirst { it.id == application.id }
                            .takeIf { it >= 0 }
                            ?.plus(1)
                    } else {
                        null
                    }
                MyWaitingChatRoomResponse(
                    roomId = room.id,
                    title = room.roomTitle,
                    thumbnail = room.thumbnail,
                    applicationStatus = application.status,
                    waitlistPosition = waitlistPosition,
                    tripType = room.tripType,
                    startDate = room.startDate,
                    endDate = room.endDate,
                    tripNights = room.tripNights,
                    tripDays = room.tripDays,
                    dayTripStartTime = room.dayTripStartTime,
                    dayTripEndTime = room.dayTripEndTime,
                    meetingDateTime = room.meetingDateTime,
                    meetingLatitude = room.meetingLatitude,
                    meetingLongitude = room.meetingLongitude,
                    meetingDetails = room.meetingDetails,
                    participantCount = participantRepository.countByChatRoomId(room.id).toInt(),
                    maxParticipants = room.maxParticipants,
                )
            }

    @Transactional(readOnly = true)
    fun getPublicCourses(tagId: Long? = null): List<TravelCourseInformationResponse> {
        val courses =
            if (tagId == null) {
                courseRepository.findAllByTypeOrderByCreatedDateTimeDesc(TravelCourseType.PUBLIC)
            } else {
                courseRepository.findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(TravelCourseType.PUBLIC, tagId)
            }
        return courses.map { it.toInformationResponse(travelTime = it.travelTimeText()) }
    }

    @Transactional(readOnly = true)
    fun getPopularPublicCourses(): List<TravelCourseInformationResponse> =
        courseRepository
            .findPopularPublicCourses(PageRequest.of(0, POPULAR_COURSE_LIMIT))
            .map { it.toInformationResponse(travelTime = it.travelTimeText()) }

    @Transactional(readOnly = true)
    fun getRoomCourse(roomId: Long): TravelCourseDetailResponse {
        val room = findRoom(roomId)
        return TravelCourseDetailResponse(
            room = room.toCourseRoomResponse(),
            course = room.course.toInformationResponse(travelTime = room.travelTimeText()),
        )
    }

    @Transactional
    fun updateRoomCourse(
        hostId: Long,
        roomId: Long,
        request: UpdateTravelCourseRequest,
    ): TravelCourseInformationResponse {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        val course = room.course
        if (room.status != ChatRoomStatus.RECRUITING ||
            course.type != TravelCourseType.CUSTOM ||
            course.owner?.id != hostId
        ) {
            throw BaseException(ErrorCode.TRAVEL_COURSE_NOT_EDITABLE)
        }
        validateCustomCourseSchedule(request.places, room.tripDays)

        course.clearCustomPlaces()
        coursePlaceRepository.deleteAllByCourseId(course.id)
        coursePlaceRepository.flush()
        request.places.forEach { place ->
            val tourismContent =
                tourismContentRepository.findByContentId(place.contentId)
                    ?: throw BaseException(ErrorCode.TOURISM_CONTENT_NOT_FOUND)
            coursePlaceRepository.save(
                course.addCustomPlace(
                    tourismContent = tourismContent,
                    dayNumber = place.dayNumber,
                    sequence = place.sequence,
                    visitTime = place.visitTime,
                ),
            )
        }
        val message = saveSystemMessage(room, "호스트가 여행 코스를 변경했어요.")
        notificationService.notifyCourseUpdated(room, message.id)
        return course.toInformationResponse(travelTime = room.travelTimeText())
    }

    @Transactional
    fun updateMeetingInfo(
        hostId: Long,
        roomId: Long,
        request: UpdateMeetingInfoRequest,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        if (room.status != ChatRoomStatus.RECRUITING) {
            throw BaseException(ErrorCode.MEETING_INFO_NOT_EDITABLE)
        }
        if ((request.meetingLatitude == null) != (request.meetingLongitude == null) ||
            request.meetingDateTime.toLocalDate() > room.startDate
        ) {
            throw BaseException(ErrorCode.BAD_REQUEST)
        }
        room.updateMeetingInfo(
            latitude = request.meetingLatitude,
            longitude = request.meetingLongitude,
            details = request.meetingDetails?.trim()?.takeIf(String::isNotEmpty),
            dateTime = request.meetingDateTime,
        )
        val message = saveSystemMessage(room, "집합 정보가 변경되었어요.")
        notificationService.notifyMeetingInfoUpdated(room, message.id)
    }

    @Transactional(readOnly = true)
    fun getCourse(courseId: Long): PublicTravelCourseDetailResponse {
        val course = findPublicCourse(courseId)
        val creator = course.owner?.takeIf { course.showCreatorNickname }
        val creatorTravelRoom =
            creator?.let {
                roomRepository.findFirstByCourseIdAndHostIdAndStatusOrderByStartDateAsc(
                    courseId = courseId,
                    hostId = it.id,
                    status = ChatRoomStatus.CONFIRMED,
                )
            }
        return PublicTravelCourseDetailResponse(
            courseId = course.id,
            title = course.title,
            description = course.description,
            creatorNickname =
                if (course.showCreatorNickname) {
                    creator?.information?.nickname ?: course.creatorNickname
                } else {
                    null
                },
            creatorTravelStartDate = creatorTravelRoom?.startDate,
            creatorTravelEndDate = creatorTravelRoom?.endDate,
            chatRoomCount = roomRepository.countByCourseIdAndStatusNot(courseId, ChatRoomStatus.CANCELLED),
            travelTime = course.travelTimeText(),
            distanceKm = course.totalDistanceKm(),
            averageRating = courseRatingRepository.findAverageByCourseId(courseId)?.rounded(1),
            ratingCount = courseRatingRepository.countByCourseId(courseId),
            tags = course.tags.sortedBy { it.id }.map { TravelCourseTagResponse(it.id, it.name) },
            thumbnail =
                course.places
                    .firstOrNull()
                    ?.tourismContent
                    ?.thumbnail,
            places = course.toResponse(editable = false).places,
        )
    }

    @Transactional
    fun rateCourse(
        userId: Long,
        roomId: Long,
        score: Int,
    ) {
        val room = findRoom(roomId)
        if (!participantRepository.hasCompletedTrip(roomId, userId, LocalDate.now())) {
            throw BaseException(ErrorCode.TRAVEL_COURSE_RATING_NOT_ALLOWED)
        }
        courseRatingRepository.findByChatRoomIdAndUserId(roomId, userId)?.update(score)
            ?: courseRatingRepository.save(
                TravelCourseRating(
                    course = room.course,
                    chatRoom = room,
                    user = findUser(userId),
                    score = score,
                ),
            )
    }

    @Transactional
    fun applyToJoin(
        userId: Long,
        roomId: Long,
        request: JoinChatRoomRequest,
    ): JoinChatRoomResponse {
        val room = findRoomForUpdate(roomId)
        if (!room.canAcceptJoinApplication()) throw BaseException(ErrorCode.CHAT_ROOM_CLOSED)
        if (participantRepository.existsByChatRoomIdAndUserId(roomId, userId) ||
            applicationRepository.existsByChatRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_APPLICATION_STATUSES,
            )
        ) {
            throw BaseException(ErrorCode.CHAT_ROOM_ALREADY_JOINED)
        }
        val user = findUser(userId)
        requireJoinConditions(room, user)
        if (room.joinApprovalMode == JoinApprovalMode.MANUAL) {
            val applicationMessage =
                request.applicationMessage
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: throw BaseException(ErrorCode.CHAT_JOIN_APPLICATION_MESSAGE_REQUIRED)
            val application =
                applicationRepository.saveAndFlush(
                    ChatRoomJoinApplication(
                        chatRoom = room,
                        user = user,
                        applicationMessage = applicationMessage,
                    ),
                )
            return JoinChatRoomResponse(roomId, JoinResult.PENDING_APPROVAL, application.id, application.status)
        }

        val participantCount = participantRepository.countByChatRoomId(roomId).toInt()
        if (participantCount >= room.maxParticipants) {
            val application =
                applicationRepository.saveAndFlush(
                    ChatRoomJoinApplication(
                        chatRoom = room,
                        user = user,
                        applicationMessage = request.applicationMessage?.trim().orEmpty(),
                        status = JoinApplicationStatus.WAITLISTED,
                    ),
                )
            return JoinChatRoomResponse(roomId, JoinResult.WAITLISTED, application.id, application.status)
        }

        val participant =
            participantRepository.saveAndFlush(
                ChatRoomParticipant(chatRoom = room, user = user, role = ChatParticipantRole.MEMBER),
            )
        val latestMessageId = recordParticipantJoined(room, user, participantCount + 1)
        participant.readThrough(latestMessageId)
        return JoinChatRoomResponse(roomId, JoinResult.JOINED)
    }

    @Transactional(readOnly = true)
    fun getJoinEligibility(
        userId: Long,
        roomId: Long,
    ): JoinEligibilityResponse {
        val room = findRoom(roomId)
        if (!room.canAcceptJoinApplication()) {
            return JoinEligibilityResponse(false)
        }
        if (participantRepository.existsByChatRoomIdAndUserId(roomId, userId) ||
            applicationRepository.existsByChatRoomIdAndUserIdAndStatusIn(roomId, userId, ACTIVE_APPLICATION_STATUSES)
        ) {
            return JoinEligibilityResponse(false)
        }
        return JoinEligibilityResponse(canApply = meetsJoinConditions(room, findUser(userId)))
    }

    @Transactional(readOnly = true)
    fun getPendingApplications(
        hostId: Long,
        roomId: Long,
    ): List<JoinApplicationResponse> {
        requireHost(findRoom(roomId), hostId)
        return applicationRepository
            .findAllByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(roomId, JoinApplicationStatus.PENDING)
            .map { application ->
                JoinApplicationResponse(
                    application.id,
                    application.applicationMessage,
                    application.user.toApplicantProfile(),
                    application.createdDateTime,
                )
            }
    }

    @Transactional
    fun approveApplication(
        hostId: Long,
        roomId: Long,
        applicationId: Long,
    ): ApproveJoinApplicationResponse {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        val application =
            applicationRepository
                .findByIdAndChatRoomId(applicationId, roomId)
                ?.takeIf { it.status == JoinApplicationStatus.PENDING }
                ?: throw BaseException(ErrorCode.CHAT_JOIN_APPLICATION_NOT_FOUND)
        val count = participantRepository.countByChatRoomId(roomId).toInt()
        return if (count < room.maxParticipants) {
            val participant =
                participantRepository.save(
                    ChatRoomParticipant(chatRoom = room, user = application.user, role = ChatParticipantRole.MEMBER),
                )
            applicationRepository.delete(application)
            val latestMessageId = recordParticipantJoined(room, application.user, count + 1)
            participant.readThrough(latestMessageId)
            ApproveJoinApplicationResponse(applicationId, ApprovalResult.JOINED, null)
        } else {
            application.moveToWaitlist()
            val position = applicationRepository.countByChatRoomIdAndStatus(roomId, JoinApplicationStatus.WAITLISTED).toInt()
            ApproveJoinApplicationResponse(applicationId, ApprovalResult.WAITLISTED, position)
        }
    }

    @Transactional
    fun rejectApplication(
        hostId: Long,
        roomId: Long,
        applicationId: Long,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        val application =
            applicationRepository
                .findByIdAndChatRoomId(applicationId, roomId)
                ?.takeIf { it.status == JoinApplicationStatus.PENDING }
                ?: throw BaseException(ErrorCode.CHAT_JOIN_APPLICATION_NOT_FOUND)
        application.reject()
    }

    @Transactional
    fun cancelJoinApplication(
        userId: Long,
        roomId: Long,
    ) {
        findRoomForUpdate(roomId)
        val application =
            applicationRepository.findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
                roomId,
                userId,
                ACTIVE_APPLICATION_STATUSES,
            ) ?: throw BaseException(ErrorCode.CHAT_JOIN_APPLICATION_NOT_FOUND)
        applicationRepository.delete(application)
    }

    @Transactional
    fun leaveRoom(
        userId: Long,
        roomId: Long,
    ): LeaveChatRoomResponse {
        val room = findRoomForUpdate(roomId)
        participantRepository.findByChatRoomIdAndUserId(roomId, userId)?.let { participant ->
            if (participant.role == ChatParticipantRole.HOST) {
                saveSystemMessage(room, "${participant.user.nickname()}님인 호스트가 모임을 나가 여행이 불발되었어요.")
                participantRepository.delete(participant)
                participantRepository.flush()
                room.cancel(LocalDateTime.now())
                return LeaveChatRoomResponse(roomId, LeaveResult.HOST_LEFT_AND_ROOM_CANCELLED, null)
            }
            participantRepository.delete(participant)
            participantRepository.flush()
            saveSystemMessage(room, "${participant.user.nickname()}님이 모임에서 나갔어요.")
            return LeaveChatRoomResponse(roomId, LeaveResult.LEFT, promoteFirstApprovedWaiter(room)?.id)
        }
        val application =
            applicationRepository.findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
                roomId,
                userId,
                ACTIVE_APPLICATION_STATUSES,
            )
                ?: throw BaseException(ErrorCode.CHAT_ROOM_NOT_JOINED)
        val result =
            when (application.status) {
                JoinApplicationStatus.PENDING -> LeaveResult.APPLICATION_CANCELLED
                JoinApplicationStatus.WAITLISTED -> LeaveResult.WAITLIST_CANCELLED
                JoinApplicationStatus.REJECTED -> error("활성 참가 신청에 거절 상태가 포함될 수 없습니다.")
            }
        applicationRepository.delete(application)
        return LeaveChatRoomResponse(roomId, result, null)
    }

    @Transactional(readOnly = true)
    fun getMembers(
        userId: Long,
        roomId: Long,
    ): ChatRoomMemberListResponse {
        requireParticipant(roomId, userId)
        val room = findRoom(roomId)
        val participants = participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(roomId)
        return ChatRoomMemberListResponse(
            participantCount = participants.size,
            maxParticipants = room.maxParticipants,
            waitlistCount =
                applicationRepository
                    .countByChatRoomIdAndStatus(roomId, JoinApplicationStatus.WAITLISTED)
                    .toInt(),
            members =
                participants.map { participant ->
                    val member = participant.user
                    ChatRoomMemberResponse(
                        userId = member.id,
                        nickname = member.nickname(),
                        profileImageUrl =
                            member.information
                                ?.profileFileName
                                ?.let(objectStorageRepository::getDownloadUrl),
                        completedTripCount = participantRepository.countCompletedTrips(member.id).toInt(),
                        host = participant.role == ChatParticipantRole.HOST,
                        me = member.id == userId,
                    )
                },
        )
    }

    @Transactional(readOnly = true)
    fun getMyKickHistories(userId: Long): List<ChatRoomKickHistoryResponse> =
        kickHistoryRepository
            .findAllByKickedUserIdOrderByCreatedDateTimeDescIdDesc(userId)
            .map {
                ChatRoomKickHistoryResponse(
                    kickHistoryId = it.id,
                    roomId = it.chatRoomId,
                    roomTitle = it.roomTitle,
                    reason = it.reason,
                    kickedAt = it.createdDateTime,
                )
            }

    @Transactional
    fun kickMember(
        hostId: Long,
        roomId: Long,
        memberId: Long,
        reason: String,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        requireChatEnabled(room)
        val participant =
            participantRepository
                .findByChatRoomIdAndUserId(roomId, memberId)
                ?.takeIf { it.role == ChatParticipantRole.MEMBER }
                ?: throw BaseException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND)
        val normalizedReason = reason.trim().takeIf(String::isNotEmpty) ?: throw BaseException(ErrorCode.BAD_REQUEST)
        val kickHistory =
            kickHistoryRepository.save(
                ChatRoomKickHistory(
                    chatRoomId = room.id,
                    roomTitle = room.roomTitle,
                    kickedUser = participant.user,
                    kickedBy = room.host,
                    reason = normalizedReason,
                ),
            )
        participantRepository.delete(participant)
        participantRepository.flush()
        notificationService.notifyChatRoomMemberKicked(kickHistory)
        saveSystemMessage(room, "${participant.user.nickname()}님이 모임에서 제외되었어요.")
        promoteFirstApprovedWaiter(room)
    }

    @Transactional
    fun changeStatus(
        hostId: Long,
        roomId: Long,
        status: ChatRoomStatus,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        if (room.status != ChatRoomStatus.RECRUITING || status == ChatRoomStatus.RECRUITING) {
            throw BaseException(ErrorCode.INVALID_CHAT_ROOM_STATUS)
        }
        if (status == ChatRoomStatus.CONFIRMED) {
            room.confirm()
            saveSystemMessage(room, "여행이 확정되었어요.")
        } else {
            saveSystemMessage(room, "여행이 불발되었어요.")
            room.cancel(LocalDateTime.now())
        }
    }

    @Transactional
    fun createNotice(
        hostId: Long,
        roomId: Long,
        notice: String,
        pinned: Boolean,
    ): Long {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        requireChatEnabled(room)
        val content = notice.trim().takeIf(String::isNotEmpty) ?: throw BaseException(ErrorCode.BAD_REQUEST)
        val savedNotice = noticeRepository.save(ChatRoomNotice(chatRoom = room, author = room.host, content = content, pinned = pinned))
        saveSystemMessage(room, "공지가 등록되었어요.\n$content")
        return savedNotice.id
    }

    @Transactional
    fun updateNotice(
        hostId: Long,
        roomId: Long,
        noticeId: Long,
        notice: String?,
        pinned: Boolean?,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        requireChatEnabled(room)
        val normalizedNotice = notice?.trim()?.takeIf(String::isNotEmpty)
        if (notice != null && normalizedNotice == null) throw BaseException(ErrorCode.BAD_REQUEST)
        val target =
            noticeRepository.findByIdAndChatRoomId(noticeId, roomId)
                ?: throw BaseException(ErrorCode.CHAT_ROOM_NOTICE_NOT_FOUND)
        if (notice == null && pinned == null) {
            noticeRepository.delete(target)
        } else {
            normalizedNotice?.let {
                target.updateContent(it)
                saveSystemMessage(room, "공지가 수정되었어요.\n$it")
            }
            pinned?.let {
                target.updatePinned(it)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getNoticeHistory(
        userId: Long,
        roomId: Long,
    ): ChatRoomNoticeHistoryResponse {
        findRoom(roomId)
        requireParticipant(roomId, userId)
        val notices =
            noticeRepository
                .findAllByChatRoomIdAndContentIsNotNullOrderByCreatedDateTimeDescIdDesc(roomId)
                .map { it.toResponse() }
        return ChatRoomNoticeHistoryResponse(
            pinnedNotices = notices.filter(ChatRoomNoticeResponse::pinned),
            unpinnedNotices = notices.filterNot(ChatRoomNoticeResponse::pinned),
        )
    }

    @Transactional
    fun sendMessage(
        userId: Long,
        roomId: Long,
        request: SendChatMessageRequest,
    ): ChatMessageResponse {
        val participant = findParticipant(roomId, userId)
        val room = participant.chatRoom
        requireChatEnabled(room)
        val replyTo =
            request.replyToMessageId?.let { messageId ->
                messageRepository.findByIdAndChatRoomId(messageId, roomId)
                    ?: throw BaseException(ErrorCode.RESOURCE_NOT_FOUND)
            }
        val participantsByUserId =
            participantRepository
                .findAllByChatRoomIdOrderByCreatedDateTimeAsc(roomId)
                .associateBy { it.user.id }
        if (!participantsByUserId.keys.containsAll(request.mentionedUserIds)) {
            throw BaseException(ErrorCode.BAD_REQUEST)
        }
        val newMessage =
            ChatMessage(
                chatRoom = room,
                sender = participant.user,
                type = ChatMessageType.USER,
                content = request.content.trim(),
                replyTo = replyTo,
            ).also { message ->
                message.mention(request.mentionedUserIds.map { checkNotNull(participantsByUserId[it]).user })
            }
        val message = messageRepository.saveAndFlush(newMessage)
        participant.readThrough(message.id)
        notificationService.notifyMessage(message)
        return message.toResponse(userId).also { realtimeMessagingService.sendChatMessage(room.id, it) }
    }

    @Transactional
    fun shareImage(
        userId: Long,
        roomId: Long,
        image: MultipartFile,
        caption: String?,
    ): ChatMessageResponse {
        if (image.isEmpty || image.size > MAX_CHAT_IMAGE_BYTES || image.contentType?.startsWith("image/") != true) {
            throw BaseException(ErrorCode.BAD_REQUEST)
        }
        requireCanShareMessage(userId, roomId)
        val imageUrl = uploadChatImage(image)
        return saveSharedMessage(
            userId,
            roomId,
            ChatMessageType.IMAGE,
            caption?.trim()?.takeIf(String::isNotEmpty) ?: "사진",
            imageUrl = imageUrl,
        )
    }

    @Transactional
    fun shareTourismContent(
        userId: Long,
        roomId: Long,
        request: ShareTourismContentRequest,
    ): ChatMessageResponse {
        requireCanShareMessage(userId, roomId)
        val content =
            tourismContentRepository.findByContentId(request.contentId)
                ?: throw BaseException(ErrorCode.TOURISM_CONTENT_NOT_FOUND)
        return saveSharedMessage(
            userId,
            roomId,
            ChatMessageType.TOURISM_CONTENT,
            content.title,
            tourismContent = content,
        )
    }

    @Transactional
    fun shareLocation(
        userId: Long,
        roomId: Long,
    ): ChatMessageResponse {
        val room = findParticipant(roomId, userId).chatRoom
        requireChatEnabled(room)
        val latitude = room.meetingLatitude ?: throw BaseException(ErrorCode.BAD_REQUEST)
        val longitude = room.meetingLongitude ?: throw BaseException(ErrorCode.BAD_REQUEST)
        val locationName = room.meetingDetails?.trim()?.takeIf(String::isNotEmpty)
        return saveSharedMessage(
            userId,
            roomId,
            ChatMessageType.LOCATION,
            locationName ?: "만날 위치",
            sharedLatitude = latitude,
            sharedLongitude = longitude,
            locationName = locationName,
        )
    }

    @Transactional
    fun createPoll(
        userId: Long,
        roomId: Long,
        request: CreateChatPollRequest,
    ): ChatMessageResponse {
        val normalizedOptions = request.options.map(String::trim)
        if (normalizedOptions.distinct().size != normalizedOptions.size) throw BaseException(ErrorCode.BAD_REQUEST)
        val message =
            saveSharedMessageEntity(
                userId,
                roomId,
                ChatMessageType.POLL,
                request.question.trim(),
                pollAnonymous = request.anonymous,
            )
        pollOptionRepository.saveAllAndFlush(
            normalizedOptions.mapIndexed { index, option ->
                ChatPollOption(message = message, text = option, sequence = index + 1)
            },
        )
        return message.toResponse(userId).also { realtimeMessagingService.sendChatMessage(roomId, it) }
    }

    @Transactional
    fun votePoll(
        userId: Long,
        roomId: Long,
        messageId: Long,
        optionId: Long,
    ): ChatMessageResponse {
        findRoomForUpdate(roomId)
        val participant = findParticipant(roomId, userId)
        requireChatEnabled(participant.chatRoom)
        val message = findPollMessage(roomId, messageId)
        val option = pollOptionRepository.findByIdAndMessageId(optionId, messageId) ?: throw BaseException(ErrorCode.RESOURCE_NOT_FOUND)
        val existingVote = pollVoteRepository.findByMessageIdAndUserId(messageId, userId)
        when {
            existingVote == null ->
                pollVoteRepository.saveAndFlush(ChatPollVote(message = message, option = option, user = participant.user))

            existingVote.option.id != option.id -> {
                existingVote.changeOption(option)
                pollVoteRepository.flush()
            }
        }
        return message.toResponse(userId).also {
            realtimeMessagingService.sendChatPollUpdated(roomId, message.toPollUpdatedResponse())
        }
    }

    @Transactional
    fun cancelPollVote(
        userId: Long,
        roomId: Long,
        messageId: Long,
    ): ChatMessageResponse {
        findRoomForUpdate(roomId)
        val participant = findParticipant(roomId, userId)
        requireChatEnabled(participant.chatRoom)
        val message = findPollMessage(roomId, messageId)
        pollVoteRepository.findByMessageIdAndUserId(messageId, userId)?.let {
            pollVoteRepository.delete(it)
            pollVoteRepository.flush()
        }
        return message.toResponse(userId).also {
            realtimeMessagingService.sendChatPollUpdated(roomId, message.toPollUpdatedResponse())
        }
    }

    @Transactional
    fun shareSettlementMemo(
        userId: Long,
        roomId: Long,
        request: CreateSettlementMemoRequest,
    ): ChatMessageResponse = saveSharedMessage(userId, roomId, ChatMessageType.SETTLEMENT_MEMO, request.memo.trim())

    @Transactional
    fun getMessages(
        userId: Long,
        roomId: Long,
        beforeMessageId: Long?,
        limit: Int,
    ): ChatMessagePageResponse {
        findRoom(roomId)
        val participant = findParticipant(roomId, userId)
        val pageSize = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, pageSize + 1)
        val fetchedMessages =
            beforeMessageId?.let {
                messageRepository.findAllByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, it, pageable)
            } ?: messageRepository.findAllByChatRoomIdOrderByIdDesc(roomId, pageable)
        val hasNext = fetchedMessages.size > pageSize
        val messagesDescending = fetchedMessages.take(pageSize)
        messagesDescending.firstOrNull()?.let { participant.readThrough(it.id) }
        return ChatMessagePageResponse(
            messages = messagesDescending.asReversed().map { it.toResponse(userId) },
            nextId = messagesDescending.lastOrNull()?.id?.takeIf { hasNext },
            hasNext = hasNext,
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentRoadmap(
        userId: Long,
        roomId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): CurrentTravelRoadmapResponse {
        val room = findParticipant(roomId, userId).chatRoom
        val today = now.toLocalDate()
        val lastTravelDate = room.endDate ?: room.startDate
        if (room.status != ChatRoomStatus.CONFIRMED || today < room.startDate || today > lastTravelDate) {
            return CurrentTravelRoadmapResponse(
                active = false,
                dayNumber = null,
                totalDays = room.tripDays,
                currentPlace = null,
                nextPlace = null,
                places = emptyList(),
            )
        }

        val dayNumber = ChronoUnit.DAYS.between(room.startDate, today).toInt() + 1
        val scheduledPlaces =
            room.course.places
                .filter { it.dayNumber == dayNumber }
                .sortedBy { it.sequence }
                .map { place -> place to place.visitTime?.let(today::atTime) }
        val current =
            scheduledPlaces
                .filter { (_, scheduledAt) -> scheduledAt != null && !scheduledAt.isAfter(now) }
                .maxWithOrNull(compareBy<Pair<TravelCoursePlace, LocalDateTime?>> { it.second }.thenBy { it.first.sequence })
        val next =
            scheduledPlaces
                .filter { (_, scheduledAt) -> scheduledAt?.isAfter(now) == true }
                .minWithOrNull(compareBy<Pair<TravelCoursePlace, LocalDateTime?>> { it.second }.thenBy { it.first.sequence })
        val responses =
            scheduledPlaces.map { (place, scheduledAt) ->
                place.toRoadmapResponse(
                    scheduledAt = scheduledAt,
                    progress =
                        when {
                            place.sequence == current?.first?.sequence -> TravelRoadmapProgress.CURRENT
                            scheduledAt != null && !scheduledAt.isAfter(now) -> TravelRoadmapProgress.COMPLETED
                            else -> TravelRoadmapProgress.UPCOMING
                        },
                )
            }
        return CurrentTravelRoadmapResponse(
            active = true,
            dayNumber = dayNumber,
            totalDays = room.tripDays,
            currentPlace = responses.firstOrNull { it.sequence == current?.first?.sequence },
            nextPlace = responses.firstOrNull { it.sequence == next?.first?.sequence },
            places = responses,
        )
    }

    private fun saveSharedMessage(
        userId: Long,
        roomId: Long,
        type: ChatMessageType,
        content: String,
        imageUrl: String? = null,
        tourismContent: TourismContent? = null,
        sharedLatitude: Double? = null,
        sharedLongitude: Double? = null,
        locationName: String? = null,
    ): ChatMessageResponse {
        val message =
            saveSharedMessageEntity(
                userId = userId,
                roomId = roomId,
                type = type,
                content = content,
                imageUrl = imageUrl,
                tourismContent = tourismContent,
                sharedLatitude = sharedLatitude,
                sharedLongitude = sharedLongitude,
                locationName = locationName,
            )
        return message.toResponse(userId).also { realtimeMessagingService.sendChatMessage(roomId, it) }
    }

    private fun saveSharedMessageEntity(
        userId: Long,
        roomId: Long,
        type: ChatMessageType,
        content: String,
        imageUrl: String? = null,
        tourismContent: TourismContent? = null,
        sharedLatitude: Double? = null,
        sharedLongitude: Double? = null,
        locationName: String? = null,
        pollAnonymous: Boolean? = null,
    ): ChatMessage {
        val participant = findParticipant(roomId, userId)
        val room = participant.chatRoom
        requireChatEnabled(room)
        val message =
            messageRepository.saveAndFlush(
                ChatMessage(
                    chatRoom = room,
                    sender = participant.user,
                    type = type,
                    content = content,
                    imageUrl = imageUrl,
                    tourismContent = tourismContent,
                    sharedLatitude = sharedLatitude,
                    sharedLongitude = sharedLongitude,
                    locationName = locationName,
                    pollAnonymous = pollAnonymous,
                ),
            )
        participant.readThrough(message.id)
        notificationService.notifyMessage(message)
        return message
    }

    private fun findPollMessage(
        roomId: Long,
        messageId: Long,
    ): ChatMessage =
        messageRepository
            .findByIdAndChatRoomId(messageId, roomId)
            ?.takeIf { it.type == ChatMessageType.POLL }
            ?: throw BaseException(ErrorCode.RESOURCE_NOT_FOUND)

    private fun requireCanShareMessage(
        userId: Long,
        roomId: Long,
    ) {
        val participant = findParticipant(roomId, userId)
        requireChatEnabled(participant.chatRoom)
    }

    private fun TravelCoursePlace.toRoadmapResponse(
        scheduledAt: LocalDateTime?,
        progress: TravelRoadmapProgress,
    ): TravelRoadmapPlaceResponse =
        TravelRoadmapPlaceResponse(
            contentId = tourismContent.contentId,
            sequence = sequence,
            title = tourismContent.title,
            thumbnail = tourismContent.thumbnail,
            latitude = tourismContent.latitude,
            longitude = tourismContent.longitude,
            scheduledAt = scheduledAt,
            progress = progress,
        )

    private fun resolveCourse(
        host: User,
        request: CreateChatRoomRequest,
    ): TravelCourse =
        when (request.courseType) {
            TravelCourseType.PUBLIC -> {
                if (request.courseId == null || request.customCourse != null) {
                    throw BaseException(ErrorCode.INVALID_TRAVEL_COURSE_SELECTION)
                }
                courseRepository.findByIdAndType(request.courseId, TravelCourseType.PUBLIC)
                    ?: throw BaseException(ErrorCode.TRAVEL_COURSE_NOT_FOUND)
            }

            TravelCourseType.CUSTOM -> {
                if (request.courseId != null || request.customCourse == null) {
                    throw BaseException(ErrorCode.INVALID_TRAVEL_COURSE_SELECTION)
                }
                createCustomCourse(host, request, request.customCourse)
            }
        }

    private fun createCustomCourse(
        host: User,
        request: CreateChatRoomRequest,
        customCourse: CreateCustomCourseRequest,
    ): TravelCourse {
        validateCustomCourseSchedule(customCourse.places, request.totalTripDays())
        val course =
            courseRepository.saveAndFlush(
                TravelCourse(
                    type = TravelCourseType.CUSTOM,
                    owner = host,
                    title = customCourse.title.trim(),
                    description = customCourse.description?.trim()?.takeIf(String::isNotEmpty),
                    durationMinutes = request.dayTripDurationMinutes(),
                    tripNights = request.tripNights(),
                    tripDays = request.tripDays(),
                ),
            )
        customCourse.places.forEach { place ->
            val tourismContent =
                tourismContentRepository.findByContentId(place.contentId)
                    ?: throw BaseException(ErrorCode.TOURISM_CONTENT_NOT_FOUND)
            coursePlaceRepository.save(
                course.addCustomPlace(
                    tourismContent = tourismContent,
                    dayNumber = place.dayNumber,
                    sequence = place.sequence,
                    visitTime = place.visitTime,
                ),
            )
        }
        val tags = courseTagRepository.findAllById(customCourse.tagIds).toList()
        if (tags.isEmpty()) throw BaseException(ErrorCode.TRAVEL_COURSE_TAG_NOT_FOUND)
        course.addTags(tags)
        return course
    }

    private fun promoteFirstApprovedWaiter(room: ChatRoom): User? {
        if (room.status != ChatRoomStatus.RECRUITING) return null
        val application =
            applicationRepository.findFirstByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
                room.id,
                JoinApplicationStatus.WAITLISTED,
            ) ?: return null
        applicationRepository.delete(application)
        applicationRepository.flush()
        val participant =
            participantRepository.saveAndFlush(
                ChatRoomParticipant(chatRoom = room, user = application.user, role = ChatParticipantRole.MEMBER),
            )
        val participantCount = participantRepository.countByChatRoomId(room.id).toInt()
        val latestMessageId = recordParticipantJoined(room, application.user, participantCount)
        participant.readThrough(latestMessageId)
        return application.user
    }

    private fun recordParticipantJoined(
        room: ChatRoom,
        user: User,
        participantCount: Int,
    ): Long {
        var latest = saveSystemMessage(room, "${user.nickname()}님이 모임에 참여했어요.")
        if (room.maxParticipants - participantCount <= 1) {
            latest = saveSystemMessage(room, "모집 마감 임박입니다.")
        }
        return latest.id
    }

    private fun saveSystemMessage(
        room: ChatRoom,
        content: String,
    ): ChatMessage {
        val message =
            messageRepository.saveAndFlush(
                ChatMessage(chatRoom = room, type = ChatMessageType.SYSTEM, content = content),
            )
        realtimeMessagingService.sendChatMessage(
            room.id,
            ChatMessageResponse(
                messageId = message.id,
                type = ChatMessageType.SYSTEM,
                senderId = null,
                senderNickname = "시스템",
                content = content,
                createdAt = runCatching { message.createdDateTime }.getOrElse { LocalDateTime.now() },
            ),
        )
        return message
    }

    private fun ChatRoomParticipant.toMySummary(): MyChatRoomSummaryResponse {
        val room = chatRoom
        if (room.isChatArchived()) {
            return MyChatRoomSummaryResponse(
                roomId = room.id,
                courseId = room.course.id,
                title = room.roomTitle,
                description = room.description,
                startDate = room.startDate,
                endDate = room.endDate,
                chatAvailable = false,
                ended = true,
                coursePublicationAvailable =
                    room.host.id == user.id &&
                        room.course.type == TravelCourseType.CUSTOM,
            )
        }
        val latest =
            messageRepository.findFirstByChatRoomIdOrderByIdDesc(room.id)
                ?: throw BaseException(ErrorCode.CHAT_ROOM_NO_MESSAGES)
        return MyChatRoomSummaryResponse(
            roomId = room.id,
            courseId = room.course.id,
            title = room.roomTitle,
            description = room.description,
            startDate = room.startDate,
            endDate = room.endDate,
            chatAvailable = true,
            thumbnail = room.thumbnail,
            status = room.status,
            recruitmentDDay = room.recruitmentDDay(),
            ended = room.hasEnded(),
            coursePublicationAvailable =
                room.host.id == user.id &&
                    room.hasCompletedTrip() &&
                    room.course.type == TravelCourseType.CUSTOM,
            participantCount = participantRepository.countByChatRoomId(room.id).toInt(),
            maxParticipants = room.maxParticipants,
            unreadMessageCount = messageRepository.countByChatRoomIdAndIdGreaterThan(room.id, lastReadMessageId),
            latestMessage = latest.toLatestResponse(),
        )
    }

    private fun ChatRoom.toDetail(favorite: Boolean): ChatRoomDetailResponse {
        val participants = participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(id)
        return ChatRoomDetailResponse(
            roomId = id,
            title = roomTitle,
            description = description,
            thumbnail = thumbnail,
            tripType = tripType,
            startDate = startDate,
            endDate = endDate,
            recruitmentDeadlineDate = recruitmentDeadlineDate,
            tripNights = tripNights,
            tripDays = tripDays,
            dayTripStartTime = dayTripStartTime,
            dayTripEndTime = dayTripEndTime,
            meetingLatitude = meetingLatitude,
            meetingLongitude = meetingLongitude,
            meetingDetails = meetingDetails,
            meetingDateTime = meetingDateTime,
            participationFee = participationFee,
            genderRestriction = genderRestriction,
            minimumAge = minimumAge,
            maximumAge = maximumAge,
            joinApprovalMode = joinApprovalMode,
            recruitmentDDay = recruitmentDDay(),
            hostId = host.id,
            hostProfileImageUrl =
                host.information
                    ?.profileFileName
                    ?.let(objectStorageRepository::getDownloadUrl),
            participantCount = participants.size,
            maxParticipants = maxParticipants,
            status = status,
            favorite = favorite,
            latestPinnedNotice =
                noticeRepository
                    .findFirstByChatRoomIdAndPinnedTrueAndContentIsNotNullOrderByCreatedDateTimeDescIdDesc(id)
                    ?.toResponse(),
            participants =
                participants.map {
                    ChatParticipantResponse(
                        userId = it.user.id,
                        profileImageUrl =
                            it.user.information
                                ?.profileFileName
                                ?.let(objectStorageRepository::getDownloadUrl),
                    )
                },
        )
    }

    private fun ChatRoom.matches(
        filter: MyChatRoomFilter,
        today: LocalDate = LocalDate.now(),
    ): Boolean {
        val ended = hasEnded(today)
        return when (filter) {
            MyChatRoomFilter.ALL -> true
            MyChatRoomFilter.RECRUITING -> status == ChatRoomStatus.RECRUITING && !ended
            MyChatRoomFilter.CONFIRMED -> status == ChatRoomStatus.CONFIRMED && !ended
            MyChatRoomFilter.ENDED -> ended
        }
    }

    private fun ChatRoom.hasEnded(today: LocalDate = LocalDate.now()): Boolean =
        status == ChatRoomStatus.CANCELLED || (endDate ?: startDate).isBefore(today)

    private fun TravelCourse.toResponse(editable: Boolean) =
        TravelCourseResponse(
            id,
            title,
            type,
            editable,
            places.map {
                TravelCoursePlaceResponse(
                    contentId = it.tourismContent.contentId,
                    dayNumber = it.dayNumber,
                    sequence = it.sequence,
                    visitTime = it.visitTime,
                    title = it.tourismContent.title,
                    thumbnail = it.tourismContent.thumbnail,
                    latitude = it.tourismContent.latitude ?: 0.0,
                    longitude = it.tourismContent.longitude ?: 0.0,
                )
            },
        )

    private fun User.toApplicantProfile(): ApplicantProfileResponse {
        val info = information
        return ApplicantProfileResponse(
            id,
            nickname(),
            info?.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            info?.gender,
            info?.birthDate?.let { Period.between(it, LocalDate.now()).years },
            mannerRating,
            participantRepository.countCompletedTrips(id).toInt(),
        )
    }

    private fun ChatMessage.toResponse(viewerUserId: Long? = null): ChatMessageResponse {
        val sharedContent = tourismContent
        val poll =
            if (type == ChatMessageType.POLL) {
                val options = pollOptionRepository.findAllByMessageIdOrderBySequenceAsc(id)
                val votes = pollVoteRepository.findAllByMessageId(id)
                val votesByOption = votes.groupBy { it.option.id }
                ChatPollResponse(
                    question = content,
                    anonymous = pollAnonymous ?: true,
                    totalVoteCount = votes.size,
                    options =
                        options.map { option ->
                            val optionVotes = votesByOption[option.id].orEmpty()
                            ChatPollOptionResponse(
                                optionId = option.id,
                                text = option.text,
                                voteCount = optionVotes.size,
                                votedByMe = viewerUserId != null && optionVotes.any { it.user.id == viewerUserId },
                                voterNicknames =
                                    if (pollAnonymous == false) optionVotes.map { it.user.nickname() } else null,
                            )
                        },
                )
            } else {
                null
            }
        return ChatMessageResponse(
            messageId = id,
            type = type,
            senderId = sender?.id,
            senderNickname = sender?.nickname() ?: SYSTEM_NICKNAME,
            content = content,
            createdAt = createdDateTime,
            imageUrl = imageUrl,
            tourismContent =
                sharedContent?.let {
                    SharedTourismContentResponse(
                        contentId = it.contentId,
                        title = it.title,
                        address = listOfNotNull(it.address1, it.address2).joinToString(" ").takeIf(String::isNotEmpty),
                        thumbnail = it.thumbnail,
                        latitude = it.latitude,
                        longitude = it.longitude,
                    )
                },
            location =
                sharedLatitude?.let { latitude ->
                    SharedLocationResponse(latitude, requireNotNull(sharedLongitude), locationName)
                },
            poll = poll,
            replyTo =
                replyTo?.let {
                    RepliedChatMessageResponse(
                        messageId = it.id,
                        senderNickname = it.sender?.nickname() ?: SYSTEM_NICKNAME,
                        content = it.content,
                    )
                },
            mentions = mentionedUsers.sortedBy { it.id }.map { MentionedChatUserResponse(it.id, it.nickname()) },
        )
    }

    private fun ChatMessage.toPollUpdatedResponse(): ChatPollUpdatedResponse {
        val poll = toResponse().poll ?: throw BaseException(ErrorCode.RESOURCE_NOT_FOUND)
        return ChatPollUpdatedResponse(
            messageId = id,
            totalVoteCount = poll.totalVoteCount,
            options =
                poll.options.map {
                    ChatPollUpdatedOptionResponse(
                        optionId = it.optionId,
                        voteCount = it.voteCount,
                        voterNicknames = it.voterNicknames,
                    )
                },
        )
    }

    private fun ChatMessage.toLatestResponse() =
        LatestChatMessageResponse(
            type = type,
            senderNickname = sender?.nickname() ?: SYSTEM_NICKNAME,
            content = content,
            sentAt = createdDateTime,
        )

    private fun ChatRoomNotice.toResponse() =
        ChatRoomNoticeResponse(
            noticeId = id,
            content = content,
            pinned = pinned,
            authorNickname = author.nickname(),
            createdAt = createdDateTime,
        )

    private fun User.nickname() = information?.nickname ?: "사용자 $id"

    private fun uploadChatRoomThumbnail(file: MultipartFile): String {
        val extension = fileExtension(file)
        val key =
            objectStorageRepository.upload(
                CHAT_ROOM_THUMBNAIL_PATH,
                ObjectStorageRepository.generateFileName(extension),
                file.inputStream,
            )
        return objectStorageRepository.getDownloadUrl(key)
    }

    private fun uploadChatImage(file: MultipartFile): String {
        val extension = fileExtension(file)
        val key =
            objectStorageRepository.upload(
                CHAT_IMAGE_PATH,
                ObjectStorageRepository.generateFileName(extension),
                file.inputStream,
            )
        return objectStorageRepository.getDownloadUrl(key)
    }

    private fun fileExtension(file: MultipartFile): String =
        file.originalFilename
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.matches(FILE_EXTENSION_PATTERN) }
            ?: DEFAULT_IMAGE_EXTENSION

    private fun requireJoinConditions(
        room: ChatRoom,
        user: User,
    ) {
        if (!meetsJoinConditions(room, user)) {
            throw BaseException(ErrorCode.CHAT_ROOM_JOIN_CONDITION_NOT_MET)
        }
    }

    private fun meetsJoinConditions(
        room: ChatRoom,
        user: User,
    ): Boolean {
        val information = user.information
        val genderMatches =
            when (room.genderRestriction) {
                GenderRestriction.NONE -> true
                GenderRestriction.FEMALE_ONLY -> information?.gender == Gender.F
                GenderRestriction.MALE_ONLY -> information?.gender == Gender.M
            }
        if (!genderMatches) return false

        val minimumAge = room.minimumAge
        val maximumAge = room.maximumAge
        val ageMatches =
            if (minimumAge == null && maximumAge == null) {
                true
            } else {
                information
                    ?.birthDate
                    ?.let { Period.between(it, LocalDate.now()).years }
                    ?.let { age ->
                        (minimumAge == null || age >= minimumAge) &&
                            (maximumAge == null || age <= maximumAge)
                    } == true
            }
        return ageMatches
    }

    private fun requireParticipant(
        roomId: Long,
        userId: Long,
    ) {
        if (!participantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw BaseException(ErrorCode.CHAT_ROOM_NOT_PARTICIPANT)
        }
    }

    private fun findParticipant(
        roomId: Long,
        userId: Long,
    ): ChatRoomParticipant =
        participantRepository.findByChatRoomIdAndUserId(roomId, userId)
            ?: throw BaseException(ErrorCode.CHAT_ROOM_NOT_PARTICIPANT)

    private fun requireChatEnabled(room: ChatRoom) {
        if (!room.canChat()) throw BaseException(ErrorCode.CHAT_DISABLED)
    }

    private fun requireHost(
        room: ChatRoom,
        userId: Long,
    ) {
        if (room.host.id != userId) throw BaseException(ErrorCode.FORBIDDEN)
    }

    private fun findRoom(id: Long): ChatRoom {
        val room =
            roomRepository.findById(id).orElseThrow {
                BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND, ErrorCode.CHAT_ROOM_NOT_FOUND.errorMessage)
            }
        if (room.isChatArchived()) throw BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND)
        return room
    }

    private fun findRoomForUpdate(id: Long): ChatRoom {
        val room = roomRepository.findByIdForUpdate(id) ?: throw BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND)
        if (room.isChatArchived()) throw BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND)
        return room
    }

    private fun findUser(id: Long) = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }

    private fun findPublicCourse(courseId: Long): TravelCourse =
        courseRepository.findByIdAndType(courseId, TravelCourseType.PUBLIC)
            ?: throw BaseException(ErrorCode.TRAVEL_COURSE_NOT_FOUND)

    private fun validateCustomCourseSchedule(
        places: List<CustomCoursePlaceRequest>,
        tripDays: Int,
    ) {
        val placesByDay = places.groupBy { it.dayNumber }
        val includesEveryTripDay = placesByDay.keys == (1..tripDays).toSet()
        val hasAtLeastTwoPlacesEveryDay = placesByDay.values.all { it.size >= MIN_PLACES_PER_DAY }
        val hasUniqueSequencesEveryDay =
            placesByDay.values.all { dailyPlaces -> dailyPlaces.map { it.sequence }.distinct().size == dailyPlaces.size }
        if (!includesEveryTripDay || !hasAtLeastTwoPlacesEveryDay || !hasUniqueSequencesEveryDay) {
            throw BaseException(ErrorCode.INVALID_TRAVEL_COURSE_SCHEDULE)
        }
    }

    private fun TravelCourse.toInformationResponse(travelTime: String): TravelCourseInformationResponse =
        TravelCourseInformationResponse(
            courseId = id,
            title = title,
            description = description,
            type = type,
            travelTime = travelTime,
            distanceKm = totalDistanceKm(),
            averageRating = courseRatingRepository.findAverageByCourseId(id)?.rounded(1),
            ratingCount = courseRatingRepository.countByCourseId(id),
            tags = tags.sortedBy { it.id }.map { TravelCourseTagResponse(it.id, it.name) },
            thumbnail = places.firstOrNull()?.tourismContent?.thumbnail,
            places = toResponse(editable = false).places,
        )

    companion object {
        private const val SYSTEM_NICKNAME = "시스템"
        private const val POPULAR_COURSE_LIMIT = 3
        private const val MIN_PLACES_PER_DAY = 2
        private const val CHAT_ROOM_THUMBNAIL_PATH = "chat-room/thumbnail/"
        private const val CHAT_IMAGE_PATH = "chat/message/image/"
        private const val MAX_CHAT_IMAGE_BYTES = 20L * 1024 * 1024
        private const val MAX_DISCOVER_ROOM_LIMIT = 20
        private const val NO_USER_ID = -1L
        private const val DEFAULT_IMAGE_EXTENSION = "jpg"
        private val FILE_EXTENSION_PATTERN = Regex("[a-z0-9]{1,10}")
        private val ACTIVE_APPLICATION_STATUSES = listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED)
    }
}

private fun ChatRoom.toCourseRoomResponse() =
    TravelCourseRoomResponse(
        roomId = id,
        tripType = tripType,
        startDate = startDate,
        endDate = endDate,
        dayTripStartTime = dayTripStartTime,
        dayTripEndTime = dayTripEndTime,
    )

private fun TravelCourse.travelTimeText(): String =
    durationMinutes?.let(::formatDurationMinutes)
        ?: if (tripNights != null && tripDays != null) "${tripNights}박 ${tripDays}일" else "정보 없음"

private fun CreateChatRoomRequest.dayTripDurationMinutes(): Long? =
    if (endDate == null) Duration.between(requireNotNull(dayTripStartTime), requireNotNull(dayTripEndTime)).toMinutes() else null

private fun validateTripSchedule(request: CreateChatRoomRequest) {
    val valid =
        when (request.tripType) {
            TripType.DAY_TRIP ->
                request.endDate == null &&
                    request.dayTripStartTime != null &&
                    request.dayTripEndTime != null &&
                    request.dayTripStartTime < request.dayTripEndTime

            TripType.OVERNIGHT ->
                request.endDate?.isAfter(request.startDate) == true &&
                    request.dayTripStartTime == null &&
                    request.dayTripEndTime == null
        }
    if (!valid) throw BaseException(ErrorCode.INVALID_TRIP_SCHEDULE)
}

private fun validateAgeRestriction(request: CreateChatRoomRequest) {
    val minimumAge = request.minimumAge
    val maximumAge = request.maximumAge
    val valid = minimumAge == null || maximumAge == null || minimumAge <= maximumAge
    if (!valid) throw BaseException(ErrorCode.INVALID_CHAT_ROOM_AGE_RESTRICTION)
}

private fun CreateChatRoomRequest.tripDays(): Int? =
    endDate?.let {
        java.time.temporal.ChronoUnit.DAYS
            .between(startDate, it)
            .toInt() + 1
    }

private fun CreateChatRoomRequest.tripNights(): Int? = tripDays()?.minus(1)

private fun CreateChatRoomRequest.totalTripDays(): Int = tripDays() ?: 1

private fun ChatRoom.travelTimeText(): String =
    if (tripDays == 1) {
        val minutes = Duration.between(requireNotNull(dayTripStartTime), requireNotNull(dayTripEndTime)).toMinutes()
        formatDurationMinutes(minutes)
    } else {
        "${tripNights}박 ${tripDays}일"
    }

private fun formatDurationMinutes(minutes: Long): String = "${minutes / 60}시간 ${minutes % 60}분".replace(" 0분", "")

private fun TravelCourse.totalDistanceKm(): Double =
    places
        .zipWithNext()
        .sumOf { (from, to) ->
            val fromLatitude = from.tourismContent.latitude ?: return@sumOf 0.0
            val fromLongitude = from.tourismContent.longitude ?: return@sumOf 0.0
            val toLatitude = to.tourismContent.latitude ?: return@sumOf 0.0
            val toLongitude = to.tourismContent.longitude ?: return@sumOf 0.0
            haversineKm(fromLatitude, fromLongitude, toLatitude, toLongitude)
        }.rounded(1)

private fun haversineKm(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double,
): Double {
    val latitudeDistance = Math.toRadians(latitude2 - latitude1)
    val longitudeDistance = Math.toRadians(longitude2 - longitude1)
    val value =
        sin(latitudeDistance / 2).pow(2) +
            cos(Math.toRadians(latitude1)) * cos(Math.toRadians(latitude2)) * sin(longitudeDistance / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(value))
}

private fun Double.rounded(scale: Int): Double {
    val factor = 10.0.pow(scale)
    return round(this * factor) / factor
}

private const val EARTH_RADIUS_KM = 6371.0088
