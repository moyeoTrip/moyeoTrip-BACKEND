package kr.hanchae.moyeotrip.controller.tour.response

import io.swagger.v3.oas.annotations.media.Schema

data class TourismContentTypeResponse(
    val contentTypeId: Int,
    val contentTypeName: String,
)

data class TourismContentPageResponse(
    val items: List<TourismContentSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class TourismContentSummaryResponse(
    val contentId: Long,
    val contentTypeId: Int,
    val title: String,
    val address1: String?,
    val address2: String?,
    val thumbnail: String?,
    val longitude: Double?,
    val latitude: Double?,
)

data class TourismContentDetailResponse(
    val contentId: Long,
    val contentTypeId: Int,
    val title: String,
    val address1: String?,
    val address2: String?,
    val zipcode: String?,
    val telephone: String?,
    val telephoneName: String?,
    val homepage: String?,
    val overview: String?,
    val thumbnail: String?,
    val longitude: Double?,
    val latitude: Double?,
    val contentImages: List<TourismContentImageResponse>,
    @field:Schema(
        description = "음식점(contentTypeId=39)에서만 제공되는 메뉴판 이미지 목록. 음식점이 아니면 빈 배열([])",
    )
    val menuImages: List<TourismContentImageResponse>,
)

data class TourismContentImageResponse(
    val contentId: Long,
    val imageName: String?,
    val originalImageUrl: String?,
    val serialNumber: String?,
    val copyrightType: String?,
)
