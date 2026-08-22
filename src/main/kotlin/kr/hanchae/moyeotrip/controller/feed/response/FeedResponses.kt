package kr.hanchae.moyeotrip.controller.feed.response

import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class FeedPageResponse(
    val feeds: List<FeedResponse>,
    val nextId: Long?,
)

data class FeedResponse(
    val feedId: Long,
    val author: FeedAuthorResponse,
    val content: String,
    val visibility: FeedVisibility,
    val images: List<FeedImageResponse>,
    val trip: FeedTripResponse,
    val likeCount: Long,
    val commentCount: Long,
    val liked: Boolean,
    val createdAt: LocalDateTime,
)

data class FeedAuthorResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

data class FeedImageResponse(
    val imageId: Long,
    val imageUrl: String,
    val sequence: Int,
)

data class FeedTripResponse(
    val chatRoomId: Long,
    val courseId: Long,
    val courseTitle: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val places: List<FeedPlaceResponse>,
)

data class FeedPlaceResponse(
    val tourismContentId: Long,
    val title: String,
    val latitude: Double?,
    val longitude: Double?,
    val dayNumber: Int,
    val sequence: Int,
    val visitTime: LocalTime?,
)

data class FeedLikeResponse(
    val liked: Boolean,
    val likeCount: Long,
)

data class FeedCommentResponse(
    val commentId: Long,
    val author: FeedAuthorResponse,
    val content: String,
    val createdAt: LocalDateTime,
    val replies: List<FeedCommentResponse> = emptyList(),
)
