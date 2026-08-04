package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomNotice
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomNoticeRepository : JpaRepository<ChatRoomNotice, Long> {
    fun findFirstByChatRoomIdOrderByIdDesc(chatRoomId: Long): ChatRoomNotice?

    fun findAllByChatRoomIdOrderByIdDesc(chatRoomId: Long): List<ChatRoomNotice>
}
