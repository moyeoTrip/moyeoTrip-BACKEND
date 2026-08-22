package kr.hanchae.moyeotrip.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCourseRatingRepository :
    JpaRepository<TravelCourseRating, Long>,
    TravelCourseRatingCustomRepository {
    fun findByChatRoomIdAndUserId(
        chatRoomId: Long,
        userId: Long,
    ): TravelCourseRating?

    fun countByCourseId(courseId: Long): Long
}

interface TravelCourseRatingCustomRepository {
    fun findAverageByCourseId(courseId: Long): Double?
}

class TravelCourseRatingCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : TravelCourseRatingCustomRepository {
    override fun findAverageByCourseId(courseId: Long): Double? =
        kotlinJdslJpqlExecutor
            .findAll {
                val rating = entity(TravelCourseRating::class)

                select(avg(rating.path(TravelCourseRating::score)))
                    .from(rating)
                    .where(rating.path(TravelCourseRating::course).path(TravelCourse::id).eq(courseId))
            }.singleOrNull()
}
