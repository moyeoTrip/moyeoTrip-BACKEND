package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface FriendshipRepository :
    JpaRepository<Friendship, Long>,
    FriendshipCustomRepository

interface FriendRequestRepository :
    JpaRepository<FriendRequest, Long>,
    FriendRequestCustomRepository {
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
}

interface FriendshipCustomRepository {
    fun findBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Friendship?

    fun existsBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Boolean

    fun findAllByUserId(userId: Long): List<Friendship>

    fun deleteBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Int
}

class FriendshipCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : FriendshipCustomRepository {
    override fun findBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Friendship? =
        kotlinJdslJpqlExecutor
            .findAll {
                val friendship = entity(Friendship::class)

                select(friendship)
                    .from(friendship)
                    .where(betweenUsers(friendship, firstUserId, secondUserId))
            }.firstOrNull()

    override fun existsBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Boolean =
        kotlinJdslJpqlExecutor
            .findAll(limit = 1) {
                val existsRoot = entity(User::class, "existsRoot")
                val friendship = entity(Friendship::class, "friendship")

                select(
                    caseWhen(
                        exists(
                            select(friendship.path(Friendship::id))
                                .from(friendship)
                                .where(betweenUsers(friendship, firstUserId, secondUserId))
                                .asSubquery(),
                        ),
                    ).then(true).`else`(false),
                ).from(existsRoot)
            }.firstOrNull() ?: false

    override fun findAllByUserId(userId: Long): List<Friendship> =
        kotlinJdslJpqlExecutor
            .findAll {
                val friendship = entity(Friendship::class)

                select(friendship)
                    .from(friendship)
                    .whereOr(
                        friendship.path(Friendship::firstUser).path(User::id).eq(userId),
                        friendship.path(Friendship::secondUser).path(User::id).eq(userId),
                    ).orderBy(friendship.path(Friendship::createdDateTime).desc())
            }.filterNotNull()

    override fun deleteBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Int =
        kotlinJdslJpqlExecutor.delete {
            val friendship = entity(Friendship::class)

            deleteFrom(friendship)
                .where(betweenUsers(friendship, firstUserId, secondUserId))
        }

    private fun com.linecorp.kotlinjdsl.dsl.jpql.Jpql.betweenUsers(
        friendship: com.linecorp.kotlinjdsl.querymodel.jpql.entity.Entity<Friendship>,
        firstUserId: Long,
        secondUserId: Long,
    ) = or(
        and(
            friendship.path(Friendship::firstUser).path(User::id).eq(firstUserId),
            friendship.path(Friendship::secondUser).path(User::id).eq(secondUserId),
        ),
        and(
            friendship.path(Friendship::firstUser).path(User::id).eq(secondUserId),
            friendship.path(Friendship::secondUser).path(User::id).eq(firstUserId),
        ),
    )
}

interface FriendRequestCustomRepository {
    fun deleteBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Int
}

class FriendRequestCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : FriendRequestCustomRepository {
    override fun deleteBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Int =
        kotlinJdslJpqlExecutor.delete {
            val request = entity(FriendRequest::class)

            deleteFrom(request)
                .where(
                    or(
                        and(
                            request.path(FriendRequest::requester).path(User::id).eq(firstUserId),
                            request.path(FriendRequest::receiver).path(User::id).eq(secondUserId),
                        ),
                        and(
                            request.path(FriendRequest::requester).path(User::id).eq(secondUserId),
                            request.path(FriendRequest::receiver).path(User::id).eq(firstUserId),
                        ),
                    ),
                )
        }
}
