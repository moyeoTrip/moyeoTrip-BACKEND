package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomJoinApplicationRepository
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
    private val applicationRepository: ChatRoomJoinApplicationRepository,
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
                val confirmed = participantRepository.countByChatRoomId(room.id) >= room.minimumParticipants
                if (confirmed) {
                    room.confirm()
                    saveSystemMessage(room, "모집이 마감되어 여행이 확정되었어요.")
                } else {
                    saveSystemMessage(room, "모집 마감까지 최소 ${room.minimumParticipants}명이 모이지 않아 여행이 불발되었어요.")
                    room.cancel(now)
                }
                // BE-32 · 손으로 바꾸는 `ChatRoomService.changeStatus` 에만 알림을 붙였더니 **이 자동 경로가 빠졌다.**
                // 오히려 이쪽이 더 필요하다 — 아무도 누르지 않았고 아무도 보고 있지 않은 채로 상태가 바뀐다.
                // 그래서 호스트도 받는다(`includeHost = true`). 손으로 누른 경우와 달리 호스트도 모르기 때문이다.
                notificationService.notifyTripStatusChanged(
                    room = room,
                    confirmed = confirmed,
                    waitingApplicants = waitingApplicants(room),
                    includeHost = true,
                )
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

    /**
     * 여행이 끝나고 [CHAT_CLOSE_DAYS] 일이 지난 방의 **채팅을 잠근다.** 읽기는 그대로 된다.
     *
     * 예전에는 잠금 시점이 따로 없어서, **삭제(+14일)가 오기 전까지 계속 쓸 수 있었다.**
     * 여행이 끝난 뒤 대화가 무기한 이어지면 방의 성격이 흐려진다는 판단이다(사용자 결정, 2026-09-14).
     *
     * 삭제 일정과는 **독립**이다 — 잠긴 뒤에도 예약된 삭제는 예정대로 온다.
     */
    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Seoul")
    @Transactional
    fun closeChatForCompletedRooms() {
        val now = LocalDateTime.now()
        roomRepository
            .findAllChatCloseDueRoomsForUpdate(ChatRoomStatus.CONFIRMED, LocalDate.now().minusDays(CHAT_CLOSE_DAYS))
            .forEach { room ->
                room.closeChatForWriting(now)
                // 어느 날 갑자기 입력창이 잠기면 「고장났나」로 읽힌다 — 왜 잠겼는지 대화에 남긴다.
                saveSystemMessage(
                    room,
                    "여행이 끝난 지 ${CHAT_CLOSE_DAYS}일이 지나 대화를 마쳤어요. 지난 대화는 계속 볼 수 있어요.",
                    systemEventKey = CHAT_CLOSED_EVENT_KEY,
                )
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

    /** 아직 기다리던 신청자(승인 대기·대기열). 상태가 바뀌어도 신청 행은 남으므로 지금 읽는다. */
    private fun waitingApplicants(room: ChatRoom) =
        WAITING_APPLICATION_STATUSES
            .flatMap { applicationRepository.findAllByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(room.id, it) }
            .map { it.user }

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
        private val WAITING_APPLICATION_STATUSES =
            listOf(JoinApplicationStatus.PENDING, JoinApplicationStatus.WAITLISTED)

        private const val CHAT_ROOM_RETENTION_DAYS = 14L

        /**
         * 여행 종료 뒤 채팅을 **잠그기까지의 날 수**. 보관·삭제(`CHAT_ROOM_RETENTION_DAYS`)보다 짧다 —
         * 「더 못 쓴다」와 「기록이 사라진다」는 다른 일이라 시점을 따로 둔다.
         */
        private const val CHAT_CLOSE_DAYS = 7L

        /** 잠금 안내 시스템 메시지가 방마다 한 번만 남도록 하는 키. */
        private const val CHAT_CLOSED_EVENT_KEY = "CHAT_CLOSED"
        private const val TRIP_STARTED_EVENT_KEY = "TRIP_STARTED"
    }
}
