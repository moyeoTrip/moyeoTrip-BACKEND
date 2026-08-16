package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ChatRoomLifecycleSchedulerTest {
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val messageRepository = mock(ChatMessageRepository::class.java)
    private val courseRepository = mock(TravelCourseRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val realtimeMessagingService = mock(RealtimeMessagingService::class.java)
    private val scheduler =
        ChatRoomLifecycleScheduler(
            roomRepository,
            participantRepository,
            messageRepository,
            courseRepository,
            notificationService,
            realtimeMessagingService,
        )

    @Test
    fun `모집 마감일까지 세 명이 모이지 않으면 채팅을 닫고 14일 후 삭제를 예약한다`() {
        val room = room()
        `when`(
            roomRepository.findAllExpiredRecruitingRoomsForUpdate(ChatRoomStatus.RECRUITING, LocalDate.now()),
        ).thenReturn(listOf(room))
        `when`(participantRepository.countByChatRoomId(room.id)).thenReturn(2L)
        `when`(messageRepository.save(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        scheduler.closeExpiredRecruitingRooms()

        assertEquals(ChatRoomStatus.CANCELLED, room.status)
        assertNotNull(room.chatClosedDateTime)
        assertEquals(14L, ChronoUnit.DAYS.between(room.chatClosedDateTime!!.toLocalDate(), room.deletionScheduledDate!!))
        verify(messageRepository).save(any(ChatMessage::class.java))
    }

    @Test
    fun `확정 여행이 종료되면 커스텀 코스를 공개한다`() {
        val host = User(id = 1L, userRole = UserRole.ROLE_USER)
        val course = TravelCourse(id = 7L, type = TravelCourseType.CUSTOM, owner = host, title = "다녀온 코스")
        val room =
            ChatRoom(
                id = 11L,
                host = host,
                course = course,
                roomTitle = "완료 여행",
                maxParticipants = 5,
                startDate = LocalDate.now().minusDays(2),
                endDate = LocalDate.now().minusDays(1),
                recruitmentDeadlineDate = LocalDate.now().minusDays(3),
                meetingDateTime = LocalDateTime.now().minusDays(2),
                status = ChatRoomStatus.CONFIRMED,
            )
        `when`(
            roomRepository.findAllCompletedConfirmedRoomsForUpdate(ChatRoomStatus.CONFIRMED, LocalDate.now()),
        ).thenReturn(listOf(room))

        scheduler.publishCompletedTravelCourses()

        assertEquals(TravelCourseType.PUBLIC, course.type)
    }

    private fun room(): ChatRoom {
        val host = User(id = 1L, userRole = UserRole.ROLE_USER)
        return ChatRoom(
            id = 10L,
            host = host,
            course = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "공개 코스"),
            roomTitle = "경주 여행",
            maxParticipants = 5,
            startDate = LocalDate.now().plusDays(5),
            endDate = LocalDate.now().plusDays(6),
            recruitmentDeadlineDate = LocalDate.now().minusDays(1),
            meetingLatitude = 37.5547,
            meetingLongitude = 126.9706,
            meetingDateTime = LocalDateTime.now().plusDays(5),
            participationFee = 50000L,
        )
    }
}
