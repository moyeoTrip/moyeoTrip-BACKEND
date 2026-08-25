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
import kr.hanchae.moyeotrip.service.user.TravelCompanionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
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
    private val travelCompanionService = mock(TravelCompanionService::class.java)
    private val scheduler =
        ChatRoomLifecycleScheduler(
            roomRepository,
            participantRepository,
            messageRepository,
            courseRepository,
            notificationService,
            realtimeMessagingService,
            travelCompanionService,
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
    fun `마감이 사흘 이내인 모집방에 마감 알림을 보낸다`() {
        val room = room()
        `when`(
            roomRepository.findAllByStatusAndRecruitmentDeadlineDateBetween(
                ChatRoomStatus.RECRUITING,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
            ),
        ).thenReturn(listOf(room))

        scheduler.notifyRecruitmentDeadline()

        verify(notificationService).notifyRecruitmentDeadline(room)
    }

    @Test
    fun `모집 마감일까지 세 명 이상 모이면 여행을 확정한다`() {
        val room = room()
        `when`(roomRepository.findAllExpiredRecruitingRoomsForUpdate(ChatRoomStatus.RECRUITING, LocalDate.now()))
            .thenReturn(listOf(room))
        `when`(participantRepository.countByChatRoomId(room.id)).thenReturn(3L)
        `when`(messageRepository.save(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        scheduler.closeExpiredRecruitingRooms()

        assertEquals(ChatRoomStatus.CONFIRMED, room.status)
        verify(messageRepository).save(
            org.mockito.ArgumentMatchers.argThat { it.content == "모집이 마감되어 여행이 확정되었어요." },
        )
    }

    @Test
    fun `확정 여행 시작일에 여행 시작 시스템 메시지를 한 번 생성한다`() {
        val host = User(id = 1L, userRole = UserRole.ROLE_USER)
        val room =
            ChatRoom(
                id = 12L,
                host = host,
                course = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "공개 코스"),
                roomTitle = "오늘 여행",
                maxParticipants = 5,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(1),
                recruitmentDeadlineDate = LocalDate.now().minusDays(1),
                meetingDateTime = LocalDate.now().atStartOfDay(),
                status = ChatRoomStatus.CONFIRMED,
            )
        `when`(
            roomRepository.findAllStartingRoomsWithoutSystemEvent(
                ChatRoomStatus.CONFIRMED,
                LocalDate.now(),
                "TRIP_STARTED",
            ),
        ).thenReturn(listOf(room))
        `when`(messageRepository.save(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }
        val messageCaptor = ArgumentCaptor.forClass(ChatMessage::class.java)

        scheduler.announceTripsStartingToday()

        verify(messageRepository).save(messageCaptor.capture())
        assertEquals("오늘 여행이 시작됐어요 🎒", messageCaptor.value.content)
        assertEquals("TRIP_STARTED", messageCaptor.value.systemEventKey)
    }

    @Test
    fun `완료된 확정 여행은 여행 종료일로부터 14일 후 메시지 삭제를 예약한다`() {
        val room =
            room().also {
                it.confirm()
            }
        `when`(
            roomRepository.findAllCompletedRoomsWithoutDeletionScheduleForUpdate(ChatRoomStatus.CONFIRMED, LocalDate.now()),
        ).thenReturn(listOf(room))

        scheduler.scheduleCompletedRoomDeletion()

        assertEquals(room.endDate!!.plusDays(14), room.deletionScheduledDate)
    }

    @Test
    fun `완료된 확정 여행의 동행자 기록을 수집한다`() {
        val room = room().also(ChatRoom::confirm)
        `when`(roomRepository.findAllCompletedConfirmedRooms(ChatRoomStatus.CONFIRMED, LocalDate.now()))
            .thenReturn(listOf(room))

        scheduler.collectCompletedTripCompanions()

        verify(travelCompanionService).collectCompletedTrip(room)
    }

    @Test
    fun `삭제 시 확정 여행은 메시지만 삭제하고 채팅방을 보관한다`() {
        val room =
            room().also {
                it.confirm()
                it.scheduleDeletion(LocalDate.now())
            }
        `when`(roomRepository.findAllDeletionDueRoomsForUpdate(LocalDate.now())).thenReturn(listOf(room))

        scheduler.deleteExpiredRooms()

        verify(messageRepository).deleteAllByChatRoomId(room.id)
        assertEquals(true, room.isChatArchived())
        verify(roomRepository, never()).delete(room)
        verify(courseRepository, never()).delete(room.course)
    }

    @Test
    fun `삭제 예정인 불발 방은 삭제하고 커스텀 코스도 함께 삭제한다`() {
        val host = User(id = 1L, userRole = UserRole.ROLE_USER)
        val customCourse = TravelCourse(id = 7L, type = TravelCourseType.CUSTOM, owner = host, title = "직접 만든 코스")
        val cancelledRoom =
            ChatRoom(
                id = 20L,
                host = host,
                course = customCourse,
                roomTitle = "불발 여행",
                maxParticipants = 3,
                startDate = LocalDate.now().minusDays(1),
                endDate = LocalDate.now(),
                recruitmentDeadlineDate = LocalDate.now().minusDays(2),
                meetingDateTime = LocalDate.now().minusDays(1).atStartOfDay(),
            ).also { it.cancel(LocalDateTime.now().minusDays(14)) }
        `when`(roomRepository.findAllDeletionDueRoomsForUpdate(LocalDate.now())).thenReturn(listOf(cancelledRoom))

        scheduler.deleteExpiredRooms()

        verify(roomRepository).delete(cancelledRoom)
        verify(roomRepository).flush()
        verify(courseRepository).delete(customCourse)
        verify(messageRepository, never()).deleteAllByChatRoomId(20L)
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
