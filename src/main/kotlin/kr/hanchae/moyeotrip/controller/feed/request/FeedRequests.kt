package kr.hanchae.moyeotrip.controller.feed.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility

@Schema(description = "여행 피드 작성 요청. 사진은 multipart images 파트로 최대 10장을 함께 전송합니다.")
data class CreateFeedRequest(
    @field:Schema(description = "피드로 기록할 완료 여행 채팅방 ID", example = "101")
    val chatRoomId: Long,
    @field:Schema(description = "피드 본문", example = "주왕산 단풍이 정말 아름다웠어요!")
    @field:NotBlank
    @field:Size(max = 500)
    val content: String,
    @field:Schema(description = "피드 공개 범위", example = "PUBLIC")
    val visibility: FeedVisibility,
)

@Schema(description = "피드 댓글 또는 대댓글 작성 요청")
data class CreateFeedCommentRequest(
    @field:Schema(description = "댓글 본문", example = "다음에 저도 가보고 싶어요!")
    @field:NotBlank
    @field:Size(max = 500)
    val content: String,
    @field:Schema(description = "대댓글을 작성할 부모 댓글 ID. 생략하면 최상위 댓글입니다.", example = "45", nullable = true)
    val parentCommentId: Long? = null,
)

@Schema(description = "피드 조회 탭", allowableValues = ["FRIENDS", "DISCOVER"])
enum class FeedTab {
    FRIENDS,
    DISCOVER,
}
