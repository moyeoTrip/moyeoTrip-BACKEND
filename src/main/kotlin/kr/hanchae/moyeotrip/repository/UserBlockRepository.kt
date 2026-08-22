package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserBlock
import org.springframework.data.jpa.repository.JpaRepository

interface UserBlockRepository :
    JpaRepository<UserBlock, Long>,
    UserBlockCustomRepository {
    fun findByBlockerIdAndBlockedId(
        blockerId: Long,
        blockedId: Long,
    ): UserBlock?

    fun findAllByBlockerIdOrderByCreatedDateTimeDesc(blockerId: Long): List<UserBlock>
}

interface UserBlockCustomRepository {
    fun findRelatedUserIds(userId: Long): List<Long>

    fun existsBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Boolean
}

class UserBlockCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : UserBlockCustomRepository {
    override fun findRelatedUserIds(userId: Long): List<Long> =
        kotlinJdslJpqlExecutor
            .findAll {
                val block = entity(UserBlock::class)
                val blockerId = block.path(UserBlock::blocker).path(User::id)
                val blockedId = block.path(UserBlock::blocked).path(User::id)

                select(
                    caseWhen(blockerId.eq(userId))
                        .then(blockedId)
                        .`else`(blockerId),
                ).from(block)
                    .whereOr(
                        blockerId.eq(userId),
                        blockedId.eq(userId),
                    )
            }.filterNotNull()

    override fun existsBetween(
        firstUserId: Long,
        secondUserId: Long,
    ): Boolean =
        kotlinJdslJpqlExecutor
            .findAll(limit = 1) {
                val existsRoot = entity(User::class, "existsRoot")
                val block = entity(UserBlock::class, "block")

                select(
                    caseWhen(
                        exists(
                            select(block.path(UserBlock::id))
                                .from(block)
                                .where(
                                    or(
                                        and(
                                            block.path(UserBlock::blocker).path(User::id).eq(firstUserId),
                                            block.path(UserBlock::blocked).path(User::id).eq(secondUserId),
                                        ),
                                        and(
                                            block.path(UserBlock::blocker).path(User::id).eq(secondUserId),
                                            block.path(UserBlock::blocked).path(User::id).eq(firstUserId),
                                        ),
                                    ),
                                ).asSubquery(),
                        ),
                    ).then(true).`else`(false),
                ).from(existsRoot)
            }.firstOrNull() ?: false
}
