package kr.hanchae.moyeotrip.entity.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.user.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "chat_rooms")
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false, updatable = false)
    val host: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_course_id", nullable = false, updatable = false)
    val course: TravelCourse,
    @Column(name = "room_title", nullable = false, length = 100)
    val roomTitle: String,
    @Column(length = 500)
    val description: String? = null,
    @Column(name = "max_participants", nullable = false)
    val maxParticipants: Int,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "recruitment_deadline_date", nullable = false)
    val recruitmentDeadlineDate: LocalDate,
    @Column(name = "trip_days", nullable = false)
    val tripDays: Int,
    @Column(name = "day_trip_start_time")
    val dayTripStartTime: LocalTime? = null,
    @Column(name = "day_trip_end_time")
    val dayTripEndTime: LocalTime? = null,
    @Column(name = "meeting_latitude", nullable = false)
    val meetingLatitude: Double,
    @Column(name = "meeting_longitude", nullable = false)
    val meetingLongitude: Double,
    @Column(name = "meeting_datetime", nullable = false)
    val meetingDateTime: LocalDateTime,
    @Column(name = "participation_fee", nullable = false, precision = 12, scale = 0)
    val participationFee: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ChatRoomStatus = ChatRoomStatus.RECRUITING,
) : BaseModifiableEntity() {
    @Column(name = "chat_closed_datetime")
    var chatClosedDateTime: LocalDateTime? = null
        protected set

    @Column(name = "deletion_scheduled_datetime")
    var deletionScheduledDateTime: LocalDateTime? = null
        protected set

    init {
        require(recruitmentDeadlineDate <= startDate)
        require(participationFee.signum() >= 0)
        require(meetingLatitude in -90.0..90.0)
        require(meetingLongitude in -180.0..180.0)
        require(meetingDateTime.toLocalDate() <= startDate)
        if (tripDays == 1) {
            val startTime = dayTripStartTime
            val endTime = dayTripEndTime
            require(startTime != null && endTime != null && startTime < endTime)
        } else {
            require(dayTripStartTime == null && dayTripEndTime == null)
        }
    }

    fun cancel(now: LocalDateTime) {
        status = ChatRoomStatus.CANCELLED
        chatClosedDateTime = now
        deletionScheduledDateTime = now.plusDays(14)
    }

    fun confirm() {
        status = ChatRoomStatus.CONFIRMED
        chatClosedDateTime = null
        deletionScheduledDateTime = null
    }

    fun canChat(): Boolean = chatClosedDateTime == null && status != ChatRoomStatus.CANCELLED

    fun dDay(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, startDate)
}

enum class ChatRoomStatus {
    RECRUITING,
    CONFIRMED,
    CANCELLED,
}
