package kr.hanchae.moyeotrip.controller.feed

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedCommentRequest
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedRequest
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.controller.feed.response.FeedCommentResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedLikeResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedPageResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "피드", description = "완료한 여행 피드, 댓글 및 좋아요 API")
@SecurityRequirement(name = "Authorization")
interface FeedAPISpec {
    @Operation(summary = "여행 피드 작성", description = "완료한 본인 여행 채팅방을 선택해 본문, 공개 범위와 최대 10장의 사진을 등록합니다.")
    fun createFeed(
        @Parameter(hidden = true) userId: Long,
        request: CreateFeedRequest,
        images: List<MultipartFile>,
    ): ResponseEntity<FeedResponse>

    @Operation(summary = "피드 목록 조회", description = "DISCOVER는 차단 관계가 아닌 전체 공개 피드, FRIENDS는 친구의 전체·친구 공개 피드를 커서 방식으로 반환합니다.")
    fun getFeeds(
        @Parameter(hidden = true) userId: Long,
        tab: FeedTab,
        beforeFeedId: Long?,
        limit: Int,
    ): FeedPageResponse

    @Operation(summary = "피드 상세 조회", description = "공개 범위와 사용자 관계를 확인한 뒤 피드, 여행 정보, 좋아요·댓글 수를 반환합니다.")
    fun getFeed(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
    ): FeedResponse

    @Operation(summary = "피드 좋아요 토글", description = "호출할 때마다 로그인 사용자의 좋아요를 추가 또는 취소하고, 추가 시 작성자에게 알림을 보냅니다.")
    fun toggleLike(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
    ): FeedLikeResponse

    @Operation(summary = "피드 댓글 목록 조회", description = "최상위 댓글과 각 댓글의 대댓글을 함께 반환합니다.")
    fun getComments(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
    ): List<FeedCommentResponse>

    @Operation(summary = "피드 댓글·대댓글 작성", description = "parentCommentId를 생략하면 댓글, 지정하면 해당 댓글의 대댓글을 작성합니다.")
    fun createComment(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
        request: CreateFeedCommentRequest,
    ): ResponseEntity<FeedCommentResponse>
}
