package kr.hanchae.moyeotrip.config.websocket

import kr.hanchae.moyeotrip.config.properties.WebCorsProperties
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
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

/**
 * STOMP 핸드셰이크 경로.
 *
 * **인그레스(`deployment/ingress.yml`)에 이 경로가 함께 있어야 한다.** 예전에는 인그레스가
 * `/api`·`/swagger-ui`·`/api-docs` 만 서비스로 보내서, **구현은 다 돼 있는데도 운영에서 404** 였다.
 * 그래서 세 클라이언트가 전부 폴링으로 돌아갔다(QA `BE-27` — iOS 는 채팅 화면이 열려 있는 동안
 * 5초마다 재조회하고 있었다).
 *
 * HTTP 단계는 `PERMITTED_URL_PATTERNS` 로 열어 두고, **실제 인증은 STOMP `CONNECT` 프레임의
 * `Authorization` 헤더**에서 한다([WebSocketConfig.authenticate]).
 */
const val STOMP_ENDPOINT = "/ws"

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtUtil: JwtUtil,
    private val participantRepository: ChatRoomParticipantRepository,
    private val corsProperties: WebCorsProperties,
    private val sentryExceptionReporter: SentryExceptionReporter,
) : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint(STOMP_ENDPOINT)
            .setAllowedOriginPatterns(*corsProperties.allowedOrigins.toTypedArray())
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(
            object : ChannelInterceptor {
                override fun preSend(
                    message: Message<*>,
                    channel: MessageChannel,
                ): Message<*> =
                    try {
                        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
                        when (accessor.command) {
                            StompCommand.CONNECT -> authenticate(accessor)
                            StompCommand.SUBSCRIBE -> authorizeSubscription(accessor)
                            else -> Unit
                        }
                        message
                    } catch (exception: RuntimeException) {
                        sentryExceptionReporter.capture(exception, WEBSOCKET_TAGS)
                        throw exception
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
        private val WEBSOCKET_TAGS = mapOf("transport" to "websocket")
    }
}
