package kr.hanchae.moyeotrip.service.realtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationResponse
import org.redisson.api.RTopic
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
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
        publish(
            RealtimeRedisEvent(
                type = RealtimeEventType.CHAT_MESSAGE,
                targetId = roomId,
                payload = objectMapper.valueToTree(message),
            ),
        )
    }

    fun sendNotification(
        userId: Long,
        notification: NotificationResponse,
    ) {
        publish(
            RealtimeRedisEvent(
                type = RealtimeEventType.NOTIFICATION,
                targetId = userId,
                payload = objectMapper.valueToTree(notification),
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
                        topic.publish(json)
                    }
                },
            )
        } else {
            topic.publish(json)
        }
    }

    private fun deliverToLocalWebSocketSessions(event: RealtimeRedisEvent) {
        when (event.type) {
            RealtimeEventType.CHAT_MESSAGE ->
                messagingTemplate.convertAndSend("/topic/chat-rooms/${event.targetId}/messages", event.payload)

            RealtimeEventType.NOTIFICATION ->
                messagingTemplate.convertAndSendToUser(event.targetId.toString(), "/queue/notifications", event.payload)
        }
    }

    companion object {
        private const val REALTIME_CHANNEL = "moyeotrip:realtime-events"
    }
}

private data class RealtimeRedisEvent(
    val type: RealtimeEventType,
    val targetId: Long,
    val payload: JsonNode,
)

private enum class RealtimeEventType {
    CHAT_MESSAGE,
    NOTIFICATION,
}
