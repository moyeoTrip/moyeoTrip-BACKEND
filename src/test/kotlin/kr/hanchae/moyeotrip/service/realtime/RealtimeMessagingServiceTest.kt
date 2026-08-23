package kr.hanchae.moyeotrip.service.realtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
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
        val message = chatMessage()
        service.sendChatMessage(10L, message)
        val json = publishedJson()

        listener.onMessage("moyeotrip:realtime-events", json)
        service.unsubscribe()

        verify(messagingTemplate).convertAndSend(eq("/topic/chat-rooms/10/messages"), any(JsonNode::class.java))
        verify(topic).removeListener(7)
    }

    @Test
    fun `Redis 알림 이벤트를 해당 사용자의 개인 큐로 전달한다`() {
        val listener = subscribe()
        val notification =
            NotificationResponse(1L, NotificationType.FRIEND_REQUEST, "친구 신청", null, 3L, false, LocalDateTime.now())
        service.sendNotification(2L, notification)

        listener.onMessage("moyeotrip:realtime-events", publishedJson())

        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/notifications"), any(JsonNode::class.java))
    }

    @Test
    fun `트랜잭션 중 생성된 이벤트는 커밋 후 Redis에 발행한다`() {
        subscribe()
        TransactionSynchronizationManager.setActualTransactionActive(true)
        TransactionSynchronizationManager.initSynchronization()

        service.sendChatMessage(10L, chatMessage())

        verify(topic, never()).publish(any(String::class.java))
        val synchronizations = TransactionSynchronizationManager.getSynchronizations()
        assertEquals(1, synchronizations.size)
        synchronizations.single().afterCommit()
        verify(topic).publish(any(String::class.java))
    }

    @Test
    fun `해석할 수 없는 Redis 메시지는 웹소켓으로 전달하지 않는다`() {
        val listener = subscribe()

        listener.onMessage("moyeotrip:realtime-events", "not-json")

        verify(messagingTemplate, never()).convertAndSend(any(String::class.java), any<Any>())
    }

    private fun subscribe(): MessageListener<String> {
        `when`(redissonClient.getTopic(eq("moyeotrip:realtime-events"), any())).thenReturn(topic)
        val captor = listenerCaptor()
        `when`(topic.addListener(eq(String::class.java), captor.capture())).thenReturn(7)
        service.subscribe()
        return captor.value
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

    @Suppress("UNCHECKED_CAST")
    private fun listenerCaptor(): ArgumentCaptor<MessageListener<String>> =
        ArgumentCaptor.forClass(MessageListener::class.java) as ArgumentCaptor<MessageListener<String>>
}
