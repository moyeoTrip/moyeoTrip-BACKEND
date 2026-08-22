package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomKickHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomKickHistoryRepository : JpaRepository<ChatRoomKickHistory, Long> {
    fun findAllByKickedUserIdOrderByCreatedDateTimeDescIdDesc(kickedUserId: Long): List<ChatRoomKickHistory>

    fun findByIdAndKickedUserId(
        id: Long,
        kickedUserId: Long,
    ): ChatRoomKickHistory?
}
