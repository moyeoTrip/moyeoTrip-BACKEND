package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.UserFollow
import org.springframework.data.jpa.repository.JpaRepository

interface UserFollowRepository : JpaRepository<UserFollow, Long> {
    fun findByFollowerIdAndFollowingId(
        followerId: Long,
        followingId: Long,
    ): UserFollow?

    fun findAllByFollowingIdOrderByCreatedDateTimeDesc(followingId: Long): List<UserFollow>

    fun findAllByFollowerIdOrderByCreatedDateTimeDesc(followerId: Long): List<UserFollow>

    fun countByFollowingId(followingId: Long): Long

    fun countByFollowerId(followerId: Long): Long
}
