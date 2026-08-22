package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class TravelCourseRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var travelCourseTagRepository: TravelCourseTagRepository

    @Nested
    inner class TagQueries {
        @Test
        fun `유형과 태그에 일치하는 코스를 생성일 내림차순으로 조회한다`() {
            val host = savedUser()
            val tag = travelCourseTagRepository.saveAndFlush(TravelCourseTag(name = "JDSL 태그 ${System.nanoTime()}"))
            val taggedCourse = TravelCourse(type = TravelCourseType.CUSTOM, owner = host, title = "태그 코스")
            taggedCourse.addTags(listOf(tag))
            travelCourseRepository.saveAndFlush(taggedCourse)
            val publishedCourse = TravelCourse(type = TravelCourseType.CUSTOM, owner = host, title = "공개 태그 코스")
            publishedCourse.addTags(listOf(tag))
            publishedCourse.publish()
            travelCourseRepository.saveAndFlush(publishedCourse)

            assertEquals(
                listOf(taggedCourse.id),
                travelCourseRepository
                    .findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(TravelCourseType.CUSTOM, tag.id)
                    .map { it.id },
            )
        }
    }

    @Nested
    inner class PopularCourseQueries {
        @Test
        fun `채팅방 수가 많은 공개 코스부터 조회한다`() {
            val host = savedUser()
            val popularCourse = savedCourse(title = "인기 코스")
            val ordinaryCourse = savedCourse(title = "일반 코스")
            val customCourse =
                travelCourseRepository.saveAndFlush(
                    TravelCourse(
                        type = TravelCourseType.CUSTOM,
                        owner = host,
                        title = "커스텀 인기 코스",
                    ),
                )
            savedRoom(host, popularCourse, title = "인기 방 1")
            savedRoom(host, popularCourse, title = "인기 방 2")
            savedRoom(host, ordinaryCourse, title = "일반 방")
            savedRoom(host, customCourse, title = "커스텀 방 1")
            savedRoom(host, customCourse, title = "커스텀 방 2")
            savedRoom(host, customCourse, title = "커스텀 방 3")

            assertEquals(
                popularCourse.id,
                travelCourseRepository.findPopularPublicCourses(PageRequest.of(0, 10)).first().id,
            )
        }
    }
}
