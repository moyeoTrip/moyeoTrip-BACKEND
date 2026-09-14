package kr.hanchae.moyeotrip.service.realtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedOptionResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatPollUpdatedResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RTopic
import org.redisson.api.RedissonClient
import org.redisson.api.listener.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime

class RealtimeMessagingServiceTest {
    private val redissonClient = mock(RedissonClient::class.java)
    private val topic = mock(RTopic::class.java)
    private val messagingTemplate = mock(SimpMessagingTemplate::class.java)
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val service = RealtimeMessagingService(redissonClient, objectMapper, messagingTemplate)

    @AfterEach
    fun clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    @Test
    fun `Redis 구독 메시지를 채팅방 웹소켓 구독자에게 전달하고 해제한다`() {
        val listener = subscribe()
        service.sendChatMessage(10L, chatMessage())

        listener.onMessage("moyeotrip:realtime-events", publishedJson())
        service.unsubscribe()

        // 본문은 **내용이 담긴 Map** 이어야 한다. JsonNode 를 그대로 보내면 내용 없이 나간다.
        val body = sentBody("/topic/chat-rooms/10/messages")
        assertEquals("안녕하세요", body["content"])
        verify(topic).removeListener(7)
    }

    @Test
    fun `Redis 알림 이벤트를 해당 사용자의 개인 큐로 전달한다`() {
        val listener = subscribe()
        val notification =
            NotificationResponse(1L, NotificationType.FRIEND_REQUEST, "친구 신청", null, 3L, false, LocalDateTime.now())
        service.sendNotification(2L, notification)

        listener.onMessage("moyeotrip:realtime-events", publishedJson())

        val body = ArgumentCaptor.forClass(Any::class.java)
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/notifications"), body.capture())
        @Suppress("UNCHECKED_CAST")
        assertEquals("친구 신청", (body.value as Map<String, Any?>)["content"])
    }

    @Test
    fun `Redis 투표 갱신 이벤트를 채팅방 투표 웹소켓 구독자에게 전달한다`() {
        val listener = subscribe()
        val poll =
            ChatPollUpdatedResponse(
                messageId = 501L,
                totalVoteCount = 1,
                options = listOf(ChatPollUpdatedOptionResponse(23L, 1, null)),
            )
        service.sendChatPollUpdated(10L, poll)

        listener.onMessage("moyeotrip:realtime-events", publishedJson())

        val body = sentBody("/topic/chat-rooms/10/polls")
        assertEquals(501L, (body["messageId"] as Number).toLong())
    }

    @Test
    fun `Redis 발행 실패가 채팅 REST 요청의 실패로 전파되지 않는다`() {
        subscribe()
        `when`(topic.publish(anyString())).thenThrow(IllegalStateException("Redis unavailable"))

        assertDoesNotThrow { service.sendChatMessage(101L, systemMessage()) }
    }

    @Test
    fun `트랜잭션 중 생성된 이벤트는 커밋 후 Redis에 발행한다`() {
        subscribe()
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)

        service.sendChatMessage(10L, chatMessage())

        verify(topic, never()).publish(anyString())
        val synchronizations = TransactionSynchronizationManager.getSynchronizations()
        assertEquals(1, synchronizations.size)
        synchronizations.single().afterCommit()
        verify(topic).publish(anyString())
    }

    @Test
    fun `트랜잭션 중 생성한 이벤트는 커밋 후 발행하며 Redis 실패를 전파하지 않는다`() {
        subscribe()
        `when`(topic.publish(anyString())).thenThrow(IllegalStateException("Redis unavailable"))
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)

        service.sendChatMessage(101L, systemMessage())

        verify(topic, never()).publish(anyString())
        val synchronization = TransactionSynchronizationManager.getSynchronizations().single()
        assertDoesNotThrow { synchronization.afterCommit() }
        verify(topic).publish(anyString())
    }

    @Test
    fun `해석할 수 없는 Redis 메시지는 웹소켓으로 전달하지 않는다`() {
        val listener = subscribe()

        listener.onMessage("moyeotrip:realtime-events", "not-json")

        verify(messagingTemplate, never()).convertAndSend(any(String::class.java), any<Any>())
    }

    @Test
    fun `구독 해제 시 등록한 Redis 리스너를 제거한다`() {
        subscribe()

        service.unsubscribe()

        verify(topic).removeListener(7)
    }

    private fun subscribe(): MessageListener<String> {
        `when`(redissonClient.getTopic(eq("moyeotrip:realtime-events"), any())).thenReturn(topic)
        val captor = listenerCaptor()
        `when`(topic.addListener(eq(String::class.java), captor.capture())).thenReturn(7)
        service.subscribe()
        return captor.value
    }

    @Test
    fun `웹소켓으로 보내는 본문은 메시지 내용이어야 한다`() {
        // JsonNode 를 그대로 convertAndSend 에 넘기면 변환기가 트리를 쓰지 않고 **JsonNode 를 POJO 로**
        // 직렬화해 `{"array":false,"nodeType":"OBJECT",…}` 가 나간다. 클라이언트는 연결도 되고
        // 프레임도 받는데 **내용만 비어 있다** — 실서버에서 실제로 그렇게 나가고 있었다(2026-09-14).
        val payload: JsonNode = objectMapper.valueToTree(chatMessage())

        val body = service.toSendableBody(payload)

        assertEquals(1L, (body["messageId"] as Number).toLong())
        assertEquals("안녕하세요", body["content"])
        assertEquals("여행자", body["senderNickname"])
        // JsonNode 의 getter 가 새어 나오면 안 된다.
        assertFalse(body.containsKey("nodeType"), "JsonNode 속성이 새어 나왔다: $body")
        assertFalse(body.containsKey("array"), "JsonNode 속성이 새어 나왔다: $body")
        assertFalse(body.containsKey("object"), "JsonNode 속성이 새어 나왔다: $body")
    }

    @Suppress("UNCHECKED_CAST")
    private fun sentBody(destination: String): Map<String, Any?> {
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(messagingTemplate).convertAndSend(eq(destination), captor.capture())
        return captor.value as Map<String, Any?>
    }

    private fun publishedJson(): String {
        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(topic).publish(captor.capture())
        return captor.value
    }

    private fun chatMessage() =
        ChatMessageResponse(
            messageId = 1L,
            type = ChatMessageType.USER,
            senderId = 2L,
            senderNickname = "여행자",
            content = "안녕하세요",
            createdAt = LocalDateTime.now(),
        )

    private fun systemMessage() =
        ChatMessageResponse(
            messageId = 1L,
            type = ChatMessageType.SYSTEM,
            senderId = null,
            senderNickname = "시스템",
            content = "테스트 메시지",
            createdAt = LocalDateTime.of(2026, 8, 23, 12, 0),
        )

    @Suppress("UNCHECKED_CAST")
    private fun listenerCaptor(): ArgumentCaptor<MessageListener<String>> =
        ArgumentCaptor.forClass(MessageListener::class.java) as ArgumentCaptor<MessageListener<String>>
}
