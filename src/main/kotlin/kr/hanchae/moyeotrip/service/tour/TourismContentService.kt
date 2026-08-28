package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentDetailResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentImageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentPageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentSummaryResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentTypeResponse
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentImage
import kr.hanchae.moyeotrip.entity.tour.TourismContentImageType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TourismContentImageRepository
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
    private val imageRepository: TourismContentImageRepository,
    private val objectStorageRepository: ObjectStorageRepository,
) {
    @Transactional(readOnly = true)
    fun getContentTypes(): List<TourismContentTypeResponse> =
        contentTypeRepository.findAllByCodeNotOrderByCodeAsc(COURSE_CONTENT_TYPE_ID).map {
            TourismContentTypeResponse(contentTypeId = it.code, contentTypeName = it.name)
        }

    @Transactional(readOnly = true)
    fun getContents(
        contentTypeId: Int?,
        keyword: String?,
        page: Int,
        size: Int,
    ): TourismContentPageResponse {
        if (contentTypeId == COURSE_CONTENT_TYPE_ID) {
            throw BaseException(ErrorCode.TOURISM_COURSE_CONTENT_NOT_LISTED)
        }
        val pageable = PageRequest.of(page.coerceAtLeast(1) - 1, size, Sort.by("title").ascending())
        val keywordPattern =
            keyword
                ?.trim()
                ?.lowercase()
                ?.takeIf(String::isNotEmpty)
                ?.let { "%$it%" }
        val contents = repository.searchListableContents(COURSE_CONTENT_TYPE_ID, contentTypeId, keywordPattern, pageable)
        return TourismContentPageResponse(
            items = contents.content.map { it.toSummaryResponse(objectStorageRepository) },
            page = contents.number + 1,
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
        if (content.telephoneName.isNullOrBlank()) {
            tourApiClient.getCommonDetail(contentId)?.let { detail ->
                content.updateCommonDetail(
                    telephoneName = detail.telname.nullIfBlank() ?: content.telephoneName,
                    homepage = detail.homepage.nullIfBlank() ?: content.homepage,
                    overview = detail.overview.nullIfBlank() ?: content.overview,
                )
            }
        }
        val contentImages = findImages(content, TourismContentImageType.CONTENT)
        val menuImages = if (content.isRestaurant()) findImages(content, TourismContentImageType.MENU) else emptyList()
        return content.toDetailResponse(contentImages, menuImages, objectStorageRepository)
    }

    private fun findImages(
        content: TourismContent,
        type: TourismContentImageType,
    ): List<TourismContentImage> = imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(content.id, type)

    private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)

    private fun TourismContent.isRestaurant(): Boolean = contentType.code == RESTAURANT_CONTENT_TYPE_ID

    companion object {
        private const val COURSE_CONTENT_TYPE_ID = 25
        private const val RESTAURANT_CONTENT_TYPE_ID = 39
    }
}

private fun TourismContent.toSummaryResponse(objectStorageRepository: ObjectStorageRepository) =
    TourismContentSummaryResponse(
        contentId = contentId,
        contentTypeId = contentType.code,
        title = title,
        address1 = address1,
        address2 = address2,
        thumbnail = thumbnail.toDownloadUrl(objectStorageRepository),
        longitude = longitude,
        latitude = latitude,
    )

private fun TourismContent.toDetailResponse(
    contentImages: List<TourismContentImage>,
    menuImages: List<TourismContentImage>,
    objectStorageRepository: ObjectStorageRepository,
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
    overview = overview,
    thumbnail = thumbnail.toDownloadUrl(objectStorageRepository),
    longitude = longitude,
    latitude = latitude,
    contentImages = contentImages.map { it.toResponse(contentId, objectStorageRepository) },
    menuImages = menuImages.map { it.toResponse(contentId, objectStorageRepository) },
)

private fun TourismContentImage.toResponse(
    contentId: Long,
    objectStorageRepository: ObjectStorageRepository,
) = TourismContentImageResponse(
    contentId = contentId,
    imageName = imageName,
    originalImageUrl = originalImageUrl.toDownloadUrl(objectStorageRepository),
)

private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)

private fun String?.toDownloadUrl(objectStorageRepository: ObjectStorageRepository): String? =
    this?.let {
        if (it.startsWith(ObjectStorageRepository.TOURISM_IMAGE_PATH)) {
            objectStorageRepository.getDownloadUrl(it)
        } else {
            it
        }
    }
