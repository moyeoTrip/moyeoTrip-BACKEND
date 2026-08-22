package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.TravelCompanion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TravelCompanionRepository : JpaRepository<TravelCompanion, Long> {
    fun existsByOwnerIdAndCompanionIdAndChatRoomId(
        ownerId: Long,
        companionId: Long,
        chatRoomId: Long,
    ): Boolean

    fun findAllByOwnerIdAndChatRoomIdOrderByIdAsc(
        ownerId: Long,
        chatRoomId: Long,
    ): List<TravelCompanion>

    fun findByOwnerIdAndCompanionIdAndChatRoomId(
        ownerId: Long,
        companionId: Long,
        chatRoomId: Long,
    ): TravelCompanion?

    fun findAllByOwnerId(ownerId: Long): List<TravelCompanion>

    @Query(
        """
        SELECT AVG(companion.mannerScore) FROM TravelCompanion companion
        WHERE companion.companion.id = :userId
          AND companion.mannerScore IS NOT NULL
        """,
    )
    fun averageMannerScoreByCompanionId(
        @Param("userId") userId: Long,
    ): Double?
}
