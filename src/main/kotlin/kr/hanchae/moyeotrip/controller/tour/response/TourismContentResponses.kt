package kr.hanchae.moyeotrip.controller.tour.response

import com.fasterxml.jackson.databind.JsonNode

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
    val firstImageUrl: String?,
    val firstThumbnailUrl: String?,
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
    val bookTour: String?,
    val overview: String?,
    val firstImageUrl: String?,
    val firstThumbnailUrl: String?,
    val longitude: Double?,
    val latitude: Double?,
    val introDetails: JsonNode,
    val additionalDetails: JsonNode,
    val contentImages: JsonNode,
    val menuImages: JsonNode,
)
