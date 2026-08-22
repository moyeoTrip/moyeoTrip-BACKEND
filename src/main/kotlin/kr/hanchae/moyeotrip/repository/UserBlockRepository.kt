package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.UserBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserBlockRepository : JpaRepository<UserBlock, Long> {
    @Query(
        """
        SELECT CASE WHEN block.blocker.id = :userId THEN block.blocked.id ELSE block.blocker.id END
        FROM UserBlock block
        WHERE block.blocker.id = :userId OR block.blocked.id = :userId
        """,
    )
    fun findRelatedUserIds(
        @Param("userId") userId: Long,
    ): List<Long>

    fun findByBlockerIdAndBlockedId(
        blockerId: Long,
        blockedId: Long,
    ): UserBlock?

    fun findAllByBlockerIdOrderByCreatedDateTimeDesc(blockerId: Long): List<UserBlock>

    @Query(
        """
        SELECT COUNT(block) > 0 FROM UserBlock block
        WHERE (block.blocker.id = :firstUserId AND block.blocked.id = :secondUserId)
           OR (block.blocker.id = :secondUserId AND block.blocked.id = :firstUserId)
        """,
    )
    fun existsBetween(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): Boolean
}
