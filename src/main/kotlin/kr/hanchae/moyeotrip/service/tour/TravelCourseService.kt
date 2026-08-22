package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.controller.tour.request.PublishTravelCourseRequest
import kr.hanchae.moyeotrip.controller.tour.response.CoursePublicationResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import kr.hanchae.moyeotrip.repository.UserTripHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TravelCourseService(
    private val courseTagRepository: TravelCourseTagRepository,
    private val roomRepository: ChatRoomRepository,
    private val courseRepository: TravelCourseRepository,
    private val userTripHistoryRepository: UserTripHistoryRepository,
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
        if (course.owner?.id != hostId) throw BaseException(ErrorCode.FORBIDDEN)
        if (course.type != TravelCourseType.CUSTOM || !hasCompletedHostedTrip(hostId, courseId)) {
            throw BaseException(ErrorCode.TRAVEL_COURSE_PUBLICATION_NOT_ALLOWED)
        }
        course.publish(
            title = request.title.trim(),
            description = request.description.trim(),
            showCreatorNickname = request.showCreatorNickname,
        )
        return CoursePublicationResponse(course.id, course.publicationStatus)
    }

    private fun hasCompletedHostedTrip(
        hostId: Long,
        courseId: Long,
    ): Boolean =
        userTripHistoryRepository.existsByUserIdAndTravelCourseIdAndHostTrue(hostId, courseId) ||
            roomRepository.existsCompletedHostRoom(hostId, courseId, ChatRoomStatus.CONFIRMED, LocalDate.now())
}
