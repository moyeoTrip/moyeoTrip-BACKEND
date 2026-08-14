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
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import kr.hanchae.moyeotrip.entity.BaseModifiableEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(name = "travel_courses")
class TravelCourse(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    val type: TravelCourseType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", updatable = false)
    val owner: User? = null,
    @Column(nullable = false, length = 100)
    val title: String,
) : BaseModifiableEntity() {
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private val coursePlaces: MutableList<TravelCoursePlace> = mutableListOf()

    val places: List<TravelCoursePlace>
        get() = coursePlaces.toList()

    fun addCustomPlace(
        tourismContent: TourismContent,
        sequence: Int,
    ): TravelCoursePlace {
        check(type == TravelCourseType.CUSTOM) { "관리자 코스의 세부 장소는 변경할 수 없습니다." }
        return TravelCoursePlace(course = this, tourismContent = tourismContent, sequence = sequence)
            .also(coursePlaces::add)
    }
}

enum class TravelCourseType {
    CUSTOM,
    MANAGED,
}
