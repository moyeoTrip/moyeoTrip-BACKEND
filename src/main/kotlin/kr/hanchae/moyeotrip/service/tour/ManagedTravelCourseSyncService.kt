package kr.hanchae.moyeotrip.service.tour

import com.fasterxml.jackson.databind.JsonNode
import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCoursePlace
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TravelCoursePlaceRepository
import kr.hanchae.moyeotrip.repository.TravelCourseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManagedTravelCourseSyncService(
    private val tourApiClient: TourApiClient,
    private val contentRepository: TourismContentRepository,
    private val courseRepository: TravelCourseRepository,
    private val placeRepository: TravelCoursePlaceRepository,
) {
    @Transactional
    fun sync(): Int {
        val courseContents = contentRepository.findAllByContentTypeCode(COURSE_CONTENT_TYPE_ID)
        courseContents.forEach(::syncCourse)
        return courseContents.size
    }

    private fun syncCourse(source: TourismContent) {
        val detailItems = tourApiClient.getAdditionalDetails(source.contentId, COURSE_CONTENT_TYPE_ID)
        val placesByContentId =
            contentRepository
                .findAllByContentIdIn(detailItems.mapNotNull { it.subContentId() })
                .associateBy { it.contentId }
        val course =
            courseRepository.findBySourceContentId(source.id)?.apply { updateManagedTitle(source.title) }
                ?: courseRepository.saveAndFlush(
                    TravelCourse(
                        type = TravelCourseType.MANAGED,
                        title = source.title,
                        sourceContent = source,
                    ),
                )
        placeRepository.deleteAllByCourseId(course.id)
        placeRepository.flush()
        detailItems
            .mapNotNull { item ->
                val content = item.subContentId()?.let(placesByContentId::get) ?: return@mapNotNull null
                val sequence = item.path("subnum").asText().toIntOrNull() ?: return@mapNotNull null
                TravelCoursePlace(
                    course = course,
                    tourismContent = content,
                    sequence = sequence,
                )
            }.sortedBy(TravelCoursePlace::sequence)
            .forEach(placeRepository::save)
    }

    private fun JsonNode.subContentId(): Long? = path("subcontentid").asText().toLongOrNull()

    companion object {
        private const val COURSE_CONTENT_TYPE_ID = 25
    }
}
