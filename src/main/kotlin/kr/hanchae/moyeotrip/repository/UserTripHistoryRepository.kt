package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.UserTripHistory
import org.springframework.data.jpa.repository.JpaRepository

interface UserTripHistoryRepository : JpaRepository<UserTripHistory, Long> {
    fun findAllByUserIdOrderByTripEndDateDescIdDesc(userId: Long): List<UserTripHistory>

    fun findAllByOriginalRoomId(originalRoomId: Long): List<UserTripHistory>

    fun existsByUserIdAndTravelCourseIdAndHostTrue(
        userId: Long,
        travelCourseId: Long,
    ): Boolean
}
