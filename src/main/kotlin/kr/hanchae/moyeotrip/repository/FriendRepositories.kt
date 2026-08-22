package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FriendshipRepository : JpaRepository<Friendship, Long> {
    @Query(
        """
        SELECT friendship FROM Friendship friendship
        WHERE (friendship.firstUser.id = :firstUserId AND friendship.secondUser.id = :secondUserId)
           OR (friendship.firstUser.id = :secondUserId AND friendship.secondUser.id = :firstUserId)
        """,
    )
    fun findBetween(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): Friendship?

    @Query(
        """
        SELECT COUNT(friendship) > 0 FROM Friendship friendship
        WHERE (friendship.firstUser.id = :firstUserId AND friendship.secondUser.id = :secondUserId)
           OR (friendship.firstUser.id = :secondUserId AND friendship.secondUser.id = :firstUserId)
        """,
    )
    fun existsBetween(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): Boolean

    @Query(
        """
        SELECT friendship FROM Friendship friendship
        WHERE friendship.firstUser.id = :userId OR friendship.secondUser.id = :userId
        ORDER BY friendship.createdDateTime DESC
        """,
    )
    fun findAllByUserId(
        @Param("userId") userId: Long,
    ): List<Friendship>

    @Modifying
    @Query(
        """
        DELETE FROM Friendship friendship
        WHERE (friendship.firstUser.id = :firstUserId AND friendship.secondUser.id = :secondUserId)
           OR (friendship.firstUser.id = :secondUserId AND friendship.secondUser.id = :firstUserId)
        """,
    )
    fun deleteBetween(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): Int
}

interface FriendRequestRepository : JpaRepository<FriendRequest, Long> {
    fun findByRequesterIdAndReceiverId(
        requesterId: Long,
        receiverId: Long,
    ): FriendRequest?

    fun findByIdAndReceiverId(
        id: Long,
        receiverId: Long,
    ): FriendRequest?

    fun findByIdAndRequesterId(
        id: Long,
        requesterId: Long,
    ): FriendRequest?

    fun findAllByReceiverIdOrderByCreatedDateTimeDesc(receiverId: Long): List<FriendRequest>

    fun findAllByRequesterIdOrderByCreatedDateTimeDesc(requesterId: Long): List<FriendRequest>

    @Modifying
    @Query(
        """
        DELETE FROM FriendRequest request
        WHERE (request.requester.id = :firstUserId AND request.receiver.id = :secondUserId)
           OR (request.requester.id = :secondUserId AND request.receiver.id = :firstUserId)
        """,
    )
    fun deleteBetween(
        @Param("firstUserId") firstUserId: Long,
        @Param("secondUserId") secondUserId: Long,
    ): Int
}
