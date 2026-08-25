package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseLike
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCourseLikeRepository :
    JpaRepository<TravelCourseLike, Long>,
    TravelCourseLikeCustomRepository {
    fun findByCourseIdAndUserId(
        courseId: Long,
        userId: Long,
    ): TravelCourseLike?

    fun countByCourseId(courseId: Long): Long
}

interface TravelCourseLikeCustomRepository {
    fun findCoursesByUserIdOrderByLikedAtDesc(userId: Long): List<TravelCourse>
}

class TravelCourseLikeCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : TravelCourseLikeCustomRepository {
    override fun findCoursesByUserIdOrderByLikedAtDesc(userId: Long): List<TravelCourse> =
        kotlinJdslJpqlExecutor
            .findAll {
                val like = entity(TravelCourseLike::class)
                val course = entity(TravelCourse::class)

                select(course)
                    .from(
                        like,
                        innerJoin(like.path(TravelCourseLike::course)).`as`(course),
                    ).whereAnd(
                        like.path(TravelCourseLike::user).path(User::id).eq(userId),
                        course.path(TravelCourse::type).eq(TravelCourseType.PUBLIC),
                    ).orderBy(
                        like.path(TravelCourseLike::createdDateTime).desc(),
                        like.path(TravelCourseLike::id).desc(),
                    )
            }.filterNotNull()
}
