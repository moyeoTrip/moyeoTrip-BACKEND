package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomNotice
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomNoticeRepository : JpaRepository<ChatRoomNotice, Long> {
    fun findFirstByChatRoomIdAndPinnedTrueAndContentIsNotNullOrderByCreatedDateTimeDescIdDesc(chatRoomId: Long): ChatRoomNotice?

    fun findAllByChatRoomIdAndContentIsNotNullOrderByCreatedDateTimeDescIdDesc(chatRoomId: Long): List<ChatRoomNotice>

    fun findByIdAndChatRoomId(
        id: Long,
        chatRoomId: Long,
    ): ChatRoomNotice?
}
