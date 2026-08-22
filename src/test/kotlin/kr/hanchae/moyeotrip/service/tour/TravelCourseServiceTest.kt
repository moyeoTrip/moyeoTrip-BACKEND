package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.CoursePublicationStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.Optional

class TravelCourseServiceTest {
    private val tagRepository = mock(TravelCourseTagRepository::class.java)
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val courseRepository = mock(TravelCourseRepository::class.java)
    private val service = TravelCourseService(tagRepository, roomRepository, courseRepository)

    @Test
    fun `완료한 커스텀 코스를 이름과 소개를 수정해 익명으로 공개한다`() {
        val course = customCourse()
        `when`(courseRepository.findById(course.id)).thenReturn(Optional.of(course))
        `when`(
            roomRepository.existsCompletedHostRoom(course.owner!!.id, course.id, ChatRoomStatus.CONFIRMED, LocalDate.now()),
        ).thenReturn(true)

        val response =
            service.publishCourse(
                hostId = course.owner!!.id,
                courseId = course.id,
                request = PublishTravelCourseRequest("새 코스 이름", "천천히 걷는 코스", false),
            )

        assertEquals(TravelCourseType.PUBLIC, course.type)
        assertEquals(CoursePublicationStatus.PUBLISHED, response.publicationStatus)
        assertEquals("새 코스 이름", course.title)
        assertEquals("천천히 걷는 코스", course.description)
        assertFalse(course.showCreatorNickname)
    }

    @Test
    fun `호스트가 아니면 코스 공개 여부를 결정할 수 없다`() {
        val course = customCourse()
        `when`(courseRepository.findById(course.id)).thenReturn(Optional.of(course))

        val exception =
            assertThrows(BaseException::class.java) {
                service.publishCourse(999L, course.id, PublishTravelCourseRequest("코스", "소개", true))
            }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    private fun customCourse(): TravelCourse {
        val host = User(id = 1L, userRole = UserRole.ROLE_USER)
        return TravelCourse(id = 2L, type = TravelCourseType.CUSTOM, owner = host, title = "기존 코스")
    }
}
