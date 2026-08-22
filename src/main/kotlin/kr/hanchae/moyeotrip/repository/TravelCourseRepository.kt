package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCourseRepository :
    JpaRepository<TravelCourse, Long>,
    TravelCourseCustomRepository {
    fun findByIdAndType(
        id: Long,
        type: TravelCourseType,
    ): TravelCourse?

    fun findAllByTypeOrderByCreatedDateTimeDesc(type: TravelCourseType): List<TravelCourse>

    fun existsByTypeAndTitle(
        type: TravelCourseType,
        title: String,
    ): Boolean
}

interface TravelCourseCustomRepository {
    fun findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(
        type: TravelCourseType,
        tagId: Long,
    ): List<TravelCourse>

    fun findPopularPublicCourses(pageable: Pageable): List<TravelCourse>
}

class TravelCourseCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : TravelCourseCustomRepository {
    override fun findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(
        type: TravelCourseType,
        tagId: Long,
    ): List<TravelCourse> =
        kotlinJdslJpqlExecutor
            .findAll {
                val course = entity(TravelCourse::class)
                val tag = entity(TravelCourseTag::class)

                selectDistinct(course)
                    .from(
                        course,
                        innerJoin(course.path(TravelCourse::courseTags)).`as`(tag),
                    ).whereAnd(
                        course.path(TravelCourse::type).eq(type),
                        tag.path(TravelCourseTag::id).eq(tagId),
                    ).orderBy(course.path(TravelCourse::createdDateTime).desc())
            }.filterNotNull()

    override fun findPopularPublicCourses(pageable: Pageable): List<TravelCourse> =
        kotlinJdslJpqlExecutor
            .findAll(pageable) {
                val course = entity(TravelCourse::class)
                val room = entity(ChatRoom::class)

                select(course)
                    .from(
                        course,
                        leftJoin(room).on(room.path(ChatRoom::course).eq(course)),
                    ).where(course.path(TravelCourse::type).eq(TravelCourseType.PUBLIC))
                    .groupBy(course)
                    .orderBy(
                        count(room).desc(),
                        course.path(TravelCourse::id).desc(),
                    )
            }.filterNotNull()
}
