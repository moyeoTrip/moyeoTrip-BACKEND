package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomKickHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomKickHistoryRepository : JpaRepository<ChatRoomKickHistory, Long> {
    fun findAllByKickedUserIdOrderByCreatedDateTimeDescIdDesc(kickedUserId: Long): List<ChatRoomKickHistory>

    // BE-34 · 내보내진 사람의 재신청을 막는다. 화면이 약속하는 규칙이라 서버가 지켜야 한다.
    fun existsByChatRoomIdAndKickedUserId(
        chatRoomId: Long,
        kickedUserId: Long,
    ): Boolean

    fun findByIdAndKickedUserId(
        id: Long,
        kickedUserId: Long,
    ): ChatRoomKickHistory?
}
