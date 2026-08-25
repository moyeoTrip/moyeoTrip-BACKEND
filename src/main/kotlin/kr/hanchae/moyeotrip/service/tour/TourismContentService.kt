package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourImageItem
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
        val pageable = PageRequest.of(page, size, Sort.by("title").ascending())
        val keywordPattern =
            keyword
                ?.trim()
                ?.lowercase()
                ?.takeIf(String::isNotEmpty)
                ?.let { "%$it%" }
        val contents = repository.searchListableContents(COURSE_CONTENT_TYPE_ID, contentTypeId, keywordPattern, pageable)
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
        if (content.telephoneName.isNullOrBlank()) {
            tourApiClient.getCommonDetail(contentId)?.let { detail ->
                content.updateCommonDetail(
                    telephoneName = detail.telname.nullIfBlank() ?: content.telephoneName,
                    homepage = detail.homepage.nullIfBlank() ?: content.homepage,
                    overview = detail.overview.nullIfBlank() ?: content.overview,
                )
            }
        }
        var contentImages = findImages(content, TourismContentImageType.CONTENT)
        var menuImages = if (content.isRestaurant()) findImages(content, TourismContentImageType.MENU) else emptyList()
        if (contentImages.isEmpty() && menuImages.isEmpty()) {
            fetchAndSaveImages(content)
            contentImages = findImages(content, TourismContentImageType.CONTENT)
            menuImages = if (content.isRestaurant()) findImages(content, TourismContentImageType.MENU) else emptyList()
        }
        return content.toDetailResponse(contentImages, menuImages)
    }

    private fun findImages(
        content: TourismContent,
        type: TourismContentImageType,
    ): List<TourismContentImage> = imageRepository.findAllByTourismContentIdAndTypeOrderByIdAsc(content.id, type)

    private fun fetchAndSaveImages(content: TourismContent) {
        val contentId = content.contentId
        val contentImages = tourApiClient.getImages(contentId, CONTENT_IMAGE_YES)
        val menuImages =
            if (content.isRestaurant()) {
                tourApiClient.getImages(contentId, MENU_IMAGE_NO)
            } else {
                emptyList()
            }
        imageRepository.saveAll(
            contentImages.map { it.toEntity(content, TourismContentImageType.CONTENT) } +
                menuImages.map { it.toEntity(content, TourismContentImageType.MENU) },
        )
    }

    private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)

    private fun TourismContent.isRestaurant(): Boolean = contentType.code == RESTAURANT_CONTENT_TYPE_ID

    companion object {
        private const val COURSE_CONTENT_TYPE_ID = 25
        private const val RESTAURANT_CONTENT_TYPE_ID = 39
        private const val CONTENT_IMAGE_YES = "Y"
        private const val MENU_IMAGE_NO = "N"
    }
}

private fun TourImageItem.toEntity(
    content: TourismContent,
    type: TourismContentImageType,
) = TourismContentImage(
    tourismContent = content,
    type = type,
    imageName = imgname.nullIfBlank(),
    originalImageUrl = originimgurl.nullIfBlank(),
    serialNumber = serialnum.nullIfBlank(),
    copyrightType = cpyrhtDivCd.nullIfBlank(),
)

private fun TourismContent.toSummaryResponse() =
    TourismContentSummaryResponse(
        contentId = contentId,
        contentTypeId = contentType.code,
        title = title,
        address1 = address1,
        address2 = address2,
        thumbnail = thumbnail,
        longitude = longitude,
        latitude = latitude,
    )

private fun TourismContent.toDetailResponse(
    contentImages: List<TourismContentImage>,
    menuImages: List<TourismContentImage>,
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
    thumbnail = thumbnail,
    longitude = longitude,
    latitude = latitude,
    contentImages = contentImages.map { it.toResponse(contentId) },
    menuImages = menuImages.map { it.toResponse(contentId) },
)

private fun TourismContentImage.toResponse(contentId: Long) =
    TourismContentImageResponse(
        contentId = contentId,
        imageName = imageName,
        originalImageUrl = originalImageUrl,
        serialNumber = serialNumber,
        copyrightType = copyrightType,
    )

private fun String.nullIfBlank(): String? = trim().takeIf(String::isNotEmpty)
