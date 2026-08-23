package kr.hanchae.moyeotrip.service.realtime

import kr.hanchae.moyeotrip.config.JacksonConfig
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RTopic
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime

class RealtimeMessagingServiceTest {
    private val redissonClient = mock(RedissonClient::class.java)
    private val topic = mock(RTopic::class.java)
    private val messagingTemplate = mock(SimpMessagingTemplate::class.java)
    private val service =
        RealtimeMessagingService(
            redissonClient = redissonClient,
            objectMapper = JacksonConfig().objectMapper(),
            messagingTemplate = messagingTemplate,
        )

    @Test
    fun `Redis 발행 실패가 채팅 REST 요청의 실패로 전파되지 않는다`() {
        subscribe()
        `when`(topic.publish(anyString())).thenThrow(IllegalStateException("Redis unavailable"))

        assertDoesNotThrow { service.sendChatMessage(101L, message()) }
    }

    @Test
    fun `트랜잭션 중 생성한 이벤트는 커밋 후 발행하며 Redis 실패를 전파하지 않는다`() {
        subscribe()
        `when`(topic.publish(anyString())).thenThrow(IllegalStateException("Redis unavailable"))
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)

        try {
            service.sendChatMessage(101L, message())

            verify(topic, never()).publish(anyString())
            val synchronization = TransactionSynchronizationManager.getSynchronizations().single()
            assertDoesNotThrow { synchronization.afterCommit() }
            verify(topic).publish(anyString())
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
            TransactionSynchronizationManager.setActualTransactionActive(false)
        }
    }

    @Test
    fun `구독 해제 시 등록한 Redis 리스너를 제거한다`() {
        `when`(redissonClient.getTopic("moyeotrip:realtime-events", StringCodec.INSTANCE)).thenReturn(topic)
        `when`(topic.addListener(org.mockito.ArgumentMatchers.eq(String::class.java), org.mockito.ArgumentMatchers.any()))
            .thenReturn(7)

        service.subscribe()
        service.unsubscribe()

        verify(topic).removeListener(7)
    }

    private fun subscribe() {
        `when`(redissonClient.getTopic("moyeotrip:realtime-events", StringCodec.INSTANCE)).thenReturn(topic)
        service.subscribe()
    }

    private fun message() =
        ChatMessageResponse(
            messageId = 1L,
            type = ChatMessageType.SYSTEM,
            senderId = null,
            senderNickname = "시스템",
            content = "테스트 메시지",
            createdAt = LocalDateTime.of(2026, 8, 23, 12, 0),
        )
}
