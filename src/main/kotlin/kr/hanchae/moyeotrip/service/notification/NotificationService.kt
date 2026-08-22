package kr.hanchae.moyeotrip.service.notification

import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomKickHistoryResponse
import kr.hanchae.moyeotrip.controller.notification.response.ChatRoomNotificationSettingResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationPageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationSettingResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomKickHistory
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.notification.ChatRoomNotificationSetting
import kr.hanchae.moyeotrip.entity.notification.Notification
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomKickHistoryRepository
import kr.hanchae.moyeotrip.repository.ChatRoomNotificationSettingRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.NotificationRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val kickHistoryRepository: ChatRoomKickHistoryRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val settingRepository: NotificationSettingRepository,
    private val roomSettingRepository: ChatRoomNotificationSettingRepository,
    private val userRepository: UserRepository,
    private val realtimeMessagingService: RealtimeMessagingService,
) {
    @Transactional(readOnly = true)
    fun getNotifications(
        userId: Long,
        lastId: Long?,
        size: Int,
        unreadOnly: Boolean,
    ): NotificationPageResponse {
        val pageable = PageRequest.of(0, size + 1)
        val beforeId = lastId ?: Long.MAX_VALUE
        val fetched =
            if (unreadOnly) {
                repository.findAllByRecipientIdAndReadDateTimeIsNullAndIdLessThanOrderByIdDesc(userId, beforeId, pageable)
            } else {
                repository.findAllByRecipientIdAndIdLessThanOrderByIdDesc(userId, beforeId, pageable)
            }
        val hasNext = fetched.size > size
        val notifications = fetched.take(size)
        return NotificationPageResponse(
            notifications = notifications.map(Notification::toResponse),
            nextLastId = notifications.lastOrNull()?.id?.takeIf { hasNext },
            hasNext = hasNext,
            unreadCount = repository.countByRecipientIdAndReadDateTimeIsNull(userId),
        )
    }

    @Transactional
    fun markRead(
        userId: Long,
        notificationId: Long,
    ) {
        val notification =
            repository.findByIdAndRecipientId(notificationId, userId)
                ?: throw BaseException(ErrorCode.NOTIFICATION_NOT_FOUND)
        notification.markRead()
    }

    @Transactional
    fun markAllRead(userId: Long) {
        repository.findAllByRecipientIdAndReadDateTimeIsNull(userId).forEach(Notification::markRead)
    }

    @Transactional(readOnly = true)
    fun getKickHistory(
        userId: Long,
        notificationId: Long,
    ): ChatRoomKickHistoryResponse {
        val notification =
            repository.findByIdAndRecipientId(notificationId, userId)
                ?: throw BaseException(ErrorCode.NOTIFICATION_NOT_FOUND)
        if (notification.type != NotificationType.CHAT_ROOM_KICKED) {
            throw BaseException(ErrorCode.BAD_REQUEST)
        }
        return kickHistoryRepository
            .findByIdAndKickedUserId(notification.referenceId, userId)
            ?.toResponse()
            ?: throw BaseException(ErrorCode.NOTIFICATION_NOT_FOUND)
    }

    @Transactional
    fun getSetting(userId: Long): NotificationSettingResponse = findOrCreateSetting(userId).toResponse()

    @Transactional(readOnly = true)
    fun getChatRoomSetting(
        userId: Long,
        roomId: Long,
    ): ChatRoomNotificationSettingResponse {
        requireParticipant(userId, roomId)
        return ChatRoomNotificationSettingResponse(
            roomId = roomId,
            enabled = roomSettingRepository.findByUserIdAndChatRoomId(userId, roomId)?.enabled ?: true,
        )
    }

    @Transactional
    fun updateChatRoomSetting(
        userId: Long,
        roomId: Long,
        enabled: Boolean,
    ): ChatRoomNotificationSettingResponse {
        val participant = requireParticipant(userId, roomId)
        val setting =
            roomSettingRepository
                .findByUserIdAndChatRoomId(userId, roomId)
                ?.also { it.update(enabled) }
                ?: roomSettingRepository.save(
                    ChatRoomNotificationSetting(user = participant.user, chatRoom = participant.chatRoom, enabled = enabled),
                )
        return ChatRoomNotificationSettingResponse(roomId, setting.enabled)
    }

    @Transactional
    fun updateSetting(
        userId: Long,
        chatNotificationMode: ChatNotificationMode,
        recruitmentDeadlineEnabled: Boolean,
        socialActivityEnabled: Boolean,
        marketingEnabled: Boolean,
        doNotDisturbEnabled: Boolean,
        doNotDisturbStartTime: LocalTime?,
        doNotDisturbEndTime: LocalTime?,
        doNotDisturbDays: Set<DayOfWeek>,
    ): NotificationSettingResponse {
        if (doNotDisturbEnabled &&
            (
                doNotDisturbStartTime == null ||
                    doNotDisturbEndTime == null ||
                    doNotDisturbStartTime == doNotDisturbEndTime ||
                    doNotDisturbDays.isEmpty()
            )
        ) {
            throw BaseException(ErrorCode.BAD_REQUEST)
        }
        val setting = findOrCreateSetting(userId)
        setting.update(
            chatNotificationMode,
            recruitmentDeadlineEnabled,
            socialActivityEnabled,
            marketingEnabled,
            doNotDisturbEnabled,
            doNotDisturbStartTime,
            doNotDisturbEndTime,
            doNotDisturbDays,
        )
        return setting.toResponse()
    }

    fun notifyRoomCreated(room: ChatRoom) {
        save(room.host, NotificationType.CHAT_ROOM_CREATED, "${room.roomTitle} 모임이 만들어졌어요 ✨", room.id, room.id)
    }

    fun notifyChatRoomMemberKicked(kickHistory: ChatRoomKickHistory) {
        save(
            recipient = kickHistory.kickedUser,
            type = NotificationType.CHAT_ROOM_KICKED,
            content = "${kickHistory.roomTitle} 모임에서 강퇴되었어요.",
            chatRoomId = kickHistory.chatRoomId,
            referenceId = kickHistory.id,
        )
    }

    fun notifyMessage(message: ChatMessage) {
        val sender = message.sender ?: return
        participantRepository
            .findAllByChatRoomIdOrderByCreatedDateTimeAsc(message.chatRoom.id)
            .asSequence()
            .map { it.user }
            .filter { it.id != sender.id }
            .filter { recipient -> roomSettingRepository.findByUserIdAndChatRoomId(recipient.id, message.chatRoom.id)?.enabled != false }
            .filter { recipient -> allowsChatMessage(recipient, message) }
            .forEach { recipient ->
                save(
                    recipient,
                    NotificationType.CHAT_MESSAGE_RECEIVED,
                    "${sender.information?.nickname ?: "사용자 ${sender.id}"}님이 메시지를 보냈어요",
                    message.chatRoom.id,
                    message.id,
                )
            }
    }

    fun notifyCourseUpdated(
        room: ChatRoom,
        referenceId: Long,
    ) {
        participantRepository
            .findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
            .asSequence()
            .filter { it.user.id != room.host.id }
            .forEach { participant ->
                save(
                    participant.user,
                    NotificationType.TRAVEL_COURSE_UPDATED,
                    "${room.roomTitle} 여행 코스가 변경되었어요.",
                    room.id,
                    referenceId,
                )
            }
    }

    fun notifyMeetingInfoUpdated(
        room: ChatRoom,
        referenceId: Long,
    ) {
        participantRepository
            .findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
            .asSequence()
            .filter { it.user.id != room.host.id }
            .forEach { participant ->
                save(
                    participant.user,
                    NotificationType.MEETING_INFO_UPDATED,
                    "${room.roomTitle} 집합 정보가 변경되었어요.",
                    room.id,
                    referenceId,
                )
            }
    }

    fun notifyRecruitmentDeadline(room: ChatRoom) {
        val participantCount = participantRepository.countByChatRoomId(room.id)
        val dDay =
            java.time.temporal.ChronoUnit.DAYS
                .between(java.time.LocalDate.now(), room.recruitmentDeadlineDate)
        participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id).forEach { participant ->
            save(
                participant.user,
                NotificationType.RECRUITMENT_DEADLINE,
                "마감 D-$dDay · 현재 $participantCount/${room.maxParticipants}명이에요",
                room.id,
                room.id * DEADLINE_REFERENCE_MULTIPLIER +
                    java.time.LocalDate
                        .now()
                        .toEpochDay(),
            )
        }
    }

    fun notifyFeedLiked(
        feed: Feed,
        likedBy: User,
    ) {
        if (feed.author.id == likedBy.id) return
        val nickname = likedBy.information?.nickname ?: "사용자 ${likedBy.id}"
        save(
            recipient = feed.author,
            type = NotificationType.FEED_LIKE,
            content = "$nickname 님이 내 피드를 좋아해요.",
            chatRoomId = feed.chatRoom.id,
            referenceId = feed.id,
        )
    }

    fun notifyFriendRequested(request: FriendRequest) {
        val nickname = request.requester.information?.nickname ?: "사용자 ${request.requester.id}"
        save(
            recipient = request.receiver,
            type = NotificationType.FRIEND_REQUEST,
            content = "$nickname 님이 친구 신청을 보냈어요.",
            chatRoomId = null,
            referenceId = request.id,
        )
    }

    fun notifyFriendAccepted(
        friendship: Friendship,
        acceptedBy: User,
    ) {
        val requester = friendship.friendOf(acceptedBy.id)
        val nickname = acceptedBy.information?.nickname ?: "사용자 ${acceptedBy.id}"
        save(
            recipient = requester,
            type = NotificationType.FRIEND_ACCEPTED,
            content = "$nickname 님과 친구가 되었어요.",
            chatRoomId = null,
            referenceId = friendship.id,
        )
    }

    private fun save(
        recipient: User,
        type: NotificationType,
        content: String,
        chatRoomId: Long?,
        referenceId: Long,
    ) {
        if (!allows(recipient, type)) return
        if (repository.existsByRecipientIdAndTypeAndReferenceId(recipient.id, type, referenceId)) return
        val notification =
            repository.save(
                Notification(
                    recipient = recipient,
                    type = type,
                    content = content,
                    chatRoomId = chatRoomId,
                    referenceId = referenceId,
                ),
            )
        if (!isDoNotDisturbing(recipient)) {
            realtimeMessagingService.sendNotification(recipient.id, notification.toResponse())
        }
    }

    private fun allows(
        recipient: User,
        type: NotificationType,
    ): Boolean = settingRepository.findByUserId(recipient.id)?.allows(type) ?: true

    private fun isDoNotDisturbing(recipient: User): Boolean =
        settingRepository.findByUserId(recipient.id)?.isDoNotDisturbing(LocalDateTime.now(SERVICE_ZONE_ID)) ?: false

    private fun allowsChatMessage(
        recipient: User,
        message: ChatMessage,
    ): Boolean =
        when (settingRepository.findByUserId(recipient.id)?.chatNotificationMode ?: ChatNotificationMode.ALL) {
            ChatNotificationMode.ALL -> true
            ChatNotificationMode.NONE -> false
            ChatNotificationMode.MENTIONS_AND_REPLIES ->
                message.replyTo?.sender?.id == recipient.id ||
                    message.mentionedUsers.any { it.id == recipient.id }
        }

    private fun findOrCreateSetting(userId: Long): NotificationSetting =
        settingRepository.findByUserId(userId)
            ?: settingRepository.save(
                NotificationSetting(
                    user = userRepository.findById(userId).orElseThrow { BaseException(ErrorCode.USER_NOT_FOUND) },
                ),
            )

    private fun requireParticipant(
        userId: Long,
        roomId: Long,
    ) = participantRepository.findByChatRoomIdAndUserId(roomId, userId)
        ?: throw BaseException(ErrorCode.CHAT_ROOM_NOT_PARTICIPANT)

    companion object {
        private const val DEADLINE_REFERENCE_MULTIPLIER = 100_000L
        private val SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul")
    }
}

private fun NotificationSetting.toResponse() =
    NotificationSettingResponse(
        doNotDisturbEnabled = doNotDisturbEnabled,
        doNotDisturbStartTime = doNotDisturbStartTime,
        doNotDisturbEndTime = doNotDisturbEndTime,
        doNotDisturbDays = doNotDisturbDays,
    )

private fun Notification.toResponse() =
    NotificationResponse(
        notificationId = id,
        type = type,
        content = content,
        chatRoomId = chatRoomId,
        referenceId = referenceId,
        read = readDateTime != null,
        createdAt = createdDateTime,
    )

private fun ChatRoomKickHistory.toResponse() =
    ChatRoomKickHistoryResponse(
        kickHistoryId = id,
        roomId = chatRoomId,
        roomTitle = roomTitle,
        reason = reason,
        kickedAt = createdDateTime,
    )
