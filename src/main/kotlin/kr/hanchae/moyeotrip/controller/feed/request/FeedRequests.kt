package kr.hanchae.moyeotrip.controller.feed.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility

data class CreateFeedRequest(
    val chatRoomId: Long,
    @field:NotBlank @field:Size(max = 500)
    val content: String,
    val visibility: FeedVisibility,
)

data class CreateFeedCommentRequest(
    @field:NotBlank @field:Size(max = 500)
    val content: String,
    val parentCommentId: Long? = null,
)

enum class FeedTab {
    FRIENDS,
    DISCOVER,
}
