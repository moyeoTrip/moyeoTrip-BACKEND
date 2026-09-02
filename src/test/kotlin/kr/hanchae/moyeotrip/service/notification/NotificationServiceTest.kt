package kr.hanchae.moyeotrip.service.notification

import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomKickHistory
import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.notification.ChatRoomNotificationSetting
import kr.hanchae.moyeotrip.entity.notification.Notification
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomKickHistoryRepository
import kr.hanchae.moyeotrip.repository.ChatRoomNotificationSettingRepository
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.NotificationRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.realtime.RealtimeMessagingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class NotificationServiceTest {
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val kickHistoryRepository = mock(ChatRoomKickHistoryRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val settingRepository = mock(NotificationSettingRepository::class.java)
    private val roomSettingRepository = mock(ChatRoomNotificationSettingRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val realtimeMessagingService = mock(RealtimeMessagingService::class.java)
    private val pushNotificationSender = mock(PushNotificationSender::class.java)
    private val service =
        NotificationService(
            notificationRepository,
            kickHistoryRepository,
            participantRepository,
            settingRepository,
            roomSettingRepository,
            userRepository,
            realtimeMessagingService,
            pushNotificationSender,
        )

    @Test
    fun `알림 목록은 lastId 커서와 다음 페이지 및 안 읽은 수를 반환한다`() {
        val recipient = user(2L, "받는이")
        val notifications =
            listOf(
                notification(5L, recipient),
                notification(4L, recipient),
                notification(3L, recipient),
            )
        `when`(
            notificationRepository.findAllByRecipientIdAndIdLessThanOrderByIdDesc(
                2L,
                Long.MAX_VALUE,
                PageRequest.of(0, 3),
            ),
        ).thenReturn(notifications)
        `when`(notificationRepository.countByRecipientIdAndReadDateTimeIsNull(2L)).thenReturn(4L)

        val response = service.getNotifications(2L, lastId = null, size = 2, unreadOnly = false)

        assertEquals(listOf(5L, 4L), response.notifications.map { it.notificationId })
        assertEquals(4L, response.nextLastId)
        assertTrue(response.hasNext)
        assertEquals(4L, response.unreadCount)
    }

    @Test
    fun `안 읽은 알림만 커서 이전에서 조회한다`() {
        `when`(
            notificationRepository.findAllByRecipientIdAndReadDateTimeIsNullAndIdLessThanOrderByIdDesc(
                2L,
                10L,
                PageRequest.of(0, 3),
            ),
        ).thenReturn(emptyList())

        val response = service.getNotifications(2L, lastId = 10L, size = 2, unreadOnly = true)

        assertFalse(response.hasNext)
        assertNull(response.nextLastId)
    }

    @Test
    fun `내 알림 한 건과 전체 안 읽은 알림을 읽음 처리한다`() {
        val recipient = user(2L, "받는이")
        val first = notification(1L, recipient)
        val second = notification(2L, recipient)
        `when`(notificationRepository.findByIdAndRecipientId(1L, 2L)).thenReturn(first)
        `when`(notificationRepository.findAllByRecipientIdAndReadDateTimeIsNull(2L)).thenReturn(listOf(second))

        service.markRead(2L, 1L)
        service.markAllRead(2L)

        assertNotNull(first.readDateTime)
        assertNotNull(second.readDateTime)
    }

    @Test
    fun `내 알림이 아니면 읽음 처리할 수 없다`() {
        `when`(notificationRepository.findByIdAndRecipientId(1L, 2L)).thenReturn(null)

        val exception = assertThrows(BaseException::class.java) { service.markRead(2L, 1L) }

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `FCM 토큰 갱신은 이전 소유자에게서 토큰을 해제하고 현재 사용자에게 저장한다`() {
        val currentUser = user(2L, "현재 사용자")
        val previousOwner = user(3L, "이전 사용자").also { it.changeFcmToken("new-token") }
        `when`(userRepository.findByIdForUpdate(2L)).thenReturn(currentUser)
        `when`(userRepository.findByFcmToken("new-token")).thenReturn(previousOwner)

        service.updateFcmToken(2L, "  new-token  ")

        assertEquals("new-token", currentUser.fcmToken)
        assertNull(previousOwner.fcmToken)
        verify(userRepository).flush()
    }

    @Test
    fun `FCM 토큰 삭제는 현재 사용자의 기기 연결을 해제한다`() {
        val currentUser = user(2L, "현재 사용자").also { it.changeFcmToken("fcm-token") }
        `when`(userRepository.findByIdForUpdate(2L)).thenReturn(currentUser)

        service.deleteFcmToken(2L)

        assertNull(currentUser.fcmToken)
    }

    @Test
    fun `공백 FCM 토큰은 저장할 수 없다`() {
        val exception = assertThrows(BaseException::class.java) { service.updateFcmToken(2L, "   ") }

        assertEquals(ErrorCode.FCM_TOKEN_BLANK, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `강퇴 타입이 아닌 알림으로 강퇴 이력을 조회할 수 없다`() {
        val recipient = user(2L, "받는이")
        val notification =
            Notification(1L, recipient, NotificationType.FEED_LIKE, "좋아요", chatRoomId = 10L, referenceId = 3L)
        `when`(notificationRepository.findByIdAndRecipientId(1L, 2L)).thenReturn(notification)

        val exception = assertThrows(BaseException::class.java) { service.getKickHistory(2L, 1L) }

        assertEquals(ErrorCode.NOT_CHAT_ROOM_KICK_NOTIFICATION, exception.errorCode)
        verifyNoInteractions(kickHistoryRepository)
    }

    @Test
    fun `알림 설정이 없으면 기본 설정을 생성한다`() {
        val recipient = user(2L, "받는이")
        `when`(settingRepository.findByUserId(2L)).thenReturn(null)
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(recipient))
        `when`(settingRepository.save(any(NotificationSetting::class.java))).thenAnswer { it.arguments[0] }

        val response = service.getSetting(2L)

        assertFalse(response.doNotDisturbEnabled)
        verify(settingRepository).save(any(NotificationSetting::class.java))
    }

    @Test
    fun `방해 금지를 켤 때 시간이나 요일이 빠지면 거부한다`() {
        val invalidCases =
            listOf(
                Triple(null, LocalTime.of(7, 0), setOf(DayOfWeek.MONDAY)),
                Triple(LocalTime.of(22, 0), null, setOf(DayOfWeek.MONDAY)),
                Triple(LocalTime.of(22, 0), LocalTime.of(22, 0), setOf(DayOfWeek.MONDAY)),
                Triple(LocalTime.of(22, 0), LocalTime.of(7, 0), emptySet()),
            )

        invalidCases.forEach { (start, end, days) ->
            val exception =
                assertThrows(BaseException::class.java) {
                    service.updateSetting(2L, ChatNotificationMode.ALL, true, true, false, true, start, end, days)
                }
            assertEquals(ErrorCode.INVALID_DO_NOT_DISTURB_CONFIGURATION, exception.errorCode)
        }
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `채팅방 알림 설정이 없으면 참가자 기준으로 생성하고 이후에는 갱신한다`() {
        val recipient = user(2L, "받는이")
        val room = room(user(1L, "호스트"))
        val participant = ChatRoomParticipant(chatRoom = room, user = recipient, role = ChatParticipantRole.MEMBER)
        val existing = ChatRoomNotificationSetting(user = recipient, chatRoom = room, enabled = false)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(roomSettingRepository.findByUserIdAndChatRoomId(2L, 10L)).thenReturn(null, existing)
        `when`(roomSettingRepository.save(any(ChatRoomNotificationSetting::class.java))).thenAnswer { it.arguments[0] }

        val created = service.updateChatRoomSetting(2L, 10L, enabled = false)
        val updated = service.updateChatRoomSetting(2L, 10L, enabled = true)

        assertFalse(created.enabled)
        assertTrue(updated.enabled)
    }

    @Test
    fun `채팅 메시지 발신자가 없으면 알림을 만들지 않는다`() {
        val message = ChatMessage(id = 20L, chatRoom = room(user(1L, "호스트")), type = ChatMessageType.SYSTEM, content = "안내")

        service.notifyMessage(message)

        verifyNoInteractions(participantRepository, notificationRepository)
    }

    @Test
    fun `멘션된 사용자는 멘션 전용 설정에서도 실시간 알림을 받는다`() {
        val sender = user(1L, "보낸이")
        val recipient = user(2L, "받는이")
        val room = room(sender)
        val message = ChatMessage(id = 20L, chatRoom = room, sender = sender, type = ChatMessageType.USER, content = "확인해주세요")
        message.mention(listOf(recipient))
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = sender, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = recipient, role = ChatParticipantRole.MEMBER),
                ),
            )
        `when`(settingRepository.findByUserId(2L))
            .thenReturn(NotificationSetting(user = recipient, chatNotificationMode = ChatNotificationMode.MENTIONS_AND_REPLIES))
        `when`(notificationRepository.save(any(Notification::class.java)))
            .thenAnswer { (it.arguments[0] as Notification).withCreatedAt(LocalDateTime.now()) }

        service.notifyMessage(message)

        verify(notificationRepository).save(any(Notification::class.java))
        verify(realtimeMessagingService).sendNotification(org.mockito.ArgumentMatchers.eq(2L), anyValue())
    }

    @Test
    fun `내 메시지에 대한 답글은 멘션 전용 설정에서도 알림을 받는다`() {
        val originalSender = user(2L, "원문 작성자")
        val replySender = user(1L, "답글 작성자")
        val room = room(replySender)
        val original = ChatMessage(id = 19L, chatRoom = room, sender = originalSender, type = ChatMessageType.USER, content = "원문")
        val reply =
            ChatMessage(
                id = 20L,
                chatRoom = room,
                sender = replySender,
                type = ChatMessageType.USER,
                content = "답글",
                replyTo = original,
            )
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = replySender, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = originalSender, role = ChatParticipantRole.MEMBER),
                ),
            )
        `when`(settingRepository.findByUserId(2L))
            .thenReturn(NotificationSetting(user = originalSender, chatNotificationMode = ChatNotificationMode.MENTIONS_AND_REPLIES))
        stubNotificationSave()

        service.notifyMessage(reply)

        verify(notificationRepository).save(any(Notification::class.java))
    }

    @Test
    fun `전체 채팅 알림을 끈 사용자는 일반 메시지 알림을 받지 않는다`() {
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
            .thenReturn(NotificationSetting(user = recipient, chatNotificationMode = ChatNotificationMode.NONE))

        service.notifyMessage(message)

        verifyNoInteractions(notificationRepository, realtimeMessagingService, pushNotificationSender)
    }

    @Test
    fun `이미 생성된 동일 알림은 중복 저장하지 않는다`() {
        val room = room(user(1L, "호스트"))
        `when`(
            notificationRepository.existsByRecipientIdAndTypeAndReferenceId(
                1L,
                NotificationType.CHAT_ROOM_CREATED,
                10L,
            ),
        ).thenReturn(true)

        service.notifyRoomCreated(room)

        verify(notificationRepository, org.mockito.Mockito.never()).save(any(Notification::class.java))
        verifyNoInteractions(realtimeMessagingService)
    }

    @Test
    fun `채팅방 생성 알림은 앱 내 실시간 알림만 보내고 푸시는 보내지 않는다`() {
        val room = room(user(1L, "호스트"))
        stubNotificationSave()

        service.notifyRoomCreated(room)

        verify(realtimeMessagingService).sendNotification(org.mockito.ArgumentMatchers.eq(1L), anyValue())
        verifyNoInteractions(pushNotificationSender)
    }

    @Test
    fun `자기 피드 좋아요는 알림을 만들지 않는다`() {
        val author = user(1L, "작성자")
        val feed = mock(kr.hanchae.moyeotrip.entity.feed.Feed::class.java)
        `when`(feed.author).thenReturn(author)

        service.notifyFeedLiked(feed, author)

        verifyNoInteractions(notificationRepository)
    }

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

    @Test
    fun `강퇴 알림을 클릭하면 수신자 본인의 강퇴 이력을 반환한다`() {
        val host = user(1L, "호스트")
        val kickedUser = user(2L, "강퇴된 사용자")
        val kickedAt = LocalDateTime.of(2026, 8, 23, 12, 0)
        val notification =
            Notification(
                id = 101L,
                recipient = kickedUser,
                type = NotificationType.CHAT_ROOM_KICKED,
                content = "테스트 방 모임에서 강퇴되었어요.",
                chatRoomId = 10L,
                referenceId = 44L,
            )
        val history =
            ChatRoomKickHistory(
                id = 44L,
                chatRoomId = 10L,
                roomTitle = "테스트 방",
                kickedUser = kickedUser,
                kickedBy = host,
                reason = "반복적인 약속 불이행",
            ).withCreatedAt(kickedAt)
        `when`(notificationRepository.findByIdAndRecipientId(101L, 2L)).thenReturn(notification)
        `when`(kickHistoryRepository.findByIdAndKickedUserId(44L, 2L)).thenReturn(history)

        val response = service.getKickHistory(2L, 101L)

        assertEquals(44L, response.kickHistoryId)
        assertEquals(10L, response.roomId)
        assertEquals("테스트 방", response.roomTitle)
        assertEquals("반복적인 약속 불이행", response.reason)
        assertEquals(kickedAt, response.kickedAt)
        verify(kickHistoryRepository).findByIdAndKickedUserId(44L, 2L)
    }

    @Test
    fun `강퇴 시 당사자에게 강퇴 이력을 참조하는 알림을 생성한다`() {
        val host = user(1L, "호스트")
        val kickedUser = user(2L, "강퇴된 사용자")
        val history =
            ChatRoomKickHistory(
                id = 44L,
                chatRoomId = 10L,
                roomTitle = "테스트 방",
                kickedUser = kickedUser,
                kickedBy = host,
                reason = "반복적인 약속 불이행",
            )
        `when`(notificationRepository.existsByRecipientIdAndTypeAndReferenceId(2L, NotificationType.CHAT_ROOM_KICKED, 44L))
            .thenReturn(false)
        `when`(notificationRepository.save(any(Notification::class.java)))
            .thenAnswer { (it.arguments[0] as Notification).withCreatedAt(LocalDateTime.of(2026, 8, 23, 12, 0)) }
        val notificationCaptor = ArgumentCaptor.forClass(Notification::class.java)

        service.notifyChatRoomMemberKicked(history)

        verify(notificationRepository).save(notificationCaptor.capture())
        assertEquals(2L, notificationCaptor.value.recipient.id)
        assertEquals(NotificationType.CHAT_ROOM_KICKED, notificationCaptor.value.type)
        assertEquals("테스트 방 모임에서 강퇴되었어요.", notificationCaptor.value.content)
        assertEquals(10L, notificationCaptor.value.chatRoomId)
        assertEquals(44L, notificationCaptor.value.referenceId)
        verify(pushNotificationSender).send(notificationCaptor.value)
    }

    @Test
    fun `채팅방 알림 설정 조회는 참가자의 저장값 또는 기본 활성값을 반환한다`() {
        val recipient = user(2L, "받는이")
        val room = room(user(1L, "호스트"))
        val participant = ChatRoomParticipant(chatRoom = room, user = recipient, role = ChatParticipantRole.MEMBER)
        `when`(participantRepository.findByChatRoomIdAndUserId(10L, 2L)).thenReturn(participant)
        `when`(roomSettingRepository.findByUserIdAndChatRoomId(2L, 10L))
            .thenReturn(null, ChatRoomNotificationSetting(user = recipient, chatRoom = room, enabled = false))

        assertTrue(service.getChatRoomSetting(2L, 10L).enabled)
        assertFalse(service.getChatRoomSetting(2L, 10L).enabled)
    }

    @Test
    fun `전체 알림 설정을 정상 값으로 갱신한다`() {
        val recipient = user(2L, "받는이")
        val setting = NotificationSetting(user = recipient)
        `when`(settingRepository.findByUserId(2L)).thenReturn(setting)

        val response =
            service.updateSetting(
                userId = 2L,
                chatNotificationMode = ChatNotificationMode.NONE,
                recruitmentDeadlineEnabled = false,
                socialActivityEnabled = false,
                marketingEnabled = true,
                doNotDisturbEnabled = true,
                doNotDisturbStartTime = LocalTime.of(22, 0),
                doNotDisturbEndTime = LocalTime.of(7, 0),
                doNotDisturbDays = setOf(DayOfWeek.MONDAY),
            )

        assertTrue(response.doNotDisturbEnabled)
        assertEquals(LocalTime.of(22, 0), response.doNotDisturbStartTime)
        assertEquals(setOf(DayOfWeek.MONDAY), response.doNotDisturbDays)
        assertEquals(ChatNotificationMode.NONE, setting.chatNotificationMode)
    }

    @Test
    fun `코스와 집합 정보 변경 알림은 호스트를 제외한 멤버에게 저장한다`() {
        val host = user(1L, "호스트")
        val member = user(2L, "멤버")
        val room = room(host)
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER),
                ),
            )
        stubNotificationSave()

        service.notifyCourseUpdated(room, 101L)
        service.notifyMeetingInfoUpdated(room, 102L)

        val captor = ArgumentCaptor.forClass(Notification::class.java)
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture())
        assertEquals(
            listOf(NotificationType.TRAVEL_COURSE_UPDATED, NotificationType.MEETING_INFO_UPDATED),
            captor.allValues.map { it.type },
        )
        assertTrue(captor.allValues.all { it.recipient.id == 2L })
    }

    @Test
    fun `모집 마감 알림은 현재 인원과 디데이를 모든 참가자에게 안내한다`() {
        val host = user(1L, "호스트")
        val member = user(2L, "멤버")
        val room = room(host)
        `when`(participantRepository.countByChatRoomId(10L)).thenReturn(2L)
        `when`(participantRepository.findAllByChatRoomIdOrderByCreatedDateTimeAsc(10L))
            .thenReturn(
                listOf(
                    ChatRoomParticipant(chatRoom = room, user = host, role = ChatParticipantRole.HOST),
                    ChatRoomParticipant(chatRoom = room, user = member, role = ChatParticipantRole.MEMBER),
                ),
            )
        stubNotificationSave()

        service.notifyRecruitmentDeadline(room)

        val captor = ArgumentCaptor.forClass(Notification::class.java)
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture())
        assertTrue(captor.allValues.all { it.type == NotificationType.RECRUITMENT_DEADLINE })
        assertTrue(captor.allValues.all { it.content.contains("현재 2/3명") })
    }

    @Test
    fun `피드 좋아요와 친구 신청 수락 이벤트를 각각 알림으로 저장한다`() {
        val author = user(1L, "작성자")
        val actor = user(2L, "좋아요 사용자")
        val receiver = user(3L, "친구 신청 수신자")
        val feed = mock(Feed::class.java)
        `when`(feed.id).thenReturn(20L)
        `when`(feed.author).thenReturn(author)
        `when`(feed.chatRoom).thenReturn(room(author))
        val friendRequest = FriendRequest(id = 30L, requester = actor, receiver = receiver)
        val friendship = Friendship(id = 40L, firstUser = author, secondUser = actor)
        stubNotificationSave()

        service.notifyFeedLiked(feed, actor)
        service.notifyFriendRequested(friendRequest)
        service.notifyFriendAccepted(friendship, actor)

        val captor = ArgumentCaptor.forClass(Notification::class.java)
        verify(notificationRepository, org.mockito.Mockito.times(3)).save(captor.capture())
        assertEquals(
            listOf(NotificationType.FEED_LIKE, NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPTED),
            captor.allValues.map { it.type },
        )
    }

    @Test
    fun `프로필이 없는 사용자의 소셜 알림에는 사용자 ID를 표시한다`() {
        val author = User(id = 1L, userRole = UserRole.ROLE_USER)
        val actor = User(id = 2L, userRole = UserRole.ROLE_USER)
        val receiver = User(id = 3L, userRole = UserRole.ROLE_USER)
        val feed = mock(Feed::class.java)
        `when`(feed.id).thenReturn(20L)
        `when`(feed.author).thenReturn(author)
        `when`(feed.chatRoom).thenReturn(room(author))
        val friendRequest = FriendRequest(id = 30L, requester = actor, receiver = receiver)
        val friendship = Friendship(id = 40L, firstUser = author, secondUser = actor)
        stubNotificationSave()

        service.notifyFeedLiked(feed, actor)
        service.notifyFriendRequested(friendRequest)
        service.notifyFriendAccepted(friendship, actor)

        val captor = ArgumentCaptor.forClass(Notification::class.java)
        verify(notificationRepository, org.mockito.Mockito.times(3)).save(captor.capture())
        assertTrue(captor.allValues.all { it.content.contains("사용자 2") })
    }

    private fun stubNotificationSave() {
        `when`(notificationRepository.save(any(Notification::class.java)))
            .thenAnswer { (it.arguments[0] as Notification).withCreatedAt(LocalDateTime.now()) }
    }

    private fun user(
        id: Long,
        nickname: String,
    ) = User(
        id = id,
        userRole = UserRole.ROLE_USER,
        userInformation = UserInformation(nickname, NicknameColor.GREEN, kr.hanchae.moyeotrip.entity.user.Gender.N),
    )

    private fun notification(
        id: Long,
        recipient: User,
    ) = Notification(
        id = id,
        recipient = recipient,
        type = NotificationType.FRIEND_REQUEST,
        content = "알림 $id",
        referenceId = id,
    ).withCreatedAt(LocalDateTime.of(2026, 8, 23, 12, 0).plusMinutes(id))

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

    private fun <T : BaseTimeEntity> T.withCreatedAt(createdAt: LocalDateTime): T {
        BaseTimeEntity::class.java
            .getDeclaredField("createdDateTime")
            .apply { isAccessible = true }
            .set(this, createdAt)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyValue(): T = org.mockito.Mockito.any<T>()
}
