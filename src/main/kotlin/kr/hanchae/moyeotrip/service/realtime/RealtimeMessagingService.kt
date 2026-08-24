package kr.hanchae.moyeotrip.service.realtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationResponse
import org.redisson.api.RTopic
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class RealtimeMessagingService(
    private val redissonClient: RedissonClient,
    private val objectMapper: ObjectMapper,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private lateinit var topic: RTopic
    private var listenerId: Int? = null

    @PostConstruct
    fun subscribe() {
        topic = redissonClient.getTopic(REALTIME_CHANNEL, StringCodec.INSTANCE)
        listenerId =
            topic.addListener(String::class.java) { _, json ->
                runCatching { objectMapper.readValue(json, RealtimeRedisEvent::class.java) }
                    .onSuccess(::deliverToLocalWebSocketSessions)
            }
    }

    @PreDestroy
    fun unsubscribe() {
        listenerId?.let { topic.removeListener(it) }
    }

    fun sendChatMessage(
        roomId: Long,
        message: ChatMessageResponse,
    ) {
        runCatching {
            publish(
                RealtimeRedisEvent(
                    type = RealtimeEventType.CHAT_MESSAGE,
                    targetId = roomId,
                    payload = objectMapper.valueToTree(message),
                ),
            )
        }.onFailure { exception ->
            logger.warn("채팅 실시간 이벤트 발행을 건너뜁니다. roomId={}", roomId, exception)
        }
    }

    fun sendNotification(
        userId: Long,
        notification: NotificationResponse,
    ) {
        runCatching {
            publish(
                RealtimeRedisEvent(
                    type = RealtimeEventType.NOTIFICATION,
                    targetId = userId,
                    payload = objectMapper.valueToTree(notification),
                ),
            )
        }.onFailure { exception ->
            logger.warn("알림 실시간 이벤트 발행을 건너뜁니다. userId={}", userId, exception)
        }
    }

    fun sendChatPollUpdated(
        roomId: Long,
        poll: ChatPollUpdatedResponse,
    ) {
        publish(
            RealtimeRedisEvent(
                type = RealtimeEventType.CHAT_POLL_UPDATED,
                targetId = roomId,
                payload = objectMapper.valueToTree(poll),
            ),
        )
    }

    private fun publish(event: RealtimeRedisEvent) {
        val json = objectMapper.writeValueAsString(event)
        if (TransactionSynchronizationManager.isActualTransactionActive() &&
            TransactionSynchronizationManager.isSynchronizationActive()
        ) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        publishToRedis(json)
                    }
                },
            )
        } else {
            publishToRedis(json)
        }
    }

    private fun publishToRedis(json: String) {
        runCatching { topic.publish(json) }
            .onFailure { exception -> logger.warn("Redis 실시간 이벤트 발행에 실패했습니다.", exception) }
    }

    private fun deliverToLocalWebSocketSessions(event: RealtimeRedisEvent) {
        when (event.type) {
            RealtimeEventType.CHAT_MESSAGE ->
                messagingTemplate.convertAndSend("/topic/chat-rooms/${event.targetId}/messages", event.payload)

            RealtimeEventType.CHAT_POLL_UPDATED ->
                messagingTemplate.convertAndSend("/topic/chat-rooms/${event.targetId}/polls", event.payload)

            RealtimeEventType.NOTIFICATION ->
                messagingTemplate.convertAndSendToUser(event.targetId.toString(), "/queue/notifications", event.payload)
        }
    }

    companion object {
        private const val REALTIME_CHANNEL = "moyeotrip:realtime-events"
        private val logger = LoggerFactory.getLogger(RealtimeMessagingService::class.java)
    }
}

private data class RealtimeRedisEvent(
    val type: RealtimeEventType,
    val targetId: Long,
    val payload: JsonNode,
)

private enum class RealtimeEventType {
    CHAT_MESSAGE,
    CHAT_POLL_UPDATED,
    NOTIFICATION,
}
