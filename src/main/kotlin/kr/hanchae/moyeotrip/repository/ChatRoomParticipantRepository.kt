package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatRoomParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomParticipantRepository : JpaRepository<ChatRoomParticipant, Long> {
    fun countByChatRoomId(chatRoomId: Long): Long

    fun existsByChatRoomIdAndUserId(
        chatRoomId: Long,
        userId: Long,
    ): Boolean

    fun findByChatRoomIdAndUserId(
        chatRoomId: Long,
        userId: Long,
    ): ChatRoomParticipant?

    fun findAllByChatRoomIdOrderByCreatedDateTimeAsc(chatRoomId: Long): List<ChatRoomParticipant>

    fun findAllByUserId(userId: Long): List<ChatRoomParticipant>

    @Query(
        """
        SELECT COUNT(participant) FROM ChatRoomParticipant participant
        WHERE participant.user.id = :userId
          AND participant.role = kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole.MEMBER
          AND participant.chatRoom.status = kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus.CONFIRMED
          AND participant.chatRoom.startDate < CURRENT_DATE
        """,
    )
    fun countCompletedTrips(
        @Param("userId") userId: Long,
    ): Long
}
