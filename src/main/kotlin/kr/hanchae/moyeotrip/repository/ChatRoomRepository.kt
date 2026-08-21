package kr.hanchae.moyeotrip.repository

import jakarta.persistence.LockModeType
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT room FROM ChatRoom room WHERE room.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): ChatRoom?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT room FROM ChatRoom room WHERE room.status = :status AND room.recruitmentDeadlineDate < :date")
    fun findAllExpiredRecruitingRoomsForUpdate(
        @Param("status") status: ChatRoomStatus,
        @Param("date") date: LocalDate,
    ): List<ChatRoom>

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 해당 락은 트랜잭션 내에서만 유효하며, 다른 트랜잭션이 해당 레코드를 수정하려고 하면 대기하게 됩니다.
    @Query("SELECT room FROM ChatRoom room WHERE room.deletionScheduledDate <= :date")
    fun findAllDeletionDueRoomsForUpdate(
        @Param("date") date: LocalDate,
    ): List<ChatRoom>

    fun findAllByStatusAndRecruitmentDeadlineDateBetween(
        status: ChatRoomStatus,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ChatRoom>

    fun findFirstByCourseIdAndHostIdAndStatusOrderByStartDateAsc(
        courseId: Long,
        hostId: Long,
        status: ChatRoomStatus,
    ): ChatRoom?

    fun countByCourseIdAndStatusNot(
        courseId: Long,
        status: ChatRoomStatus,
    ): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT room FROM ChatRoom room
        WHERE room.status = :status
          AND room.course.type = CUSTOM
          AND ((room.endDate IS NULL AND room.startDate < :date) OR room.endDate < :date)
        """,
    )
    fun findAllCompletedConfirmedRoomsForUpdate(
        @Param("status") status: ChatRoomStatus,
        @Param("date") date: LocalDate,
    ): List<ChatRoom>
}
