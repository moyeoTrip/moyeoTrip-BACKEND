package kr.hanchae.moyeotrip.entity.tour

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.user.User
import java.time.LocalTime

@Entity
@Table(name = "travel_courses")
class TravelCourse(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    type: TravelCourseType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", updatable = false)
    val owner: User? = null,
    @Column(nullable = false, length = 100)
    val title: String,
    @Column(length = 500)
    val description: String? = null,
    @Column(name = "duration_minutes")
    val durationMinutes: Long? = null,
    @Column(name = "trip_nights")
    val tripNights: Int? = null,
    @Column(name = "trip_days")
    val tripDays: Int? = null,
) : BaseModifiableEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: TravelCourseType = type
        protected set

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @OrderBy("dayNumber ASC, sequence ASC")
    private val coursePlaces: MutableList<TravelCoursePlace> = mutableListOf()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "travel_course_tag_mappings",
        joinColumns = [JoinColumn(name = "travel_course_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    private val courseTags: MutableSet<TravelCourseTag> = linkedSetOf()

    val places: List<TravelCoursePlace>
        get() = coursePlaces.toList()

    val tags: Set<TravelCourseTag>
        get() = courseTags.toSet()

    fun addCustomPlace(
        tourismContent: TourismContent,
        dayNumber: Int,
        sequence: Int,
        visitTime: LocalTime,
    ): TravelCoursePlace {
        check(type == TravelCourseType.CUSTOM) { "공개된 코스의 세부 장소는 변경할 수 없습니다." }
        return TravelCoursePlace(
            course = this,
            tourismContent = tourismContent,
            dayNumber = dayNumber,
            sequence = sequence,
            visitTime = visitTime,
        ).also(coursePlaces::add)
    }

    fun clearCustomPlaces() {
        check(type == TravelCourseType.CUSTOM) { "공개된 코스의 세부 장소는 변경할 수 없습니다." }
        coursePlaces.clear()
    }

    fun publish() {
        check(type == TravelCourseType.CUSTOM) { "커스텀 코스만 공개할 수 있습니다." }
        type = TravelCourseType.PUBLIC
    }

    fun addTags(tags: Collection<TravelCourseTag>) {
        check(type == TravelCourseType.CUSTOM) { "공개된 코스의 태그는 변경할 수 없습니다." }
        courseTags += tags
    }
}

enum class TravelCourseType {
    CUSTOM,
    PUBLIC,
}
