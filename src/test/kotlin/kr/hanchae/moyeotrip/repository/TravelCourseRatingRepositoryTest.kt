package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TravelCourseRatingRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var ratingRepository: TravelCourseRatingRepository

    @Nested
    inner class FindAverageByCourseId {
        @Test
        fun `코스별 평점 평균을 반환한다`() {
            val host = savedUser()
            val reviewer = savedUser()
            val course = savedCourse()
            val room = savedRoom(host, course)
            val otherCourse = savedCourse()
            val otherRoom = savedRoom(host, otherCourse)
            ratingRepository.saveAndFlush(TravelCourseRating(course = course, chatRoom = room, user = host, score = 5))
            ratingRepository.saveAndFlush(TravelCourseRating(course = course, chatRoom = room, user = reviewer, score = 3))
            ratingRepository.saveAndFlush(TravelCourseRating(course = otherCourse, chatRoom = otherRoom, user = host, score = 1))

            assertEquals(4.0, ratingRepository.findAverageByCourseId(course.id))
            assertNull(ratingRepository.findAverageByCourseId(Long.MAX_VALUE))
        }
    }
}
