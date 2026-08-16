package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "travel_course_places")
class TravelCoursePlace(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_course_id", nullable = false, updatable = false)
    val course: TravelCourse,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tourism_content_id", nullable = false, updatable = false)
    val tourismContent: TourismContent,
    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,
    @Column(name = "place_sequence", nullable = false)
    val sequence: Int,
    @Column(name = "visit_time")
    val visitTime: LocalTime? = null,
)
