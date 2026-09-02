package kr.hanchae.moyeotrip.entity.chat

import kr.hanchae.moyeotrip.entity.tour.CoursePublicationStatus
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TravelCourseTest {
    @Test
    fun `관리자가 구성한 코스에는 호스트가 장소를 추가할 수 없다`() {
        val course = TravelCourse(id = 1L, type = TravelCourseType.PUBLIC, title = "공개 코스")

        assertThrows(IllegalStateException::class.java) {
            course.addCustomPlace(
                TourismContent(contentId = 1L, contentType = TourismContentType(12, "관광지"), title = "임의 장소"),
                1,
                1,
                LocalTime.of(9, 0),
            )
        }
    }

    @Test
    fun `커스텀 코스는 장소를 추가하고 모두 비울 수 있다`() {
        val course = TravelCourse(id = 1L, type = TravelCourseType.CUSTOM, title = "커스텀 코스")
        val content = TourismContent(contentId = 1L, contentType = TourismContentType(12, "관광지"), title = "주산지")

        val place = course.addCustomPlace(content, 2, 3, LocalTime.of(14, 30))

        assertEquals(listOf(place), course.places)
        assertEquals(2, place.dayNumber)
        assertEquals(3, place.sequence)
        course.clearCustomPlaces()
        assertTrue(course.places.isEmpty())
    }

    @Test
    fun `공개 코스는 장소를 비우거나 태그를 바꿀 수 없다`() {
        val course = TravelCourse(id = 1L, type = TravelCourseType.PUBLIC, title = "공개 코스")

        assertThrows(IllegalStateException::class.java) { course.clearCustomPlaces() }
        assertThrows(IllegalStateException::class.java) {
            course.addTags(listOf(TravelCourseTag(id = 1L, name = "힐링")))
        }
    }

    @Test
    fun `커스텀 코스는 태그를 등록하고 공개할 수 있다`() {
        val owner =
            User(
                id = 1L,
                userRole = UserRole.ROLE_USER,
                userInformation = UserInformation("숲속여행자", NicknameColor.GREEN, Gender.N),
            )
        val course = TravelCourse(id = 1L, type = TravelCourseType.CUSTOM, owner = owner, title = "임시 제목")
        val tags = listOf(TravelCourseTag(id = 1L, name = "힐링"), TravelCourseTag(id = 2L, name = "자연"))

        course.addTags(tags)
        course.publish(title = "공개 제목", description = "공개 설명")

        assertEquals(tags.toSet(), course.tags)
        assertEquals(TravelCourseType.PUBLIC, course.type)
        assertEquals(CoursePublicationStatus.PUBLISHED, course.publicationStatus)
        assertEquals("공개 제목", course.title)
        assertEquals("공개 설명", course.description)
        assertTrue(course.showCreatorNickname)
        assertEquals("숲속여행자", course.creatorNickname)
        assertThrows(IllegalStateException::class.java) { course.publish() }
    }

    @Test
    fun `소유자 정보가 없어도 닉네임을 숨겨 공개할 수 있다`() {
        val course = TravelCourse(id = 1L, type = TravelCourseType.CUSTOM, title = "커스텀 코스")

        course.publish(showCreatorNickname = false)

        assertEquals(TravelCourseType.PUBLIC, course.type)
        assertTrue(course.creatorNickname == null)
        assertTrue(!course.showCreatorNickname)
    }
}
