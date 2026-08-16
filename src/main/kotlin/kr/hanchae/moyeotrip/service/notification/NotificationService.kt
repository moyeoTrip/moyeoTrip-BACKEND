package kr.hanchae.moyeotrip.service.notification

import kr.hanchae.moyeotrip.controller.notification.response.NotificationPageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationSettingResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.notification.Notification
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.NotificationRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val settingRepository: NotificationSettingRepository,
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

    @Transactional
    fun getSetting(userId: Long): NotificationSettingResponse = findOrCreateSetting(userId).toResponse()

    @Transactional
    fun updateSetting(
        userId: Long,
        chatMessageEnabled: Boolean,
        recruitmentDeadlineEnabled: Boolean,
        socialActivityEnabled: Boolean,
        marketingEnabled: Boolean,
    ): NotificationSettingResponse {
        val setting = findOrCreateSetting(userId)
        setting.update(chatMessageEnabled, recruitmentDeadlineEnabled, socialActivityEnabled, marketingEnabled)
        return setting.toResponse()
    }

    fun notifyRoomCreated(room: ChatRoom) {
        save(room.host, NotificationType.CHAT_ROOM_CREATED, "${room.roomTitle} 모임이 만들어졌어요 ✨", room.id, room.id)
    }

    fun notifyMessage(message: ChatMessage) {
        val sender = message.sender ?: return
        participantRepository
            .findAllByChatRoomIdOrderByCreatedDateTimeAsc(message.chatRoom.id)
            .asSequence()
            .map { it.user }
            .filter { it.id != sender.id }
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

    private fun save(
        recipient: User,
        type: NotificationType,
        content: String,
        chatRoomId: Long,
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
        realtimeMessagingService.sendNotification(recipient.id, notification.toResponse())
    }

    private fun allows(
        recipient: User,
        type: NotificationType,
    ): Boolean = settingRepository.findByUserId(recipient.id)?.allows(type) ?: true

    private fun findOrCreateSetting(userId: Long): NotificationSetting =
        settingRepository.findByUserId(userId)
            ?: settingRepository.save(
                NotificationSetting(
                    user = userRepository.findById(userId).orElseThrow { BaseException(ErrorCode.USER_NOT_FOUND) },
                ),
            )

    companion object {
        private const val DEADLINE_REFERENCE_MULTIPLIER = 100_000L
    }
}

private fun NotificationSetting.toResponse() =
    NotificationSettingResponse(
        chatMessageEnabled = chatMessageEnabled,
        recruitmentDeadlineEnabled = recruitmentDeadlineEnabled,
        socialActivityEnabled = socialActivityEnabled,
        marketingEnabled = marketingEnabled,
    )

private fun Notification.toResponse() =
    NotificationResponse(
        notificationId = id,
        type = type,
        content = content,
        chatRoomId = chatRoomId,
        read = readDateTime != null,
        createdAt = createdDateTime,
        //TODO:친구 신청 및 피드 알림 추가
    )
