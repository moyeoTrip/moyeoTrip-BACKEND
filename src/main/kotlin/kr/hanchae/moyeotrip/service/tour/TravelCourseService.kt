package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.CoursePublicationResponse
import kr.hanchae.moyeotrip.controller.tour.response.LikedTravelCourseResponse
import kr.hanchae.moyeotrip.controller.tour.response.LikedTravelCourseTagResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseFavoriteResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseLike
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseLikeRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TravelCourseService(
    private val courseTagRepository: TravelCourseTagRepository,
    private val roomRepository: ChatRoomRepository,
    private val courseRepository: TravelCourseRepository,
    private val courseLikeRepository: TravelCourseLikeRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getCourseTags(): List<TravelCourseTagResponse> =
        courseTagRepository.findAllByOrderByIdAsc().map { TravelCourseTagResponse(tagId = it.id, name = it.name) }

    @Transactional
    fun publishCourse(
        hostId: Long,
        courseId: Long,
        request: PublishTravelCourseRequest,
    ): CoursePublicationResponse {
        val course = courseRepository.findById(courseId).orElseThrow { BaseException(ErrorCode.TRAVEL_COURSE_NOT_FOUND) }
        if (course.owner?.id != hostId) throw BaseException(ErrorCode.TRAVEL_COURSE_OWNER_REQUIRED)
        if (course.type != TravelCourseType.CUSTOM || !hasCompletedHostedTrip(hostId, courseId)) {
            throw BaseException(ErrorCode.TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED)
        }
        course.publish(
            title = request.title.trim(),
            description = request.description.trim(),
            showCreatorNickname = request.showCreatorNickname,
            creatorNickname = checkNotNull(course.owner?.information).nickname,
        )
        return CoursePublicationResponse(course.id, course.publicationStatus)
    }

    @Transactional
    fun toggleFavorite(
        userId: Long,
        courseId: Long,
    ): TravelCourseFavoriteResponse {
        val course =
            courseRepository.findByIdAndType(courseId, TravelCourseType.PUBLIC)
                ?: throw BaseException(ErrorCode.TRAVEL_COURSE_NOT_FOUND)
        val likeCount = courseLikeRepository.countByCourseId(courseId)
        val existing = courseLikeRepository.findByCourseIdAndUserId(courseId, userId)
        if (existing != null) {
            courseLikeRepository.delete(existing)
            return TravelCourseFavoriteResponse(favorite = false, favoriteCount = (likeCount - 1L).coerceAtLeast(0L))
        }
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        courseLikeRepository.save(TravelCourseLike(course = course, user = user))
        return TravelCourseFavoriteResponse(favorite = true, favoriteCount = likeCount + 1L)
    }

    @Transactional(readOnly = true)
    fun getLikedCourses(userId: Long): List<LikedTravelCourseResponse> =
        courseLikeRepository
            .findCoursesByUserIdOrderByLikedAtDesc(userId)
            .map { course ->
                LikedTravelCourseResponse(
                    courseId = course.id,
                    title = course.title,
                    description = course.description,
                    thumbnail =
                        course.places
                            .firstOrNull()
                            ?.tourismContent
                            ?.thumbnail,
                    tags = course.tags.sortedBy { it.id }.map { LikedTravelCourseTagResponse(it.id, it.name) },
                )
            }

    private fun hasCompletedHostedTrip(
        hostId: Long,
        courseId: Long,
    ): Boolean = roomRepository.existsCompletedHostRoom(hostId, courseId, ChatRoomStatus.CONFIRMED, LocalDate.now())
}
