package kr.hanchae.moyeotrip.service.notification

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.notification.ChatRoomNotificationSetting
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.repository.ChatRoomNotificationSettingRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.NotificationRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationServiceTest {
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val settingRepository = mock(NotificationSettingRepository::class.java)
    private val roomSettingRepository = mock(ChatRoomNotificationSettingRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val realtimeMessagingService = mock(RealtimeMessagingService::class.java)
    private val service =
        NotificationService(
            notificationRepository,
            participantRepository,
            settingRepository,
            roomSettingRepository,
            userRepository,
            realtimeMessagingService,
        )

    @Test
    fun `멘션 답글 전용 설정은 일반 채팅 알림을 만들지 않는다`() {
        val sender = user(1L, "보낸이")
        val recipient = user(2L, "받는이")
        val room = room(sender)
        val message = ChatMessage(id = 20L, chatRoom = room, sender = sender, type = ChatMessageType.USER, content = "안녕하세요")
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = recipient, role = ChatParticipantRole.MEMBER),
                ),
            )
        `when`(settingRepository.findByUserId(2L))
            .thenReturn(NotificationSetting(user = recipient, chatNotificationMode = ChatNotificationMode.MENTIONS_AND_REPLIES))

        service.notifyMessage(message)

        verifyNoInteractions(notificationRepository)
        verifyNoInteractions(realtimeMessagingService)
    }

    @Test
    fun `채팅방 알림을 끄면 전역 설정이 켜져 있어도 알림을 만들지 않는다`() {
        val sender = user(1L, "보낸이")
        val recipient = user(2L, "받는이")
        val room = room(sender)
        val message = ChatMessage(id = 20L, chatRoom = room, sender = sender, type = ChatMessageType.USER, content = "안녕하세요")
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = recipient, role = ChatParticipantRole.MEMBER),
                ),
            )
        `when`(roomSettingRepository.findByUserIdAndChatRoomId(2L, 10L))
            .thenReturn(ChatRoomNotificationSetting(user = recipient, chatRoom = room, enabled = false))

        service.notifyMessage(message)

        verifyNoInteractions(notificationRepository)
        verifyNoInteractions(realtimeMessagingService)
    }

    @Test
    fun `자정을 넘는 방해 금지 시간은 시작 요일의 다음 날 종료 시각까지 적용한다`() {
        val setting = NotificationSetting(user = user(2L, "받는이"))
        setting.update(
            chatNotificationMode = ChatNotificationMode.ALL,
            recruitmentDeadlineEnabled = true,
            socialActivityEnabled = true,
            marketingEnabled = false,
            doNotDisturbEnabled = true,
            doNotDisturbStartTime = LocalTime.of(22, 30),
            doNotDisturbEndTime = LocalTime.of(7, 0),
            doNotDisturbDays = setOf(DayOfWeek.MONDAY),
        )

        assertTrue(setting.isDoNotDisturbing(LocalDateTime.of(2026, 8, 24, 23, 0)))
        assertTrue(setting.isDoNotDisturbing(LocalDateTime.of(2026, 8, 25, 6, 59)))
        assertFalse(setting.isDoNotDisturbing(LocalDateTime.of(2026, 8, 25, 7, 0)))
        assertFalse(setting.isDoNotDisturbing(LocalDateTime.of(2026, 8, 25, 23, 0)))
    }

    private fun user(
        id: Long,
        nickname: String,
    ) = User(
        id = id,
        userRole = UserRole.ROLE_USER,
        userInformation = UserInformation(nickname, NicknameColor.GREEN, kr.hanchae.moyeotrip.entity.user.Gender.N),
    )

    private fun room(host: User) =
        ChatRoom(
            id = 10L,
            host = host,
            course = TravelCourse(id = 5L, type = TravelCourseType.PUBLIC, title = "테스트 코스"),
            roomTitle = "테스트 방",
            maxParticipants = 3,
            startDate = LocalDate.now().plusDays(10),
            endDate = LocalDate.now().plusDays(11),
            recruitmentDeadlineDate = LocalDate.now().plusDays(5),
            meetingDateTime = LocalDate.now().plusDays(10).atStartOfDay(),
        )
}
