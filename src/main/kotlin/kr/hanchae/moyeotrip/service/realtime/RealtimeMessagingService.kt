package kr.hanchae.moyeotrip.service.realtime

import com.fasterxml.jackson.core.type.TypeReference
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
        val body = toSendableBody(event.payload)
        when (event.type) {
            RealtimeEventType.CHAT_MESSAGE ->
                messagingTemplate.convertAndSend("/topic/chat-rooms/${event.targetId}/messages", body)

            RealtimeEventType.CHAT_POLL_UPDATED ->
                messagingTemplate.convertAndSend("/topic/chat-rooms/${event.targetId}/polls", body)

            RealtimeEventType.NOTIFICATION ->
                messagingTemplate.convertAndSendToUser(event.targetId.toString(), "/queue/notifications", body)
        }
    }

    /**
     * `JsonNode` 를 **그대로 보내면 안 된다.**
     *
     * 메시지 변환기가 `JsonNode` 를 일반 객체로 보고 getter 를 직렬화해 버려,
     * 실제 내용 대신 `{"array":false,"object":true,"nodeType":"OBJECT",…}` 가 나간다.
     * 클라이언트는 **연결도 되고 프레임도 받는데 내용만 비어 있는** 상태가 된다 —
     * 실서버에서 그렇게 나가고 있었다(2026-09-14 안드로이드 QA 에서 발견).
     *
     * `Map` 으로 바꿔 보내면 어떤 변환기를 쓰든 내용 그대로 직렬화된다.
     */
    internal fun toSendableBody(payload: JsonNode): Map<String, Any?> = objectMapper.convertValue(payload, MAP_TYPE)

    companion object {
        private val MAP_TYPE = object : TypeReference<Map<String, Any?>>() {}
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
