package kr.hanchae.moyeotrip.service.chat

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomJoinApplication
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.chat.TravelCourse
import kr.hanchae.moyeotrip.entity.chat.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.repository.ChatMessageRepository
import kr.hanchae.moyeotrip.repository.ChatRoomJoinApplicationRepository
import kr.hanchae.moyeotrip.repository.ChatRoomNoticeRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TravelCoursePlaceRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime

class ChatRoomServiceTest {
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val applicationRepository = mock(ChatRoomJoinApplicationRepository::class.java)
    private val messageRepository = mock(ChatMessageRepository::class.java)
    private val courseRepository = mock(TravelCourseRepository::class.java)
    private val placeRepository = mock(TravelCoursePlaceRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val noticeRepository = mock(ChatRoomNoticeRepository::class.java)
    private val service =
        ChatRoomService(
            roomRepository,
            participantRepository,
            applicationRepository,
            messageRepository,
            courseRepository,
            placeRepository,
            userRepository,
            objectStorageRepository,
            noticeRepository,
        )

    @Test
    fun `호스트가 신청자를 승인할 때 정원이 가득 차면 승인된 대기열로 이동한다`() {
        val host = user(1L)
        val applicant = user(3L)
        val room = room(host)
        val application =
            ChatRoomJoinApplication(id = 30L, chatRoom = room, user = applicant, applicationMessage = "함께 가고 싶어요")
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(applicationRepository.findByIdAndChatRoomId(30L, 10L)).thenReturn(application)
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(3L)
        `when`(applicationRepository.countByChatRoomIdAndStatus(10L, JoinApplicationStatus.WAITLISTED)).thenReturn(1L)

        val response = service.approveApplication(1L, 10L, 30L)

        assertEquals("WAITLISTED", response.result.name)
        assertEquals(JoinApplicationStatus.WAITLISTED, application.status)
        assertEquals(1, response.waitlistPosition)
    }

    @Test
    fun `참가자가 나가면 호스트가 승인한 대기자 중 첫 사용자가 자동 참가한다`() {
        val host = user(1L)
        val leavingUser = user(2L)
        val waitingUser = user(3L)
        val room = room(host)
        val participant = ChatRoomParticipant(id = 20L, chatRoom = room, user = leavingUser, role = ChatParticipantRole.MEMBER)
        val waiting =
            ChatRoomJoinApplication(
                id = 30L,
                chatRoom = room,
                user = waitingUser,
                applicationMessage = "신청합니다",
                status = JoinApplicationStatus.WAITLISTED,
            )
        `when`(roomRepository.findByIdForUpdate(10L)).thenReturn(room)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(
            applicationRepository.findFirstByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
                10L,
                JoinApplicationStatus.WAITLISTED,
            ),
        ).thenReturn(waiting)
        `when`(participantRepository.save(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(participantRepository.saveAndFlush(any(ChatRoomParticipant::class.java))).thenAnswer { it.arguments[0] }
        `when`(messageRepository.saveAndFlush(any(ChatMessage::class.java))).thenAnswer { it.arguments[0] }

        val response = service.leaveRoom(2L, 10L)

        assertEquals(3L, response.promotedUserId)
        verify(applicationRepository).delete(waiting)
        verify(participantRepository).saveAndFlush(any(ChatRoomParticipant::class.java))
    }

    private fun user(id: Long) = User(id = id, userRole = UserRole.ROLE_USER)

    private fun room(host: User) =
        ChatRoom(
            id = 10L,
            host = host,
            course = TravelCourse(id = 5L, type = TravelCourseType.MANAGED, title = "울릉도 대표 코스"),
            roomTitle = "울릉도 여행",
            maxParticipants = 3,
            startDate = LocalDate.now().plusDays(10),
            recruitmentDeadlineDate = LocalDate.now().plusDays(5),
            tripDays = 2,
            meetingLatitude = 36.0322,
            meetingLongitude = 129.3747,
            meetingDateTime = LocalDateTime.now().plusDays(10),
            participationFee = 100000L,
        )
}
