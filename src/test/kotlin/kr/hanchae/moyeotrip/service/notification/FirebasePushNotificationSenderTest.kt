package kr.hanchae.moyeotrip.service.notification

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import kr.hanchae.moyeotrip.entity.notification.Notification
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class FirebasePushNotificationSenderTest {
    private val firebaseMessaging = mock(FirebaseMessaging::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val sender = FirebasePushNotificationSender(firebaseMessaging, userRepository)

    @Test
    fun `채팅방 생성 외 모든 알림 타입을 Android와 iOS 설정을 포함해 FCM으로 전송한다`() {
        val recipient = userWithToken()
        val pushTypes = NotificationType.entries.filterNot { it == NotificationType.CHAT_ROOM_CREATED }

        pushTypes.forEachIndexed { index, type ->
            sender.send(notification(index.toLong() + 1L, recipient, type))
        }

        val captor = ArgumentCaptor.forClass(Message::class.java)
        verify(firebaseMessaging, times(pushTypes.size)).send(captor.capture())
        assertEquals(pushTypes.map(NotificationType::name), captor.allValues.map { it.data()["notificationType"] })
        captor.allValues.forEach { message ->
            assertEquals("fcm-token", message.field<String>("token"))
            assertNotNull(message.field<AndroidConfig>("androidConfig"))
            assertNotNull(message.field<ApnsConfig>("apnsConfig"))
        }

        val message = captor.allValues.first()
        assertEquals("1", message.data()["notificationId"])
        assertEquals("10", message.data()["chatRoomId"])
        assertEquals("101", message.data()["referenceId"])
        val androidNotification = message.field<AndroidConfig>("androidConfig").field<AndroidNotification>("notification")
        assertEquals(FirebasePushNotificationSender.ANDROID_CHANNEL_ID, androidNotification.field<String>("channelId"))
        assertEquals("default", androidNotification.field<String>("sound"))
        val apnsConfig = message.field<ApnsConfig>("apnsConfig")
        val aps = apnsConfig.field<Map<String, Any>>("payload")["aps"] as Map<*, *>
        assertEquals("default", aps["sound"])
        assertEquals("alert", apnsConfig.field<Map<String, String>>("headers")["apns-push-type"])
        assertEquals("10", apnsConfig.field<Map<String, String>>("headers")["apns-priority"])
    }

    @Test
    fun `채팅방 생성 알림과 FCM 토큰이 없는 사용자는 푸시를 전송하지 않는다`() {
        val recipient = userWithToken()
        sender.send(notification(1L, recipient, NotificationType.CHAT_ROOM_CREATED))
        recipient.clearFcmTokenIfMatches("fcm-token")
        sender.send(notification(2L, recipient, NotificationType.FRIEND_REQUEST))

        verifyNoInteractions(firebaseMessaging, userRepository)
    }

    @Test
    fun `FCM에서 등록 해제된 토큰이라고 응답하면 현재 토큰을 제거한다`() {
        val recipient = userWithToken()
        val exception = mock(FirebaseMessagingException::class.java)
        `when`(exception.messagingErrorCode).thenReturn(MessagingErrorCode.UNREGISTERED)
        `when`(firebaseMessaging.send(any(Message::class.java))).thenThrow(exception)

        sender.send(notification(1L, recipient, NotificationType.FRIEND_REQUEST))

        assertNull(recipient.fcmToken)
        verify(userRepository).save(recipient)
    }

    @Test
    fun `일시적인 FCM 오류는 토큰을 제거하거나 호출자에게 전파하지 않는다`() {
        val recipient = userWithToken()
        val exception = mock(FirebaseMessagingException::class.java)
        `when`(exception.messagingErrorCode).thenReturn(MessagingErrorCode.UNAVAILABLE)
        `when`(firebaseMessaging.send(any(Message::class.java))).thenThrow(exception)

        sender.send(notification(1L, recipient, NotificationType.FRIEND_REQUEST))

        assertEquals("fcm-token", recipient.fcmToken)
        verify(userRepository, never()).save(recipient)
    }

    private fun userWithToken() =
        User(id = 2L, userRole = UserRole.ROLE_USER).also {
            it.changeFcmToken("fcm-token")
        }

    private fun notification(
        id: Long,
        recipient: User,
        type: NotificationType,
    ) = Notification(
        id = id,
        recipient = recipient,
        type = type,
        content = "푸시 알림 본문",
        chatRoomId = 10L,
        referenceId = 100L + id,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.field(name: String): T =
        javaClass
            .getDeclaredField(name)
            .apply { isAccessible = true }
            .get(this) as T

    private fun Message.data(): Map<String, String> = field("data")
}
