package kr.hanchae.moyeotrip.controller.chat

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatWebSocketController(
    private val chatRoomService: ChatRoomService,
) {
    @MessageMapping("/chat-rooms/{roomId}/messages")
    fun sendMessage(
        principal: Principal,
        @DestinationVariable roomId: Long,
        @Valid request: SendChatMessageRequest,
    ) {
        chatRoomService.sendMessage(principal.name.toLong(), roomId, request)
    }
}
