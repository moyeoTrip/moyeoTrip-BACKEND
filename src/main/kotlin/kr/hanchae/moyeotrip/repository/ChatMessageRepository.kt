package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

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
}
