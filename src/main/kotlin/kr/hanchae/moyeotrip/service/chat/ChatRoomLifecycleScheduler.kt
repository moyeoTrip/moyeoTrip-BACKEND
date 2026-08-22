package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import kr.hanchae.moyeotrip.service.user.TravelCompanionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class ChatRoomLifecycleScheduler(
    private val roomRepository: ChatRoomRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val messageRepository: ChatMessageRepository,
    private val courseRepository: TravelCourseRepository,
    private val notificationService: NotificationService,
    private val realtimeMessagingService: RealtimeMessagingService,
    private val travelCompanionService: TravelCompanionService,
) {
    @Scheduled(cron = "0 0 13 * * *", zone = "Asia/Seoul")
    @Transactional
    fun notifyRecruitmentDeadline() {
        roomRepository
            .findAllByStatusAndRecruitmentDeadlineDateBetween(
                ChatRoomStatus.RECRUITING,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
            ).forEach(notificationService::notifyRecruitmentDeadline)
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun closeExpiredRecruitingRooms() {
        val now = LocalDateTime.now()
        roomRepository
            .findAllExpiredRecruitingRoomsForUpdate(ChatRoomStatus.RECRUITING, LocalDate.now())
            .forEach { room ->
                if (participantRepository.countByChatRoomId(room.id) >= MINIMUM_TRIP_PARTICIPANTS) {
                    room.confirm()
                    saveSystemMessage(room, "모집이 마감되어 여행이 확정되었어요.")
                } else {
                    saveSystemMessage(room, "모집 마감까지 3명이 모이지 않아 여행이 불발되었어요.")
                    room.cancel(now)
                }
            }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun announceTripsStartingToday() {
        roomRepository
            .findAllStartingRoomsWithoutSystemEvent(ChatRoomStatus.CONFIRMED, LocalDate.now(), TRIP_STARTED_EVENT_KEY)
            .forEach { room ->
                saveSystemMessage(room, "오늘 여행이 시작됐어요 🎒", TRIP_STARTED_EVENT_KEY)
            }
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun collectCompletedTripCompanions() {
        roomRepository
            .findAllCompletedConfirmedRooms(ChatRoomStatus.CONFIRMED, LocalDate.now())
            .forEach(travelCompanionService::collectCompletedTrip)
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun scheduleCompletedRoomDeletion() {
        roomRepository
            .findAllCompletedRoomsWithoutDeletionScheduleForUpdate(ChatRoomStatus.CONFIRMED, LocalDate.now())
            .forEach { room ->
                room.scheduleDeletion((room.endDate ?: room.startDate).plusDays(CHAT_ROOM_RETENTION_DAYS))
            }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun deleteExpiredRooms() {
        val now = LocalDateTime.now()
        roomRepository.findAllDeletionDueRoomsForUpdate(LocalDate.now()).forEach { room ->
            if (room.status == ChatRoomStatus.CONFIRMED) {
                messageRepository.deleteAllByChatRoomId(room.id)
                room.archiveChat(now)
            } else {
                val customCourse = room.course.takeIf { it.type == TravelCourseType.CUSTOM }
                roomRepository.delete(room)
                roomRepository.flush()
                customCourse?.let(courseRepository::delete)
            }
        }
    }

    private fun saveSystemMessage(
        room: ChatRoom,
        content: String,
        systemEventKey: String? = null,
    ) {
        val message =
            messageRepository.save(
                ChatMessage(
                    chatRoom = room,
                    type = ChatMessageType.SYSTEM,
                    content = content,
                    systemEventKey = systemEventKey,
                ),
            )
        realtimeMessagingService.sendChatMessage(
            room.id,
            ChatMessageResponse(
                messageId = message.id,
                type = ChatMessageType.SYSTEM,
                senderId = null,
                senderNickname = "시스템",
                content = content,
                createdAt = LocalDateTime.now(),
            ),
        )
    }

    companion object {
        const val MINIMUM_TRIP_PARTICIPANTS = 3L
        private const val CHAT_ROOM_RETENTION_DAYS = 14L
        private const val TRIP_STARTED_EVENT_KEY = "TRIP_STARTED"
    }
}
