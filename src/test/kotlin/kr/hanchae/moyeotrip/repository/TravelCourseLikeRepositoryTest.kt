package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseLike
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TravelCourseLikeRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var courseLikeRepository: TravelCourseLikeRepository

    @Test
    fun `내가 찜한 코스만 찜한 최신순으로 조회한다`() {
        val user = savedUser()
        val anotherUser = savedUser()
        val firstCourse = savedCourse(title = "첫 코스")
        val secondCourse = savedCourse(title = "두 번째 코스")
        val excludedCourse = savedCourse(title = "제외 코스")
        val customCourse =
            travelCourseRepository.saveAndFlush(
                TravelCourse(type = TravelCourseType.CUSTOM, owner = user, title = "비공개 커스텀 코스"),
            )
        courseLikeRepository.saveAndFlush(TravelCourseLike(course = firstCourse, user = user))
        courseLikeRepository.saveAndFlush(TravelCourseLike(course = secondCourse, user = user))
        courseLikeRepository.saveAndFlush(TravelCourseLike(course = excludedCourse, user = anotherUser))
        courseLikeRepository.saveAndFlush(TravelCourseLike(course = customCourse, user = user))

        val result = courseLikeRepository.findCoursesByUserIdOrderByLikedAtDesc(user.id)

        assertEquals(listOf(secondCourse.id, firstCourse.id), result.map { it.id })
    }
}
