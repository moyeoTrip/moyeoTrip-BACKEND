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
import jakarta.persistence.Transient
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.user.User
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
    @Column(length = 1000)
    val thumbnail: String? = null,
    @Column(name = "max_participants", nullable = false)
    val maxParticipants: Int,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "end_date")
    val endDate: LocalDate? = null,
    @Column(name = "recruitment_deadline_date", nullable = false)
    val recruitmentDeadlineDate: LocalDate,
    @Column(name = "day_trip_start_time")
    val dayTripStartTime: LocalTime? = null,
    @Column(name = "day_trip_end_time")
    val dayTripEndTime: LocalTime? = null,
    meetingLatitude: Double? = null,
    meetingLongitude: Double? = null,
    meetingDetails: String? = null,
    meetingDateTime: LocalDateTime,
    @Column(name = "participation_fee")
    val participationFee: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_restriction", nullable = false, length = 20)
    val genderRestriction: GenderRestriction = GenderRestriction.NONE,
    @Column(name = "minimum_age")
    val minimumAge: Int? = null,
    @Column(name = "maximum_age")
    val maximumAge: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "join_approval_mode", nullable = false, length = 20)
    val joinApprovalMode: JoinApprovalMode = JoinApprovalMode.MANUAL,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ChatRoomStatus = ChatRoomStatus.RECRUITING,
) : BaseModifiableEntity() {
    @get:Transient
    val tripDays: Int
        get() = endDate?.let { ChronoUnit.DAYS.between(startDate, it).toInt() + 1 } ?: 1

    @get:Transient
    val tripNights: Int
        get() = tripDays - 1

    @get:Transient
    val tripType: TripType
        get() = if (endDate == null) TripType.DAY_TRIP else TripType.OVERNIGHT

    @Column(name = "meeting_latitude")
    var meetingLatitude: Double? = meetingLatitude
        protected set

    @Column(name = "meeting_longitude")
    var meetingLongitude: Double? = meetingLongitude
        protected set

    @Column(name = "meeting_details", length = 500)
    var meetingDetails: String? = meetingDetails
        protected set

    @Column(name = "meeting_datetime", nullable = false)
    var meetingDateTime: LocalDateTime = meetingDateTime
        protected set

    @Column(name = "chat_closed_datetime")
    var chatClosedDateTime: LocalDateTime? = null
        protected set

    @Column(name = "deletion_scheduled_date")
    var deletionScheduledDate: LocalDate? = null
        protected set

    init {
        require(recruitmentDeadlineDate <= startDate)
        endDate?.let { require(it.isAfter(startDate)) }
        require(tripDays <= 30)
        require(maxParticipants in 3..12)
        participationFee?.let { require(it >= 0) }
        val minimumAgeValue = minimumAge
        val maximumAgeValue = maximumAge
        minimumAgeValue?.let { require(it in 20..100) }
        maximumAgeValue?.let { require(it in 20..100) }
        if (minimumAgeValue != null && maximumAgeValue != null) require(minimumAgeValue <= maximumAgeValue)
        meetingLatitude?.let { require(it in -90.0..90.0) }
        meetingLongitude?.let { require(it in -180.0..180.0) }
        require((meetingLatitude == null) == (meetingLongitude == null))
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
        deletionScheduledDate = now.toLocalDate().plusDays(14)
    }

    fun confirm() {
        status = ChatRoomStatus.CONFIRMED
        chatClosedDateTime = null
        deletionScheduledDate = null
    }

    fun updateMeetingInfo(
        latitude: Double?,
        longitude: Double?,
        details: String?,
        dateTime: LocalDateTime,
    ) {
        latitude?.let { require(it in -90.0..90.0) }
        longitude?.let { require(it in -180.0..180.0) }
        require((latitude == null) == (longitude == null))
        require(dateTime.toLocalDate() <= startDate)
        meetingLatitude = latitude
        meetingLongitude = longitude
        meetingDetails = details
        meetingDateTime = dateTime
    }

    fun canChat(): Boolean = chatClosedDateTime == null && status != ChatRoomStatus.CANCELLED

    fun canAcceptJoinApplication(today: LocalDate = LocalDate.now()): Boolean =
        status != ChatRoomStatus.CANCELLED && today < startDate.minusDays(1)

    fun hasCompletedTrip(today: LocalDate = LocalDate.now()): Boolean =
        status == ChatRoomStatus.CONFIRMED && (endDate ?: startDate).isBefore(today)

    fun recruitmentDDay(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, recruitmentDeadlineDate)
}

enum class ChatRoomStatus {
    RECRUITING,
    CONFIRMED,
    CANCELLED,
}

enum class GenderRestriction {
    NONE,
    FEMALE_ONLY,
    MALE_ONLY,
}

enum class JoinApprovalMode {
    AUTO,
    MANUAL,
}
