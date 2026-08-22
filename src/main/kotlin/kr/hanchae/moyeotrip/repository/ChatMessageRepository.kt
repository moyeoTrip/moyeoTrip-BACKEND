package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
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

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ChatMessage message WHERE message.chatRoom.id = :roomId")
    fun deleteAllByChatRoomId(
        @Param("roomId") roomId: Long,
    ): Int
}
