package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
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
) {
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun notifyRecruitmentDeadline() {
        roomRepository
            .findAllByStatusAndRecruitmentDeadlineDate(ChatRoomStatus.RECRUITING, LocalDate.now().plusDays(1))
            .forEach(notificationService::notifyRecruitmentDeadline)
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
    fun deleteExpiredRooms() {
        roomRepository.findAllDeletionDueRoomsForUpdate(LocalDate.now()).forEach { room ->
            val customCourse = room.course.takeIf { it.type == TravelCourseType.CUSTOM }
            roomRepository.delete(room)
            roomRepository.flush()
            customCourse?.let(courseRepository::delete)
        }
    }

    private fun saveSystemMessage(
        room: kr.hanchae.moyeotrip.entity.chat.ChatRoom,
        content: String,
    ) {
        messageRepository.save(ChatMessage(chatRoom = room, type = ChatMessageType.SYSTEM, content = content))
    }

    companion object {
        const val MINIMUM_TRIP_PARTICIPANTS = 3L
    }
}
