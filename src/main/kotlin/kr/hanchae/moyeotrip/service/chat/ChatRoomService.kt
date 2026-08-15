package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.response.ApplicantProfileResponse
import kr.hanchae.moyeotrip.controller.chat.response.ApprovalResult
import kr.hanchae.moyeotrip.controller.chat.response.ApproveJoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessagePageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatParticipantResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomMyState
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomNoticeResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinResult
import kr.hanchae.moyeotrip.controller.chat.response.LatestChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveResult
import kr.hanchae.moyeotrip.controller.chat.response.MyChatRoomSummaryResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyWaitingChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCoursePlaceResponse
import kr.hanchae.moyeotrip.controller.chat.response.TravelCourseResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomJoinApplication
import kr.hanchae.moyeotrip.entity.chat.ChatRoomNotice
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomJoinApplicationRepository
import kr.hanchae.moyeotrip.repository.ChatRoomNoticeRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TravelCoursePlaceRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

@Service
class ChatRoomService(
    private val roomRepository: ChatRoomRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val applicationRepository: ChatRoomJoinApplicationRepository,
    private val messageRepository: ChatMessageRepository,
    private val courseRepository: TravelCourseRepository,
    private val coursePlaceRepository: TravelCoursePlaceRepository,
    private val tourismContentRepository: TourismContentRepository,
    private val userRepository: UserRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val noticeRepository: ChatRoomNoticeRepository,
    private val notificationService: NotificationService,
) {
    @Transactional
    fun createRoom(
        userId: Long,
        request: CreateChatRoomRequest,
    ) {
        val host = findUser(userId)
        val course = resolveCourse(host, request)
        val room =
            roomRepository.saveAndFlush(
                ChatRoom(
                    host = host,
                    course = course,
                    roomTitle = request.title.trim(),
                    description = request.description?.trim()?.takeIf(String::isNotEmpty),
                    maxParticipants = request.maxParticipants,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    recruitmentDeadlineDate = request.recruitmentDeadlineDate,
                    dayTripStartTime = request.dayTripStartTime,
                    dayTripEndTime = request.dayTripEndTime,
                    meetingLatitude = request.meetingLatitude,
                    meetingLongitude = request.meetingLongitude,
                    meetingDateTime = request.meetingDateTime,
                    participationFee = request.participationFee,
                ),
            )
        val hostParticipant =
            participantRepository.saveAndFlush(ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST))
        val openingMessage = saveSystemMessage(room, "${host.nickname()}님이 모임을 개설했어요.")
        hostParticipant.readThrough(openingMessage.id)
        notificationService.notifyRoomCreated(room)
    }

    @Transactional(readOnly = true)
    fun getRoom(
        userId: Long,
        roomId: Long,
    ): ChatRoomDetailResponse = findParticipant(roomId, userId).chatRoom.toDetail(userId)

    @Transactional(readOnly = true)
    fun getMyRooms(userId: Long): List<MyChatRoomSummaryResponse> =
        participantRepository
            .findAllByUserId(userId)
            .map { it.toMySummary() }
            .sortedByDescending { it.latestMessage.sentAt }

    @Transactional(readOnly = true)
    fun getMyWaitingRooms(userId: Long): List<MyWaitingChatRoomResponse> =
        applicationRepository
            .findAllByUserIdAndStatusInOrderByCreatedDateTimeDesc(
                userId,
                listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED, JoinApplicationStatus.REJECTED),
            ).map { application ->
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
                    startDate = room.startDate,
                    dDay = room.dDay(),
                    roomStatus = room.status,
                    applicationStatus = application.status,
                    waitlistPosition = waitlistPosition,
                )
            }

    @Transactional(readOnly = true)
    fun getManagedCourses(): List<TravelCourseResponse> =
        courseRepository
            .findAllByTypeOrderByCreatedDateTimeDesc(TravelCourseType.MANAGED)
            .map { it.toResponse(editable = false) }

    @Transactional(readOnly = true)
    fun getPopularManagedCourses(): List<TravelCourseResponse> =
        courseRepository
            .findPopularManagedCourses(PageRequest.of(0, POPULAR_COURSE_LIMIT))
            .map { it.toResponse(editable = false) }

    @Transactional(readOnly = true)
    fun getRoomCourse(
        userId: Long,
        roomId: Long,
    ): TravelCourseResponse {
        val room = findRoom(roomId)
        requireParticipant(roomId, userId)
        return room.course.toResponse(editable = room.course.type == TravelCourseType.CUSTOM && room.course.owner?.id == userId)
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
        applicationRepository.save(
            ChatRoomJoinApplication(
                chatRoom = room,
                user = findUser(userId),
                applicationMessage = request.applicationMessage.trim(),
            ),
        )
        return JoinChatRoomResponse(roomId, JoinResult.PENDING_APPROVAL)
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

    @Transactional
    fun kickMember(
        hostId: Long,
        roomId: Long,
        memberId: Long,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        requireChatEnabled(room)
        val participant =
            participantRepository
                .findByChatRoomIdAndUserId(roomId, memberId)
                ?.takeIf { it.role == ChatParticipantRole.MEMBER }
                ?: throw BaseException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND)
        participantRepository.delete(participant)
        participantRepository.flush()
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
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        requireChatEnabled(room)
        val content = notice.trim().takeIf(String::isNotEmpty) ?: throw BaseException(ErrorCode.BAD_REQUEST)
        noticeRepository.save(ChatRoomNotice(chatRoom = room, author = room.host, content = content))
        saveSystemMessage(room, "공지가 등록되었어요.\n$content")
    }

    @Transactional
    fun updateNotice(
        hostId: Long,
        roomId: Long,
        noticeId: Long,
        notice: String?,
    ) {
        val room = findRoomForUpdate(roomId)
        requireHost(room, hostId)
        requireChatEnabled(room)
        val normalizedNotice = notice?.trim()?.takeIf(String::isNotEmpty)
        val target =
            noticeRepository.findByIdAndChatRoomId(noticeId, roomId)
                ?: throw BaseException(ErrorCode.CHAT_ROOM_NOTICE_NOT_FOUND)
        if (normalizedNotice == null) {
            noticeRepository.delete(target)
        } else {
            target.updateContent(normalizedNotice)
            saveSystemMessage(room, "공지가 수정되었어요.\n$normalizedNotice")
        }
    }

    @Transactional(readOnly = true)
    fun getNoticeHistory(
        userId: Long,
        roomId: Long,
    ): List<ChatRoomNoticeResponse> {
        requireParticipant(roomId, userId)
        return noticeRepository.findAllByChatRoomIdOrderByIdDesc(roomId).map { it.toResponse() }
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
        val message =
            messageRepository
                .saveAndFlush(
                    ChatMessage(
                        chatRoom = room,
                        sender = findUser(userId),
                        type = ChatMessageType.USER,
                        content = request.content.trim(),
                    ),
                )
        participant.readThrough(message.id)
        notificationService.notifyMessage(message)
        return message.toResponse()
    }

    @Transactional
    fun getMessages(
        userId: Long,
        roomId: Long,
        beforeMessageId: Long?,
        limit: Int,
    ): ChatMessagePageResponse {
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
            messages = messagesDescending.asReversed().map { it.toResponse() },
            nextCursor = messagesDescending.lastOrNull()?.id?.takeIf { hasNext },
            hasNext = hasNext,
        )
    }

    private fun resolveCourse(
        host: User,
        request: CreateChatRoomRequest,
    ): TravelCourse {
        val hasManaged = request.managedCourseId != null
        val hasCustom = request.customCourseTitle.isNotBlank() && request.customPlaces.isNotEmpty()
        if (hasManaged == hasCustom) throw BaseException(ErrorCode.INVALID_TRAVEL_COURSE_SELECTION)
        request.managedCourseId?.let { managedCourseId ->
            return courseRepository.findByIdAndType(managedCourseId, TravelCourseType.MANAGED)
                ?: throw BaseException(ErrorCode.TRAVEL_COURSE_NOT_FOUND)
        }
        val course =
            courseRepository.saveAndFlush(
                TravelCourse(type = TravelCourseType.CUSTOM, owner = host, title = request.customCourseTitle.trim()),
            )
        check(
            request.customPlaces
                .map { it.sequence }
                .distinct()
                .size == request.customPlaces.size,
        ) {
            "코스 장소의 순서는 중복될 수 없습니다."
        }
        request.customPlaces.forEach { place ->
            val tourismContent =
                tourismContentRepository.findByContentId(place.contentId)
                    ?: throw BaseException(ErrorCode.TOURISM_CONTENT_NOT_FOUND)
            coursePlaceRepository.save(
                course.addCustomPlace(tourismContent, place.sequence),
            )
        }
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
    ): ChatMessage =
        messageRepository.saveAndFlush(
            ChatMessage(chatRoom = room, type = ChatMessageType.SYSTEM, content = content),
        )

    private fun ChatRoomParticipant.toMySummary(): MyChatRoomSummaryResponse {
        val room = chatRoom
        val latest =
            messageRepository.findFirstByChatRoomIdOrderByIdDesc(room.id)
                ?: throw BaseException(ErrorCode.CHAT_ROOM_NO_MESSAGES)
        return MyChatRoomSummaryResponse(
            roomId = room.id,
            title = room.roomTitle,
            status = room.status,
            dDay = room.dDay(),
            unreadMessageCount = messageRepository.countByChatRoomIdAndIdGreaterThan(room.id, lastReadMessageId),
            latestMessage = latest.toLatestResponse(),
        )
    }

    private fun ChatRoom.toDetail(userId: Long): ChatRoomDetailResponse {
        val participants = participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(id)
        val approvedWaitlistCount =
            if (host.id == userId) {
                applicationRepository.countByChatRoomIdAndStatus(id, JoinApplicationStatus.WAITLISTED).toInt()
            } else {
                null
            }
        val pendingApplicationCount =
            if (host.id == userId) {
                applicationRepository.countByChatRoomIdAndStatus(id, JoinApplicationStatus.PENDING).toInt()
            } else {
                null
            }
        return ChatRoomDetailResponse(
            id,
            roomTitle,
            description,
            noticeRepository.findFirstByChatRoomIdAndContentIsNotNullOrderByIdDesc(id)?.toResponse(),
            startDate,
            endDate,
            recruitmentDeadlineDate,
            tripNights,
            tripDays,
            dayTripStartTime,
            dayTripEndTime,
            meetingLatitude,
            meetingLongitude,
            meetingDateTime,
            participationFee,
            dDay(),
            host.id,
            participants.size,
            maxParticipants,
            approvedWaitlistCount,
            pendingApplicationCount,
            status,
            participants.map { ChatParticipantResponse(it.user.id, it.user.nickname(), it.role) },
            ChatRoomMyState.PARTICIPANT,
            null,
        )
    }

    private fun TravelCourse.toResponse(editable: Boolean) =
        TravelCourseResponse(
            id,
            title,
            type,
            editable,
            places.map {
                TravelCoursePlaceResponse(
                    it.sequence,
                    it.tourismContent.contentId,
                    it.tourismContent.title,
                    it.tourismContent.firstThumbnailUrl,
                    it.tourismContent.latitude ?: 0.0,
                    it.tourismContent.longitude ?: 0.0,
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

    private fun ChatMessage.toResponse() =
        ChatMessageResponse(
            messageId = id,
            type = type,
            senderId = sender?.id,
            senderNickname = sender?.nickname() ?: SYSTEM_NICKNAME,
            content = content,
            createdAt = createdDateTime,
        )

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
            authorNickname = author.nickname(),
            createdAt = createdDateTime,
        )

    private fun User.nickname() = information?.nickname ?: "사용자 $id"

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

    private fun findRoom(id: Long) =
        roomRepository.findById(id).orElseThrow {
            BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND, ErrorCode.CHAT_ROOM_NOT_FOUND.errorMessage)
        }

    private fun findRoomForUpdate(id: Long) =
        roomRepository.findByIdForUpdate(id)
            ?: throw BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND)

    private fun findUser(id: Long) = userRepository.findById(id).orElseThrow { UserNotFoundException(id) }

    companion object {
        private const val SYSTEM_NICKNAME = "시스템"
        private const val POPULAR_COURSE_LIMIT = 3
        private val ACTIVE_APPLICATION_STATUSES = listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED)
    }
}
