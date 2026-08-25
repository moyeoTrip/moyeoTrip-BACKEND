package kr.hanchae.moyeotrip.service.notification

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import kr.hanchae.moyeotrip.entity.notification.Notification
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import kr.hanchae.moyeotrip.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface PushNotificationSender {
    fun send(notification: Notification)
}

@Service
class FirebasePushNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
    private val userRepository: UserRepository,
) : PushNotificationSender {
    override fun send(notification: Notification) {
        if (notification.type == NotificationType.CHAT_ROOM_CREATED) return
        val token =
            notification.recipient.fcmToken
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return

        try {
            firebaseMessaging.send(notification.toFirebaseMessage(token))
        } catch (exception: FirebaseMessagingException) {
            if (exception.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                removeInvalidToken(notification, token)
                logger.info("등록 해제된 FCM 토큰을 사용자 정보에서 제거했습니다. userId={}", notification.recipient.id)
            } else {
                logger.warn(
                    "FCM 푸시 전송에 실패했습니다. userId={}, notificationType={}, errorCode={}",
                    notification.recipient.id,
                    notification.type,
                    exception.messagingErrorCode,
                    exception,
                )
            }
        } catch (exception: RuntimeException) {
            logger.warn(
                "FCM 푸시 메시지를 생성하거나 전송하지 못했습니다. userId={}, notificationType={}",
                notification.recipient.id,
                notification.type,
                exception,
            )
        }
    }

    private fun removeInvalidToken(
        notification: Notification,
        token: String,
    ) {
        if (notification.recipient.clearFcmTokenIfMatches(token)) {
            userRepository.save(notification.recipient)
        }
    }

    private fun Notification.toFirebaseMessage(token: String): Message {
        val data =
            buildMap {
                put(DATA_NOTIFICATION_ID, id.toString())
                put(DATA_NOTIFICATION_TYPE, type.name)
                put(DATA_REFERENCE_ID, referenceId.toString())
                chatRoomId?.let { put(DATA_CHAT_ROOM_ID, it.toString()) }
            }
        return Message
            .builder()
            .setToken(token)
            .setNotification(
                com.google.firebase.messaging.Notification
                    .builder()
                    .setTitle(type.pushTitle())
                    .setBody(content)
                    .build(),
            ).putAllData(data)
            .setAndroidConfig(
                AndroidConfig
                    .builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification
                            .builder()
                            .setChannelId(ANDROID_CHANNEL_ID)
                            .setSound(DEFAULT_SOUND)
                            .build(),
                    ).build(),
            ).setApnsConfig(
                ApnsConfig
                    .builder()
                    .putHeader(APNS_PUSH_TYPE_HEADER, APNS_PUSH_TYPE_ALERT)
                    .putHeader(APNS_PRIORITY_HEADER, APNS_PRIORITY_IMMEDIATE)
                    .setAps(
                        Aps
                            .builder()
                            .setSound(DEFAULT_SOUND)
                            .setThreadId(type.name)
                            .build(),
                    ).build(),
            ).build()
    }

    private fun NotificationType.pushTitle(): String =
        when (this) {
            NotificationType.CHAT_ROOM_CREATED -> error("채팅방 생성 알림은 푸시로 전송하지 않습니다.")
            NotificationType.CHAT_ROOM_KICKED -> "모임 알림"
            NotificationType.CHAT_MESSAGE_RECEIVED -> "새 메시지"
            NotificationType.TRAVEL_COURSE_UPDATED,
            NotificationType.MEETING_INFO_UPDATED,
            NotificationType.RECRUITMENT_DEADLINE,
            -> "여행 알림"
            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED,
            -> "친구 알림"
            NotificationType.FEED_LIKE -> "피드 알림"
            NotificationType.MARKETING -> "모여트립"
        }

    companion object {
        const val ANDROID_CHANNEL_ID = "moyeotrip_notifications"
        private const val DEFAULT_SOUND = "default"
        private const val APNS_PUSH_TYPE_HEADER = "apns-push-type"
        private const val APNS_PUSH_TYPE_ALERT = "alert"
        private const val APNS_PRIORITY_HEADER = "apns-priority"
        private const val APNS_PRIORITY_IMMEDIATE = "10"
        private const val DATA_NOTIFICATION_ID = "notificationId"
        private const val DATA_NOTIFICATION_TYPE = "notificationType"
        private const val DATA_REFERENCE_ID = "referenceId"
        private const val DATA_CHAT_ROOM_ID = "chatRoomId"
        private val logger = LoggerFactory.getLogger(FirebasePushNotificationSender::class.java)
    }
}
