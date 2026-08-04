package kr.hanchae.moyeotrip.repository

import jakarta.persistence.LockModeType
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT room FROM ChatRoom room WHERE room.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): ChatRoom?

    @Query(
        """
        SELECT DISTINCT room FROM ChatRoom room JOIN room.course.coursePlaces place
        WHERE (:keyword IS NULL OR LOWER(room.roomTitle) LIKE LOWER(CONCAT(CONCAT('%', :keyword), '%'))
               OR LOWER(room.course.title) LIKE LOWER(CONCAT(CONCAT('%', :keyword), '%'))
               OR LOWER(place.placeName) LIKE LOWER(CONCAT(CONCAT('%', :keyword), '%')))
        ORDER BY room.createdDateTime DESC
        """,
    )

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT room FROM ChatRoom room WHERE room.status = :status AND room.recruitmentDeadlineDate < :date")
    fun findAllExpiredRecruitingRoomsForUpdate(
        @Param("status") status: ChatRoomStatus,
        @Param("date") date: LocalDate,
    ): List<ChatRoom>

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 해당 락은 트랜잭션 내에서만 유효하며, 다른 트랜잭션이 해당 레코드를 수정하려고 하면 대기하게 됩니다.
    @Query("SELECT room FROM ChatRoom room WHERE room.deletionScheduledDateTime <= :dateTime")
    fun findAllDeletionDueRoomsForUpdate(
        @Param("dateTime") dateTime: LocalDateTime,
    ): List<ChatRoom>
}
