package kr.hanchae.moyeotrip.entity.chat

import io.swagger.v3.oas.annotations.media.Schema
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
import kr.hanchae.moyeotrip.entity.user.AgePolicy
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
    roomTitle: String,
    description: String? = null,
    thumbnail: String? = null,
    maxParticipants: Int,
    minimumParticipants: Int = MINIMUM_PARTICIPANTS,
    startDate: LocalDate,
    endDate: LocalDate? = null,
    recruitmentDeadlineDate: LocalDate,
    dayTripStartTime: LocalTime? = null,
    dayTripEndTime: LocalTime? = null,
    meetingLatitude: Double? = null,
    meetingLongitude: Double? = null,
    meetingDetails: String? = null,
    meetingDateTime: LocalDateTime,
    participationFee: Long? = null,
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
    @Column(name = "room_title", nullable = false, length = 100)
    var roomTitle: String = roomTitle
        protected set

    @Column(length = 500)
    var description: String? = description
        protected set

    @Column(length = 1000)
    var thumbnail: String? = thumbnail
        protected set

    @Column(name = "max_participants", nullable = false)
    var maxParticipants: Int = maxParticipants
        protected set

    @Column(name = "minimum_participants", nullable = false)
    var minimumParticipants: Int = minimumParticipants
        protected set

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = startDate
        protected set

    @Column(name = "end_date")
    var endDate: LocalDate? = endDate
        protected set

    @Column(name = "recruitment_deadline_date", nullable = false)
    var recruitmentDeadlineDate: LocalDate = recruitmentDeadlineDate
        protected set

    @Column(name = "day_trip_start_time")
    var dayTripStartTime: LocalTime? = dayTripStartTime
        protected set

    @Column(name = "day_trip_end_time")
    var dayTripEndTime: LocalTime? = dayTripEndTime
        protected set

    @Column(name = "participation_fee")
    var participationFee: Long? = participationFee
        protected set

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
        require(maxParticipants in MINIMUM_PARTICIPANTS..MAXIMUM_PARTICIPANTS)
        require(minimumParticipants in MINIMUM_PARTICIPANTS..maxParticipants)
        participationFee?.let { require(it >= 0) }
        val minimumAgeValue = minimumAge
        val maximumAgeValue = maximumAge
        minimumAgeValue?.let { require(it in MINIMUM_CONDITION_AGE..MAXIMUM_CONDITION_AGE) }
        maximumAgeValue?.let { require(it in MINIMUM_CONDITION_AGE..MAXIMUM_CONDITION_AGE) }
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

    fun updateRecruitmentPost(
        title: String,
        description: String?,
        minimumParticipants: Int,
        maxParticipants: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        recruitmentDeadlineDate: LocalDate,
        dayTripStartTime: LocalTime?,
        dayTripEndTime: LocalTime?,
        participationFee: Long?,
    ) {
        require(recruitmentDeadlineDate <= startDate)
        endDate?.let { require(it.isAfter(startDate)) }
        val days = endDate?.let { ChronoUnit.DAYS.between(startDate, it).toInt() + 1 } ?: 1
        require(days <= 30)
        require(maxParticipants in MINIMUM_PARTICIPANTS..MAXIMUM_PARTICIPANTS)
        require(minimumParticipants in MINIMUM_PARTICIPANTS..maxParticipants)
        participationFee?.let { require(it >= 0) }
        require(meetingDateTime.toLocalDate() <= startDate)
        if (endDate == null) {
            require(dayTripStartTime != null && dayTripEndTime != null && dayTripStartTime < dayTripEndTime)
        } else {
            require(dayTripStartTime == null && dayTripEndTime == null)
        }
        roomTitle = title
        this.description = description
        this.minimumParticipants = minimumParticipants
        this.maxParticipants = maxParticipants
        this.startDate = startDate
        this.endDate = endDate
        this.recruitmentDeadlineDate = recruitmentDeadlineDate
        this.dayTripStartTime = dayTripStartTime
        this.dayTripEndTime = dayTripEndTime
        this.participationFee = participationFee
    }

    fun updateThumbnail(thumbnail: String) {
        this.thumbnail = thumbnail
    }

    fun canChat(): Boolean = chatClosedDateTime == null && status != ChatRoomStatus.CANCELLED

    fun canAcceptJoinApplication(today: LocalDate = LocalDate.now()): Boolean =
        status != ChatRoomStatus.CANCELLED && today < startDate.minusDays(1)

    fun hasCompletedTrip(today: LocalDate = LocalDate.now()): Boolean =
        status == ChatRoomStatus.CONFIRMED && (endDate ?: startDate).isBefore(today)

    fun scheduleDeletion(deletionDate: LocalDate) {
        require(status == ChatRoomStatus.CONFIRMED)
        deletionScheduledDate = deletionDate
    }

    /**
     * 여행이 끝나고 일정 기간이 지나 **더 쓸 수 없게** 잠근다. 읽기는 그대로 된다.
     *
     * [archiveChat] 과 다르다 — 저쪽은 **보관**이라 삭제 예약을 지우고 메시지도 함께 정리된다.
     * 이쪽은 「대화를 여기서 멈춘다」뿐이라 **삭제 예약을 건드리지 않는다.**
     * 이미 잠긴 방은 그대로 둔다 — 다시 부른다고 잠금 시각이 밀리면 안 된다.
     */
    fun closeChatForWriting(now: LocalDateTime) {
        require(status == ChatRoomStatus.CONFIRMED)
        if (chatClosedDateTime == null) {
            chatClosedDateTime = now
        }
    }

    fun archiveChat(now: LocalDateTime) {
        require(status == ChatRoomStatus.CONFIRMED)
        chatClosedDateTime = now
        deletionScheduledDate = null
    }

    fun isChatArchived(): Boolean = status == ChatRoomStatus.CONFIRMED && chatClosedDateTime != null

    fun recruitmentDDay(today: LocalDate = LocalDate.now()): Long? =
        ChronoUnit.DAYS
            .between(today, recruitmentDeadlineDate)
            .takeIf { it >= 0 }

    companion object {
        const val MINIMUM_PARTICIPANTS = 3
        const val MAXIMUM_PARTICIPANTS = 20

        /** 모집의 연령 조건 하한은 **가입 최소 연령과 같다**. 이유는 [AgePolicy] 참고. */
        const val MINIMUM_CONDITION_AGE = AgePolicy.MINIMUM_SIGNUP_AGE
        const val MAXIMUM_CONDITION_AGE = 100
    }
}

@Schema(
    description = "채팅방 여행 상태. RECRUITING=모집 중, CONFIRMED=여행 확정, CANCELLED=여행 불발·취소",
)
enum class ChatRoomStatus {
    RECRUITING,
    CONFIRMED,
    CANCELLED,
}

@Schema(
    description = "참가 성별 조건. NONE=제한 없음, FEMALE_ONLY=여성만, MALE_ONLY=남성만",
)
enum class GenderRestriction {
    NONE,
    FEMALE_ONLY,
    MALE_ONLY,
}

@Schema(
    description = "참가 승인 방식. AUTO=조건을 충족하면 즉시 참가 또는 대기열 등록, MANUAL=호스트가 신청을 승인해야 함",
    allowableValues = ["AUTO", "MANUAL"],
)
enum class JoinApprovalMode {
    AUTO,
    MANUAL,
}
