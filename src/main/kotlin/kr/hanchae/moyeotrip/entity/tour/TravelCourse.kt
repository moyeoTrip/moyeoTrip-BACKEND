package kr.hanchae.moyeotrip.entity.tour

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
    title: String,
    description: String? = null,
    @Column(name = "duration_minutes")
    val durationMinutes: Long? = null,
    @Column(name = "trip_nights")
    val tripNights: Int? = null,
    @Column(name = "trip_days")
    val tripDays: Int? = null,
    publicationStatus: CoursePublicationStatus =
        if (type == TravelCourseType.PUBLIC) CoursePublicationStatus.PUBLISHED else CoursePublicationStatus.NOT_REQUESTED,
    showCreatorNickname: Boolean = true,
    creatorNickname: String? = null,
) : BaseModifiableEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: TravelCourseType = type
        protected set

    @Column(nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(length = 500)
    var description: String? = description
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    var publicationStatus: CoursePublicationStatus = publicationStatus
        protected set

    @Column(name = "show_creator_nickname", nullable = false, columnDefinition = "NUMBER(1)")
    var showCreatorNickname: Boolean = showCreatorNickname
        protected set

    @Column(name = "creator_nickname", length = 24)
    var creatorNickname: String? = creatorNickname
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
    internal val courseTags: MutableSet<TravelCourseTag> = linkedSetOf()

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

    fun publish(
        title: String = this.title,
        description: String? = this.description,
        showCreatorNickname: Boolean = true,
        creatorNickname: String? = owner?.information?.nickname,
    ) {
        check(type == TravelCourseType.CUSTOM) { "커스텀 코스만 공개할 수 있습니다." }
        this.title = title
        this.description = description
        this.showCreatorNickname = showCreatorNickname
        this.creatorNickname = creatorNickname
        type = TravelCourseType.PUBLIC
        publicationStatus = CoursePublicationStatus.PUBLISHED
    }

    fun addTags(tags: Collection<TravelCourseTag>) {
        check(type == TravelCourseType.CUSTOM) { "공개된 코스의 태그는 변경할 수 없습니다." }
        courseTags += tags
    }
}

@Schema(
    description = "여행 코스 종류. CUSTOM=호스트가 채팅방용으로 직접 구성한 코스, PUBLIC=공개 코스",
    allowableValues = ["CUSTOM", "PUBLIC"],
)
enum class TravelCourseType {
    CUSTOM,
    PUBLIC,
}

@Schema(
    description = "커스텀 코스 공개 상태. NOT_REQUESTED=아직 공개를 결정하지 않음, PENDING=공개 정보 입력 중, DECLINED=공개하지 않음, PUBLISHED=공개 코스로 발행됨",
    allowableValues = ["NOT_REQUESTED", "PENDING", "DECLINED", "PUBLISHED"],
)
enum class CoursePublicationStatus {
    NOT_REQUESTED,
    PENDING,
    DECLINED,
    PUBLISHED,
}
