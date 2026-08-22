package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository :
    JpaRepository<ChatMessage, Long>,
    ChatMessageCustomRepository {
    fun findByIdAndChatRoomId(
        id: Long,
        chatRoomId: Long,
    ): ChatMessage?

    fun findAllByChatRoomIdOrderByIdDesc(
        chatRoomId: Long,
        pageable: Pageable,
    ): List<ChatMessage>

    fun findAllByChatRoomIdAndIdLessThanOrderByIdDesc(
        chatRoomId: Long,
        beforeMessageId: Long,
        pageable: Pageable,
    ): List<ChatMessage>

    fun findFirstByChatRoomIdOrderByIdDesc(chatRoomId: Long): ChatMessage?

    fun countByChatRoomIdAndIdGreaterThan(
        chatRoomId: Long,
        lastReadMessageId: Long,
    ): Long
}

interface ChatMessageCustomRepository {
    fun deleteAllByChatRoomId(roomId: Long): Int
}

class ChatMessageCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : ChatMessageCustomRepository {
    override fun deleteAllByChatRoomId(roomId: Long): Int =
        kotlinJdslJpqlExecutor.delete {
            val message = entity(ChatMessage::class)

            deleteFrom(message)
                .where(message.path(ChatMessage::chatRoom).path(ChatRoom::id).eq(roomId))
        }
}
