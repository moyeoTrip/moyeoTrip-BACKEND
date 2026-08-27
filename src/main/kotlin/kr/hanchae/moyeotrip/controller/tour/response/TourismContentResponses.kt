package kr.hanchae.moyeotrip.controller.tour.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관광 콘텐츠 유형 정보")
data class TourismContentTypeResponse(
    @field:Schema(description = "한국관광공사 관광 콘텐츠 유형 ID", example = "12")
    val contentTypeId: Int,
    @field:Schema(description = "관광 콘텐츠 유형명", example = "관광지")
    val contentTypeName: String,
)

@Schema(description = "관광 콘텐츠 페이지 조회 응답")
data class TourismContentPageResponse(
    @field:Schema(description = "현재 페이지의 관광 콘텐츠 목록")
    val items: List<TourismContentSummaryResponse>,
    @field:Schema(description = "1부터 시작하는 현재 페이지 번호", example = "1")
    val page: Int,
    @field:Schema(description = "현재 페이지 크기", example = "20")
    val size: Int,
    @field:Schema(description = "전체 관광 콘텐츠 수", example = "1250")
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "63")
    val totalPages: Int,
)

@Schema(description = "관광 콘텐츠 목록 요약 정보")
data class TourismContentSummaryResponse(
    @field:Schema(description = "TourismContent ID", example = "126508")
    val contentId: Long,
    @field:Schema(description = "한국관광공사 관광 콘텐츠 유형 ID", example = "12")
    val contentTypeId: Int,
    @field:Schema(description = "관광 장소명", example = "주산지")
    val title: String,
    @field:Schema(description = "기본 주소", nullable = true)
    val address1: String?,
    @field:Schema(description = "상세 주소", nullable = true)
    val address2: String?,
    @field:Schema(description = "목록 대표 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "경도. 좌표 미제공이면 null", nullable = true)
    val longitude: Double?,
    @field:Schema(description = "위도. 좌표 미제공이면 null", nullable = true)
    val latitude: Double?,
)

@Schema(description = "관광 콘텐츠 상세 정보")
data class TourismContentDetailResponse(
    @field:Schema(description = "TourismContent ID", example = "126508")
    val contentId: Long,
    @field:Schema(description = "한국관광공사 관광 콘텐츠 유형 ID", example = "12")
    val contentTypeId: Int,
    @field:Schema(description = "관광 장소명", example = "주산지")
    val title: String,
    @field:Schema(description = "기본 주소", nullable = true)
    val address1: String?,
    @field:Schema(description = "상세 주소", nullable = true)
    val address2: String?,
    @field:Schema(description = "우편번호", nullable = true)
    val zipcode: String?,
    @field:Schema(description = "전화번호", nullable = true)
    val telephone: String?,
    @field:Schema(description = "전화번호 안내 명칭", nullable = true)
    val telephoneName: String?,
    @field:Schema(description = "공식 홈페이지 URL 또는 HTML", nullable = true)
    val homepage: String?,
    @field:Schema(description = "관광 장소 소개", nullable = true)
    val overview: String?,
    @field:Schema(description = "대표 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "경도. 좌표 미제공이면 null", nullable = true)
    val longitude: Double?,
    @field:Schema(description = "위도. 좌표 미제공이면 null", nullable = true)
    val latitude: Double?,
    @field:Schema(description = "관광 콘텐츠 일반 이미지 목록")
    val contentImages: List<TourismContentImageResponse>,
    @field:Schema(
        description = "음식점(contentTypeId=39)에서만 제공되는 메뉴판 이미지 목록. 음식점이 아니면 빈 배열([])",
    )
    val menuImages: List<TourismContentImageResponse>,
)

@Schema(description = "관광 콘텐츠 이미지 정보")
data class TourismContentImageResponse(
    @field:Schema(description = "이미지가 연결된 TourismContent ID", example = "126508")
    val contentId: Long,
    @field:Schema(description = "이미지 파일명", nullable = true)
    val imageName: String?,
    @field:Schema(description = "원본 이미지 URL", nullable = true)
    val originalImageUrl: String?,
)
