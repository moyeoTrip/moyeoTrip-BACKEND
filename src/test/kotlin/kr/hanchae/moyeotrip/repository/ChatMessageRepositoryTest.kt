package kr.hanchae.moyeotrip.repository

import jakarta.persistence.EntityManager
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChatMessageRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository

    @Nested
    inner class DeleteAllByChatRoomId {
        @Test
        fun `지정한 채팅방의 메시지만 삭제한다`() {
            val host = savedUser()
            val course = savedCourse()
            val room = savedRoom(host, course)
            val otherRoom = savedRoom(host, course, title = "다른 채팅방")
            chatMessageRepository.saveAndFlush(
                ChatMessage(
                    chatRoom = room,
                    sender = host,
                    type = ChatMessageType.USER,
                    content = "삭제 대상",
                ),
            )
            chatMessageRepository.saveAndFlush(
                ChatMessage(
                    chatRoom = otherRoom,
                    sender = host,
                    type = ChatMessageType.USER,
                    content = "보존 대상",
                ),
            )

            assertEquals(1, chatMessageRepository.deleteAllByChatRoomId(room.id))
            entityManager.clear()

            assertEquals(0, chatMessageRepository.countByChatRoomIdAndIdGreaterThan(room.id, 0))
            assertEquals(1, chatMessageRepository.countByChatRoomIdAndIdGreaterThan(otherRoom.id, 0))
        }
    }
}
