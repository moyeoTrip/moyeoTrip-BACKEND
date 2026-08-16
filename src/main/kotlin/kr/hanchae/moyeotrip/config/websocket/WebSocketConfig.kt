package kr.hanchae.moyeotrip.config.websocket

import kr.hanchae.moyeotrip.config.properties.WebCorsProperties
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import kr.hanchae.moyeotrip.utils.jwt.isBearerToken
import kr.hanchae.moyeotrip.utils.jwt.removeBearer
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.security.Principal

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtUtil: JwtUtil,
    private val participantRepository: ChatRoomParticipantRepository,
    private val corsProperties: WebCorsProperties,
) : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns(*corsProperties.allowedOrigins.toTypedArray())
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(
            object : ChannelInterceptor {
                override fun preSend(
                    message: Message<*>,
                    channel: MessageChannel,
                ): Message<*> {
                    val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
                    when (accessor.command) {
                        StompCommand.CONNECT -> authenticate(accessor)
                        StompCommand.SUBSCRIBE -> authorizeSubscription(accessor)
                        else -> Unit
                    }
                    return message
                }
            },
        )
    }

    private fun authenticate(accessor: StompHeaderAccessor) {
        val authorization = accessor.getFirstNativeHeader("Authorization")
        require(!authorization.isNullOrBlank() && authorization.isBearerToken()) { "WebSocket 인증이 필요합니다." }
        val token = authorization.removeBearer()
        require(jwtUtil.validateToken(jwtUtil.accessKey, token)) { "유효하지 않은 WebSocket 인증 토큰입니다." }
        val userId = jwtUtil.getUserId(jwtUtil.accessKey, token)
        accessor.user = Principal { userId.toString() }
    }

    private fun authorizeSubscription(accessor: StompHeaderAccessor) {
        val roomId =
            CHAT_ROOM_TOPIC
                .matchEntire(accessor.destination.orEmpty())
                ?.groupValues
                ?.get(1)
                ?.toLong() ?: return
        val userId = accessor.user?.name?.toLongOrNull() ?: error("WebSocket 인증이 필요합니다.")
        require(participantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            "채팅방 참가자만 채팅 메시지를 구독할 수 있습니다."
        }
    }

    companion object {
        private val CHAT_ROOM_TOPIC = Regex("/topic/chat-rooms/(\\d+)/messages")
    }
}
