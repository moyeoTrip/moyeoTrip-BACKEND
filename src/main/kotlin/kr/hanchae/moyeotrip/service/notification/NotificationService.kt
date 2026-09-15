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
    private val pushNotificationSender: PushNotificationSender,
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

    @Transactional
    fun updateFcmToken(
        userId: Long,
        fcmToken: String,
    ) {
        val normalizedToken = fcmToken.trim().takeIf(String::isNotEmpty) ?: throw BaseException(ErrorCode.FCM_TOKEN_BLANK)
        val user = userRepository.findByIdForUpdate(userId) ?: throw BaseException(ErrorCode.USER_NOT_FOUND)
        val previousOwner =
            userRepository
                .findByFcmToken(normalizedToken)
                ?.takeIf { it.id != userId }
        previousOwner?.let {
            it.clearFcmToken()
            userRepository.flush()
        }
        user.changeFcmToken(normalizedToken)
    }

    @Transactional
    fun deleteFcmToken(userId: Long) {
        val user = userRepository.findByIdForUpdate(userId) ?: throw BaseException(ErrorCode.USER_NOT_FOUND)
        user.clearFcmToken()
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
            throw BaseException(ErrorCode.NOT_CHAT_ROOM_KICK_NOTIFICATION)
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
            throw BaseException(ErrorCode.INVALID_DO_NOT_DISTURB_CONFIGURATION)
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

    /// BE-32 · 대기자가 자리를 이어받아 자동 합류했을 때 당사자에게 알린다.
    ///
    /// 승격은 **당사자가 아무 조작도 하지 않은 채** 일어나는 유일한 상태 변화다. 알림이 없으면
    /// 대기자는 자기가 합류한 것을 모른 채 방치된다 (QA BE-32, 2026-09-15).
    ///
    /// `referenceId` 로 참가자 행 id 를 쓴다 — 방 id 를 쓰면 나갔다가 다시 대기·승격된 사람에게
    /// 두 번째 알림이 `uk_notification_reference` 에 막혀 사라진다.
    fun notifyWaitlistPromoted(
        room: ChatRoom,
        recipient: User,
        participantId: Long,
    ) {
        save(
            recipient = recipient,
            type = NotificationType.CHAT_ROOM_WAITLIST_PROMOTED,
            content = "자리가 나서 ${room.roomTitle} 모임에 합류했어요 🎉",
            chatRoomId = room.id,
            referenceId = participantId,
        )
    }

    /// BE-32 · 여행 확정·불발을 **동행자와 아직 기다리는 신청자** 모두에게 알린다.
    ///
    /// 호스트도 참가자 행을 갖지만 **직접 누른 사람**이라 뺀다 — 집합 정보 변경(`notifyMeetingInfoUpdated`)과 같은 방식이다.
    ///
    /// 신청자를 빼먹으면 화면 약속을 절반만 지키게 된다. 18-1 은 확정에 「대기 중인 신청자에게는 마감 알림이 가요」,
    /// 불발에 「승인된 동행자와 대기 중인 신청자 모두에게 알림이 가요」라고 적는다. 이 사람들은
    /// **영영 못 들어가게 된 쪽**이라 오히려 알림이 더 필요하다. 그래서 문구도 동행자와 다르다 —
    /// 동행자에게는 「확정되었어요」지만 신청자에게는 「모집이 마감되었어요」다.
    ///
    /// 방 하나는 `RECRUITING` 에서만 상태가 바뀌므로 확정·불발은 각각 한 번뿐이고, `referenceId` 는 방 id 로 충분하다.
    fun notifyTripStatusChanged(
        room: ChatRoom,
        confirmed: Boolean,
        waitingApplicants: List<User> = emptyList(),
        includeHost: Boolean = false,
    ) {
        val type = if (confirmed) NotificationType.CHAT_ROOM_CONFIRMED else NotificationType.CHAT_ROOM_CANCELLED
        val memberContent =
            if (confirmed) {
                "${room.roomTitle} 여행이 확정되었어요 ✈️"
            } else {
                "${room.roomTitle} 여행이 불발되었어요."
            }
        val applicantContent =
            if (confirmed) {
                "${room.roomTitle} 모집이 마감되었어요."
            } else {
                "${room.roomTitle} 여행이 불발되었어요."
            }
        participantRepository
            .findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
            .asSequence()
            // 호스트가 직접 눌렀으면 본인은 이미 안다. 마감일이 지나 **저절로** 바뀐 것이면 호스트도 받아야 한다.
            .filter { includeHost || it.user.id != room.host.id }
            .forEach { participant ->
                save(participant.user, type, memberContent, room.id, room.id)
            }
        waitingApplicants
            .asSequence()
            .distinctBy { it.id }
            .forEach { applicant ->
                save(applicant, type, applicantContent, room.id, room.id)
            }
    }

    /// BE-33 · 공지가 올라왔음을 방 사람들에게 알린다.
    ///
    /// 예전에는 채팅 시스템 메시지만 남겼다. 시스템 메시지는 `notifyMessage` 를 타지 않아서
    /// 방을 열어 보지 않으면 공지가 올라온 줄 알 수 없었는데, 20-2d 는 「공지를 올리면 방 사람들에게 알림이 가요」라고 적는다.
    fun notifyNoticePosted(
        room: ChatRoom,
        noticeId: Long,
        content: String,
    ) {
        participantRepository
            .findAllByChatRoomIdOrderByCreatedDateTimeAsc(room.id)
            .asSequence()
            .filter { it.user.id != room.host.id }
            .forEach { participant ->
                save(participant.user, NotificationType.CHAT_ROOM_NOTICE_POSTED, "${room.roomTitle} 새 공지 · $content", room.id, noticeId)
            }
    }

    /// BE-33 · 참가 신청 결과를 신청자에게 알린다.
    ///
    /// 19-2·13-1 이 「호스트 승인을 기다리고 있어요. 결과는 알림으로 알려드려요」라고 약속하는데
    /// 예전에는 승인에도 거절에도 아무것도 만들지 않았다. 신청자는 스스로 화면을 다시 열어 보는 수밖에 없었다.
    ///
    /// `referenceId` 는 신청 행 id 다 — 같은 방에 다시 신청할 수 있어서 방 id 로는 두 번째 결과가 사라진다.
    fun notifyApplicationApproved(
        room: ChatRoom,
        applicant: User,
        applicationId: Long,
        joined: Boolean,
    ) {
        val content =
            if (joined) {
                "${room.roomTitle} 참가가 승인되었어요 🎉"
            } else {
                "${room.roomTitle} 신청이 승인되어 대기 순서에 올랐어요."
            }
        save(applicant, NotificationType.CHAT_ROOM_APPLICATION_APPROVED, content, room.id, applicationId)
    }

    fun notifyApplicationRejected(
        room: ChatRoom,
        applicant: User,
        applicationId: Long,
    ) {
        save(
            applicant,
            NotificationType.CHAT_ROOM_APPLICATION_REJECTED,
            "${room.roomTitle} 신청이 받아들여지지 않았어요.",
            room.id,
            applicationId,
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
            if (type != NotificationType.CHAT_ROOM_CREATED) {
                pushNotificationSender.send(notification)
            }
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
