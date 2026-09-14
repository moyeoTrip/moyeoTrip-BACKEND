package kr.hanchae.moyeotrip.controller.feed.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.feed.FeedReportReason
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

@Schema(description = "피드 본문 수정 요청")
data class UpdateFeedRequest(
    @field:Schema(description = "수정할 피드 본문", example = "주왕산 단풍이 정말 아름다웠어요!")
    @field:NotBlank
    @field:Size(max = 500)
    val content: String,
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

@Schema(description = "피드 댓글 또는 대댓글 수정 요청")
data class UpdateFeedCommentRequest(
    @field:Schema(description = "수정할 댓글 본문", example = "다음에 저도 꼭 가보고 싶어요!")
    @field:NotBlank
    @field:Size(max = 500)
    val content: String,
)

@Schema(description = "피드 신고 요청")
data class CreateFeedReportRequest(
    @field:Schema(description = "신고 사유", example = "SPAM")
    val reason: FeedReportReason,
    @field:Schema(description = "기타 신고 상세 내용", nullable = true, maxLength = 300)
    @field:Size(max = 300)
    val details: String? = null,
)

@Schema(
    description = "피드 조회 탭. FRIENDS=내 친구의 전체·친구 공개 피드, DISCOVER=차단 관계가 아닌 전체 공개 피드, MINE=내가 쓴 피드",
    allowableValues = ["FRIENDS", "DISCOVER", "MINE"],
)
enum class FeedTab {
    FRIENDS,
    DISCOVER,

    // BE-28 · 「내 피드」 화면이 그릴 것이 없어 피드 탭 전체로 떨어지던 것을 막는다. 공개 범위와 무관하게 내 것만 준다.
    MINE,
}
