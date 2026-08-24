package kr.hanchae.moyeotrip.controller.chat

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatWebSocketController(
    private val chatRoomService: ChatRoomService,
    private val sentryExceptionReporter: SentryExceptionReporter,
) {
    @MessageMapping("/chat-rooms/{roomId}/messages")
    fun sendMessage(
        principal: Principal,
        @DestinationVariable roomId: Long,
        @Valid request: SendChatMessageRequest,
    ) {
        try {
            chatRoomService.sendMessage(principal.name.toLong(), roomId, request)
        } catch (exception: Exception) {
            sentryExceptionReporter.capture(exception, WEBSOCKET_MESSAGE_TAGS)
            throw exception
        }
    }

    companion object {
        private val WEBSOCKET_MESSAGE_TAGS =
            mapOf(
                "transport" to "websocket",
                "message.destination" to "/app/chat-rooms/{roomId}/messages",
            )
    }
}
