package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.user.TravelCompanion
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCompanionRepository :
    JpaRepository<TravelCompanion, Long>,
    TravelCompanionCustomRepository {
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
}

interface TravelCompanionCustomRepository {
    fun averageMannerScoreByCompanionId(userId: Long): Double?

    fun findAllReviewedByCompanionId(companionId: Long): List<TravelCompanion>
}

class TravelCompanionCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : TravelCompanionCustomRepository {
    override fun averageMannerScoreByCompanionId(userId: Long): Double? =
        kotlinJdslJpqlExecutor
            .findAll {
                val companion = entity(TravelCompanion::class)

                select(avg(companion.path(TravelCompanion::mannerScore)))
                    .from(companion)
                    .whereAnd(
                        companion.path(TravelCompanion::companion).path(User::id).eq(userId),
                        companion.path(TravelCompanion::mannerScore).isNotNull(),
                    )
            }.singleOrNull()

    override fun findAllReviewedByCompanionId(companionId: Long): List<TravelCompanion> =
        kotlinJdslJpqlExecutor
            .findAll {
                val record = entity(TravelCompanion::class)

                select(record)
                    .from(record)
                    .whereAnd(
                        record.path(TravelCompanion::companion).path(User::id).eq(companionId),
                        record.path(TravelCompanion::oneLineReview).isNotNull(),
                    ).orderBy(
                        record.path(TravelCompanion::createdDateTime).desc(),
                        record.path(TravelCompanion::id).desc(),
                    )
            }.filterNotNull()
}
