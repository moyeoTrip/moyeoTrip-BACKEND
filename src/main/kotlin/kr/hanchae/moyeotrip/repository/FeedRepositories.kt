package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import jakarta.persistence.LockModeType
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedComment
import kr.hanchae.moyeotrip.entity.feed.FeedLike
import kr.hanchae.moyeotrip.entity.feed.FeedReport
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserBlock
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FeedRepository :
    JpaRepository<Feed, Long>,
    FeedCustomRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT feed FROM Feed feed WHERE feed.id = :feedId")
    fun findByIdForUpdate(
        @Param("feedId") feedId: Long,
    ): Feed?

    fun countByAuthorIdAndVisibility(
        authorId: Long,
        visibility: FeedVisibility,
    ): Long

    fun existsByChatRoomIdAndAuthorId(
        chatRoomId: Long,
        authorId: Long,
    ): Boolean
}

interface FeedReportRepository : JpaRepository<FeedReport, Long> {
    fun existsByFeedIdAndReporterId(
        feedId: Long,
        reporterId: Long,
    ): Boolean

    fun countByFeedId(feedId: Long): Long
}

interface FeedCustomRepository {
    fun findRandomDiscoverFeeds(
        userId: Long,
        limit: Int,
    ): List<Feed>

    fun findFriendFeeds(
        userId: Long,
        beforeId: Long,
        publicVisibility: FeedVisibility,
        friendsVisibility: FeedVisibility,
        pageable: Pageable,
    ): List<Feed>
}

class FeedCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : FeedCustomRepository {
    override fun findRandomDiscoverFeeds(
        userId: Long,
        limit: Int,
    ): List<Feed> =
        kotlinJdslJpqlExecutor
            .findAll(limit = limit) {
                val feed = entity(Feed::class)
                val block = entity(UserBlock::class)

                select(feed)
                    .from(feed)
                    .whereAnd(
                        feed.path(Feed::visibility).eq(FeedVisibility.PUBLIC),
                        notExists(
                            select(block.path(UserBlock::id))
                                .from(block)
                                .whereOr(
                                    and(
                                        block.path(UserBlock::blocker).path(User::id).eq(userId),
                                        block.path(UserBlock::blocked).path(User::id).eq(feed.path(Feed::author).path(User::id)),
                                    ),
                                    and(
                                        block.path(UserBlock::blocker).path(User::id).eq(feed.path(Feed::author).path(User::id)),
                                        block.path(UserBlock::blocked).path(User::id).eq(userId),
                                    ),
                                ).asSubquery(),
                        ),
                    ).orderBy(function(Double::class, "DBMS_RANDOM.VALUE").asc())
            }.filterNotNull()

    override fun findFriendFeeds(
        userId: Long,
        beforeId: Long,
        publicVisibility: FeedVisibility,
        friendsVisibility: FeedVisibility,
        pageable: Pageable,
    ): List<Feed> =
        kotlinJdslJpqlExecutor
            .findAll(pageable) {
                val feed = entity(Feed::class)
                val friendship = entity(Friendship::class)
                val block = entity(UserBlock::class)
                val authorId = feed.path(Feed::author).path(User::id)

                select(feed)
                    .from(feed)
                    .whereAnd(
                        feed.path(Feed::id).lt(beforeId),
                        exists(
                            select(friendship.path(Friendship::id))
                                .from(friendship)
                                .whereOr(
                                    and(
                                        friendship.path(Friendship::firstUser).path(User::id).eq(userId),
                                        friendship.path(Friendship::secondUser).path(User::id).eq(authorId),
                                    ),
                                    and(
                                        friendship.path(Friendship::secondUser).path(User::id).eq(userId),
                                        friendship.path(Friendship::firstUser).path(User::id).eq(authorId),
                                    ),
                                ).asSubquery(),
                        ),
                        feed.path(Feed::visibility).`in`(publicVisibility, friendsVisibility),
                        notExists(
                            select(block.path(UserBlock::id))
                                .from(block)
                                .whereOr(
                                    and(
                                        block.path(UserBlock::blocker).path(User::id).eq(userId),
                                        block.path(UserBlock::blocked).path(User::id).eq(authorId),
                                    ),
                                    and(
                                        block.path(UserBlock::blocker).path(User::id).eq(authorId),
                                        block.path(UserBlock::blocked).path(User::id).eq(userId),
                                    ),
                                ).asSubquery(),
                        ),
                    ).orderBy(feed.path(Feed::id).desc())
            }.filterNotNull()
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
