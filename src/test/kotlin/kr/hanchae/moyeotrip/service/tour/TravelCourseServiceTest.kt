package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.CoursePublicationStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseLike
import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseLikeRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.Optional

class TravelCourseServiceTest {
    private val tagRepository = mock(TravelCourseTagRepository::class.java)
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val courseRepository = mock(TravelCourseRepository::class.java)
    private val courseLikeRepository = mock(TravelCourseLikeRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val service = TravelCourseService(tagRepository, roomRepository, courseRepository, courseLikeRepository, userRepository)

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
        assertEquals("코스 만든 사람", course.creatorNickname)
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

    @Test
    fun `여행 코스 태그는 ID 오름차순 저장소 결과를 응답으로 변환한다`() {
        `when`(tagRepository.findAllByOrderByIdAsc())
            .thenReturn(listOf(TravelCourseTag(id = 1L, name = "힐링"), TravelCourseTag(id = 2L, name = "맛집")))

        val response = service.getCourseTags()

        assertEquals(listOf(1L, 2L), response.map { it.tagId })
        assertEquals(listOf("힐링", "맛집"), response.map { it.name })
    }

    @Test
    fun `존재하지 않는 코스는 공개할 수 없다`() {
        `when`(courseRepository.findById(404L)).thenReturn(Optional.empty())

        val exception =
            assertThrows(BaseException::class.java) {
                service.publishCourse(1L, 404L, PublishTravelCourseRequest("코스", "소개", true))
            }

        assertEquals(ErrorCode.TRAVEL_COURSE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `등록 코스는 다시 공개 처리할 수 없다`() {
        val course =
            TravelCourse(
                id = 2L,
                type = TravelCourseType.PUBLIC,
                owner = User(id = 1L, userRole = UserRole.ROLE_USER),
                title = "등록 코스",
            )
        `when`(courseRepository.findById(2L)).thenReturn(Optional.of(course))

        val exception =
            assertThrows(BaseException::class.java) {
                service.publishCourse(1L, 2L, PublishTravelCourseRequest("코스", "소개", true))
            }

        assertEquals(ErrorCode.TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `완료한 호스트 여행이 없는 커스텀 코스는 공개할 수 없다`() {
        val course = customCourse()
        `when`(courseRepository.findById(course.id)).thenReturn(Optional.of(course))
        `when`(
            roomRepository.existsCompletedHostRoom(course.owner!!.id, course.id, ChatRoomStatus.CONFIRMED, LocalDate.now()),
        ).thenReturn(false)

        val exception =
            assertThrows(BaseException::class.java) {
                service.publishCourse(1L, course.id, PublishTravelCourseRequest("코스", "소개", true))
            }

        assertEquals(ErrorCode.TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED, exception.errorCode)
        assertTrue(course.type == TravelCourseType.CUSTOM)
    }

    @Test
    fun `공개 코스에 좋아요를 추가한다`() {
        val course = TravelCourse(id = 2L, type = TravelCourseType.PUBLIC, title = "공개 코스")
        val user = User(id = 3L, userRole = UserRole.ROLE_USER)
        `when`(courseRepository.findByIdAndType(2L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(courseLikeRepository.countByCourseId(2L)).thenReturn(4L)
        `when`(courseLikeRepository.findByCourseIdAndUserId(2L, 3L)).thenReturn(null)
        `when`(userRepository.findById(3L)).thenReturn(Optional.of(user))

        val response = service.toggleLike(3L, 2L)

        assertTrue(response.liked)
        assertEquals(5L, response.likeCount)
        verify(courseLikeRepository).save(org.mockito.ArgumentMatchers.any(TravelCourseLike::class.java))
    }

    @Test
    fun `공개 코스의 기존 좋아요를 취소한다`() {
        val course = TravelCourse(id = 2L, type = TravelCourseType.PUBLIC, title = "공개 코스")
        val user = User(id = 3L, userRole = UserRole.ROLE_USER)
        val like = TravelCourseLike(id = 9L, course = course, user = user)
        `when`(courseRepository.findByIdAndType(2L, TravelCourseType.PUBLIC)).thenReturn(course)
        `when`(courseLikeRepository.countByCourseId(2L)).thenReturn(1L)
        `when`(courseLikeRepository.findByCourseIdAndUserId(2L, 3L)).thenReturn(like)

        val response = service.toggleLike(3L, 2L)

        assertFalse(response.liked)
        assertEquals(0L, response.likeCount)
        verify(courseLikeRepository).delete(like)
    }

    private fun customCourse(): TravelCourse {
        val host =
            User(
                id = 1L,
                userRole = UserRole.ROLE_USER,
                userInformation =
                    UserInformation(
                        nickname = "코스 만든 사람",
                        nicknameColor = NicknameColor.MINT,
                        gender = Gender.N,
                    ),
            )
        return TravelCourse(id = 2L, type = TravelCourseType.CUSTOM, owner = host, title = "기존 코스")
    }
}
