package kr.hanchae.moyeotrip.service.tour

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentDetailResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentPageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentSummaryResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentTypeResponse
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentDetail
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.TourismContentDetailRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TourismContentTypeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TourismContentService(
    private val tourApiClient: TourApiClient,
    private val repository: TourismContentRepository,
    private val contentTypeRepository: TourismContentTypeRepository,
    private val detailRepository: TourismContentDetailRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun getContentTypes(): List<TourismContentTypeResponse> =
        contentTypeRepository.findAllByCodeNotOrderByCodeAsc(COURSE_CONTENT_TYPE_ID).map {
            TourismContentTypeResponse(contentTypeId = it.code, contentTypeName = it.name)
        }

    @Transactional(readOnly = true)
    fun getContents(
        contentTypeId: Int?,
        page: Int,
        size: Int,
    ): TourismContentPageResponse {
        if (contentTypeId == COURSE_CONTENT_TYPE_ID) {
            throw BaseException(ErrorCode.TOURISM_COURSE_CONTENT_NOT_LISTED)
        }
        val pageable = PageRequest.of(page, size, Sort.by("title").ascending())
        val contents =
            contentTypeId?.let { repository.findAllByContentTypeCode(it, pageable) }
                ?: repository.findAllByContentTypeCodeNot(COURSE_CONTENT_TYPE_ID, pageable)
        return TourismContentPageResponse(
            items = contents.content.map(TourismContent::toSummaryResponse),
            page = contents.number,
            size = contents.size,
            totalElements = contents.totalElements,
            totalPages = contents.totalPages,
        )
    }

    @Transactional
    fun getContent(contentId: Long): TourismContentDetailResponse {
        val content =
            repository.findByContentId(contentId)
                ?: throw BaseException(ErrorCode.TOURISM_CONTENT_NOT_FOUND)
        if (!content.hasCommonDetail()) {
            tourApiClient.getCommonDetail(contentId)?.let { detail ->
                content.updateCommonDetail(
                    telephoneName = detail.telname.nullIfBlank(),
                    homepage = detail.homepage.nullIfBlank(),
                    bookTour = detail.booktour.nullIfBlank(),
                    overview = detail.overview.nullIfBlank(),
                )
            }
        }
        val detail =
            detailRepository.findByTourismContentId(content.id)
                ?: fetchAndSaveDetails(content)
        return content.toDetailResponse(detail, objectMapper)
    }

    private fun fetchAndSaveDetails(content: TourismContent): TourismContentDetail {
        val contentId = content.contentId
        val contentTypeId = content.contentType.code
        val intro = tourApiClient.getIntroDetail(contentId, contentTypeId)
        val info = tourApiClient.getAdditionalDetails(contentId, contentTypeId)
        val contentImages = tourApiClient.getImages(contentId, IMAGE_YES)
        val menuImages = tourApiClient.getImages(contentId, IMAGE_NO)
        return detailRepository.saveAndFlush(
            TourismContentDetail(
                tourismContent = content,
                introPayload = objectMapper.writeValueAsString(intro),
                infoPayload = objectMapper.writeValueAsString(info),
                contentImagePayload = objectMapper.writeValueAsString(contentImages),
                menuImagePayload = objectMapper.writeValueAsString(menuImages),
            ),
        )
    }

    private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)

    private fun TourismContent.hasCommonDetail(): Boolean =
        telephoneName != null || homepage != null || bookTour != null || overview != null

    companion object {
        private const val COURSE_CONTENT_TYPE_ID = 25
        private const val IMAGE_YES = "Y"
        private const val IMAGE_NO = "N"
    }
}

private fun TourismContent.toSummaryResponse() =
    TourismContentSummaryResponse(
        contentId = contentId,
        contentTypeId = contentType.code,
        title = title,
        address1 = address1,
        address2 = address2,
        firstImageUrl = firstImageUrl,
        firstThumbnailUrl = firstThumbnailUrl,
        longitude = longitude,
        latitude = latitude,
    )

private fun TourismContent.toDetailResponse(
    detail: TourismContentDetail,
    objectMapper: ObjectMapper,
) = TourismContentDetailResponse(
    contentId = contentId,
    contentTypeId = contentType.code,
    title = title,
    address1 = address1,
    address2 = address2,
    zipcode = zipcode,
    telephone = telephone,
    telephoneName = telephoneName,
    homepage = homepage,
    bookTour = bookTour,
    overview = overview,
    firstImageUrl = firstImageUrl,
    firstThumbnailUrl = firstThumbnailUrl,
    longitude = longitude,
    latitude = latitude,
    introDetails = objectMapper.readTree(detail.introPayload),
    additionalDetails = objectMapper.readTree(detail.infoPayload),
    contentImages = objectMapper.readTree(detail.contentImagePayload),
    menuImages = objectMapper.readTree(detail.menuImagePayload),
)
