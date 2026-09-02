package kr.hanchae.moyeotrip.config.websocket

import kr.hanchae.moyeotrip.config.properties.JwtProperties
import kr.hanchae.moyeotrip.config.properties.WebCorsProperties
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RedissonClient
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.GenericMessage
import org.springframework.messaging.support.MessageBuilder
import java.security.Principal
import java.util.Base64

class WebSocketConfigTest {
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val sentryExceptionReporter = mock(SentryExceptionReporter::class.java)
    private val jwtUtil =
        JwtUtil(
            JwtProperties(
                accessKey = encodedKey(),
                refreshKey = encodedKey(),
                accessTokenExpirationTime = 60_000,
                refreshTokenExpirationTime = 120_000,
            ),
            mock(RedissonClient::class.java),
        )
    private val config =
        WebSocketConfig(
            jwtUtil,
            participantRepository,
            WebCorsProperties(allowedOrigins = listOf("https://example.com")),
            sentryExceptionReporter,
        )
    private val interceptor: ChannelInterceptor =
        ExposedChannelRegistration()
            .also(config::configureClientInboundChannel)
            .registeredInterceptors()
            .single()
    private val channel = mock(MessageChannel::class.java)

    @Test
    fun `STOMP 헤더가 없는 메시지는 그대로 통과한다`() {
        val message = GenericMessage("payload")

        assertSame(message, interceptor.preSend(message, channel))
    }

    @Test
    fun `CONNECT는 Bearer access token이 필요하다`() {
        val exceptions =
            listOf(null, "", "Basic credential").map { authorization ->
                val message = stompMessage(StompCommand.CONNECT, authorization = authorization)
                assertThrows(IllegalArgumentException::class.java) { interceptor.preSend(message, channel) }
            }

        exceptions.forEach { exception ->
            verify(sentryExceptionReporter).capture(exception, mapOf("transport" to "websocket"))
        }
    }

    @Test
    fun `CONNECT는 유효하지 않은 access token을 거부한다`() {
        val message = stompMessage(StompCommand.CONNECT, authorization = "Bearer invalid-token")

        assertThrows(IllegalArgumentException::class.java) { interceptor.preSend(message, channel) }
    }

    @Test
    fun `CONNECT 인증 성공 시 사용자 ID principal을 저장한다`() {
        val token = jwtUtil.generateAccessToken(42L, "테스터")
        val message = stompMessage(StompCommand.CONNECT, authorization = "Bearer $token")

        val result = interceptor.preSend(message, channel)!!
        val accessor = StompHeaderAccessor.wrap(result)

        assertEquals("42", accessor.user?.name)
    }

    @Test
    fun `채팅 메시지 구독은 해당 방 참가자만 허용한다`() {
        `when`(participantRepository.existsByChatRoomIdAndUserId(10L, 42L)).thenReturn(true)
        val allowed = stompMessage(StompCommand.SUBSCRIBE, destination = "/topic/chat-rooms/10/messages", userId = "42")

        assertSame(allowed, interceptor.preSend(allowed, channel))
        verify(participantRepository).existsByChatRoomIdAndUserId(10L, 42L)

        `when`(participantRepository.existsByChatRoomIdAndUserId(11L, 42L)).thenReturn(false)
        val denied = stompMessage(StompCommand.SUBSCRIBE, destination = "/topic/chat-rooms/11/messages", userId = "42")
        val exception = assertThrows(IllegalArgumentException::class.java) { interceptor.preSend(denied, channel) }
        assertTrue(exception.message.orEmpty().contains("참가자"))
    }

    @Test
    fun `채팅 구독에 인증 사용자 정보가 없거나 숫자가 아니면 거부한다`() {
        val missing = stompMessage(StompCommand.SUBSCRIBE, destination = "/topic/chat-rooms/10/messages")
        val malformed = stompMessage(StompCommand.SUBSCRIBE, destination = "/topic/chat-rooms/10/messages", userId = "invalid")

        assertThrows(IllegalStateException::class.java) { interceptor.preSend(missing, channel) }
        assertThrows(IllegalStateException::class.java) { interceptor.preSend(malformed, channel) }
    }

    @Test
    fun `채팅 메시지 외 구독과 SEND 명령은 참가 여부 확인 없이 통과한다`() {
        val notification = stompMessage(StompCommand.SUBSCRIBE, destination = "/user/queue/notifications", userId = "42")
        val poll = stompMessage(StompCommand.SUBSCRIBE, destination = "/topic/chat-rooms/10/polls", userId = "42")
        val send = stompMessage(StompCommand.SEND, destination = "/app/chat-rooms/10/messages", userId = "42")

        assertSame(notification, interceptor.preSend(notification, channel))
        assertSame(poll, interceptor.preSend(poll, channel))
        assertSame(send, interceptor.preSend(send, channel))
        verify(participantRepository, never()).existsByChatRoomIdAndUserId(any(Long::class.java), any(Long::class.java))
    }

    private fun stompMessage(
        command: StompCommand,
        authorization: String? = null,
        destination: String? = null,
        userId: String? = null,
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(command)
        authorization?.let { accessor.setNativeHeader("Authorization", it) }
        accessor.destination = destination
        accessor.user = userId?.let { Principal { it } }
        accessor.setLeaveMutable(true)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    private class ExposedChannelRegistration : ChannelRegistration() {
        fun registeredInterceptors(): List<ChannelInterceptor> = interceptors
    }

    companion object {
        private fun encodedKey(): String = Base64.getEncoder().encodeToString("01234567890123456789012345678901".toByteArray())
    }
}
