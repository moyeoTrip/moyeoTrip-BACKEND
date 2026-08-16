package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.repository.TravelCourseTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TravelCourseService(
    private val courseTagRepository: TravelCourseTagRepository,
) {
    @Transactional(readOnly = true)
    fun getCourseTags(): List<TravelCourseTagResponse> =
        courseTagRepository.findAllByOrderByIdAsc().map { TravelCourseTagResponse(tagId = it.id, name = it.name) }
}
