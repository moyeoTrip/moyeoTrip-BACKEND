package kr.hanchae.moyeotrip.controller.tour.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 사용자가 찜한 공개 여행 코스 정보")
data class LikedTravelCourseResponse(
    @field:Schema(description = "여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "여행 코스 제목", example = "주왕산 단풍길 코스")
    val title: String,
    @field:Schema(description = "여행 코스 소개", nullable = true)
    val description: String?,
    @field:Schema(description = "여행 코스 대표 썸네일 URL", nullable = true)
    val thumbnail: String?,
    @field:Schema(description = "공개 코스 태그 목록")
    val tags: List<LikedTravelCourseTagResponse>,
)

@Schema(description = "찜한 여행 코스의 태그 정보")
data class LikedTravelCourseTagResponse(
    @field:Schema(description = "여행 코스 태그 ID", example = "1")
    val tagId: Long,
    @field:Schema(description = "여행 코스 태그명", example = "자연")
    val name: String,
)
