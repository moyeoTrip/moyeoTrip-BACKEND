package kr.hanchae.moyeotrip.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import java.time.LocalDate

@Entity
@Table(
    name = "user_trip_histories",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_trip_history_room", columnNames = ["user_id", "original_room_id"])],
)
class UserTripHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @Column(name = "original_room_id", nullable = false, updatable = false)
    val originalRoomId: Long,
    @Column(name = "travel_course_id", nullable = false, updatable = false)
    val travelCourseId: Long,
    @Column(name = "room_title", nullable = false, length = 100, updatable = false)
    val roomTitle: String,
    @Column(name = "room_description", length = 500, updatable = false)
    val roomDescription: String? = null,
    @Column(name = "trip_start_date", nullable = false, updatable = false)
    val tripStartDate: LocalDate,
    @Column(name = "trip_end_date", nullable = false, updatable = false)
    val tripEndDate: LocalDate,
    @Column(name = "host", nullable = false, columnDefinition = "NUMBER(1)", updatable = false)
    val host: Boolean,
) : BaseTimeEntity()
