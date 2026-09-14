package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomJoinApplication
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomJoinApplicationRepository : JpaRepository<ChatRoomJoinApplication, Long> {
    fun existsByChatRoomIdAndUserIdAndStatusIn(
        chatRoomId: Long,
        userId: Long,
        statuses: Collection<JoinApplicationStatus>,
    ): Boolean

    fun findFirstByChatRoomIdAndUserIdAndStatusInOrderByCreatedDateTimeDescIdDesc(
        chatRoomId: Long,
        userId: Long,
        statuses: Collection<JoinApplicationStatus>,
    ): ChatRoomJoinApplication?

    fun findByIdAndChatRoomId(
        id: Long,
        chatRoomId: Long,
    ): ChatRoomJoinApplication?

    fun findAllByUserIdAndStatusInOrderByCreatedDateTimeDesc(
        userId: Long,
        statuses: Collection<JoinApplicationStatus>,
    ): List<ChatRoomJoinApplication>

    fun countByChatRoomIdAndStatus(
        chatRoomId: Long,
        status: JoinApplicationStatus,
    ): Long

    fun findAllByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
        chatRoomId: Long,
        status: JoinApplicationStatus,
    ): List<ChatRoomJoinApplication>

    // BE-17: 호스트의 거절 기록 조회. 거절 시각이 없는 과거 행도 섞이므로 신청 시각 내림차순으로 정렬한다.
    fun findAllByChatRoomIdAndStatusOrderByCreatedDateTimeDescIdDesc(
        chatRoomId: Long,
        status: JoinApplicationStatus,
    ): List<ChatRoomJoinApplication>

    fun findFirstByChatRoomIdAndStatusOrderByCreatedDateTimeAscIdAsc(
        chatRoomId: Long,
        status: JoinApplicationStatus,
    ): ChatRoomJoinApplication?
}
