package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedComment
import kr.hanchae.moyeotrip.entity.feed.FeedLike
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FeedRepository : JpaRepository<Feed, Long> {
    fun existsByChatRoomIdAndAuthorId(
        chatRoomId: Long,
        authorId: Long,
    ): Boolean

    @Query(
        value =
            """
        SELECT random_feeds.*
        FROM (
            SELECT feeds.*
            FROM feeds
            WHERE feeds.visibility = 'PUBLIC'
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_blocks blocks
                  WHERE (blocks.blocker_id = :userId AND blocks.blocked_id = feeds.author_id)
                     OR (blocks.blocker_id = feeds.author_id AND blocks.blocked_id = :userId)
              )
            ORDER BY DBMS_RANDOM.VALUE
        ) random_feeds
        WHERE ROWNUM <= :limit
        """,
        nativeQuery = true,
    )
    fun findRandomDiscoverFeeds(
        @Param("userId") userId: Long,
        @Param("limit") limit: Int,
    ): List<Feed>

    @Query(
        """
        SELECT feed FROM Feed feed
        WHERE feed.id < :beforeId
          AND EXISTS (
              SELECT friendship.id FROM Friendship friendship
              WHERE (friendship.firstUser.id = :userId AND friendship.secondUser.id = feed.author.id)
                 OR (friendship.secondUser.id = :userId AND friendship.firstUser.id = feed.author.id)
          )
          AND feed.visibility IN (:publicVisibility, :friendsVisibility)
          AND NOT EXISTS (
              SELECT block.id FROM UserBlock block
              WHERE (block.blocker.id = :userId AND block.blocked.id = feed.author.id)
                 OR (block.blocker.id = feed.author.id AND block.blocked.id = :userId)
          )
        ORDER BY feed.id DESC
        """,
    )
    fun findFriendFeeds(
        @Param("userId") userId: Long,
        @Param("beforeId") beforeId: Long,
        @Param("publicVisibility") publicVisibility: FeedVisibility,
        @Param("friendsVisibility") friendsVisibility: FeedVisibility,
        pageable: Pageable,
    ): List<Feed>
}

interface FeedLikeRepository : JpaRepository<FeedLike, Long> {
    fun findByFeedIdAndUserId(
        feedId: Long,
        userId: Long,
    ): FeedLike?

    fun existsByFeedIdAndUserId(
        feedId: Long,
        userId: Long,
    ): Boolean

    fun countByFeedId(feedId: Long): Long
}

interface FeedCommentRepository : JpaRepository<FeedComment, Long> {
    fun countByFeedId(feedId: Long): Long

    fun findAllByFeedIdAndParentIsNullOrderByCreatedDateTimeAsc(feedId: Long): List<FeedComment>

    fun findAllByParentIdOrderByCreatedDateTimeAsc(parentId: Long): List<FeedComment>

    fun findByIdAndFeedId(
        id: Long,
        feedId: Long,
    ): FeedComment?
}
