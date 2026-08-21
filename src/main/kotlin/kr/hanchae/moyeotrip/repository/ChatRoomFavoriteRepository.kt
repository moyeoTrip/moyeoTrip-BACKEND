package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomFavorite
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomFavoriteRepository : JpaRepository<ChatRoomFavorite, Long> {
    fun existsByUserIdAndChatRoomId(
        userId: Long,
        chatRoomId: Long,
    ): Boolean

    fun findByUserIdAndChatRoomId(
        userId: Long,
        chatRoomId: Long,
    ): ChatRoomFavorite?
}
