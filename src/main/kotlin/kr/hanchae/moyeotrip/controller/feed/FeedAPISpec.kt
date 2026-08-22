package kr.hanchae.moyeotrip.controller.feed

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedCommentRequest
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedRequest
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.controller.feed.response.FeedCommentResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedLikeResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedPageResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "피드", description = "완료한 여행 피드, 댓글 및 좋아요 API")
interface FeedAPISpec {
    @Operation(summary = "여행 피드 작성", description = "완료한 본인 여행 채팅방을 선택해 본문, 공개 범위와 최대 10장의 사진을 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "피드 작성 성공",
                content = [Content(schema = Schema(implementation = FeedResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "사진 또는 피드 입력값이 유효하지 않거나 같은 여행에 이미 피드를 작성함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "완료한 여행의 참가자가 아니어서 피드를 작성할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun createFeed(
        @Parameter(hidden = true) userId: Long,
        request: CreateFeedRequest,
        images: List<MultipartFile>,
    ): ResponseEntity<FeedResponse>

    @Operation(summary = "피드 목록 조회", description = "DISCOVER는 차단 관계가 아닌 전체 공개 피드, FRIENDS는 친구의 전체·친구 공개 피드를 커서 방식으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "피드 목록 조회 성공",
                content = [Content(schema = Schema(implementation = FeedPageResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getFeeds(
        @Parameter(hidden = true) userId: Long,
        tab: FeedTab,
        beforeFeedId: Long?,
        limit: Int,
    ): FeedPageResponse

    @Operation(summary = "피드 상세 조회", description = "공개 범위와 사용자 관계를 확인한 뒤 피드, 여행 정보, 좋아요·댓글 수를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "피드 상세 조회 성공",
                content = [Content(schema = Schema(implementation = FeedResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "차단 관계이거나 피드 공개 범위에 접근할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FEED_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getFeed(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
    ): FeedResponse

    @Operation(summary = "피드 좋아요 토글", description = "호출할 때마다 로그인 사용자의 좋아요를 추가 또는 취소하고, 추가 시 작성자에게 알림을 보냅니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "좋아요 상태 변경 성공",
                content = [Content(schema = Schema(implementation = FeedLikeResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "차단 관계이거나 피드 공개 범위에 접근할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FEED_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun toggleLike(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
    ): FeedLikeResponse

    @Operation(summary = "피드 댓글 목록 조회", description = "최상위 댓글과 각 댓글의 대댓글을 함께 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "댓글 목록 조회 성공",
                content = [Content(schema = Schema(implementation = FeedCommentResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "차단 관계이거나 피드 공개 범위에 접근할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FEED_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getComments(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
    ): List<FeedCommentResponse>

    @Operation(summary = "피드 댓글·대댓글 작성", description = "parentCommentId를 생략하면 댓글, 지정하면 해당 댓글의 대댓글을 작성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "댓글 또는 대댓글 작성 성공",
                content = [Content(schema = Schema(implementation = FeedCommentResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "댓글 입력값이 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "차단 관계이거나 피드 공개 범위에 접근할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드 또는 부모 댓글을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.FEED_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun createComment(
        @Parameter(hidden = true) userId: Long,
        feedId: Long,
        request: CreateFeedCommentRequest,
    ): ResponseEntity<FeedCommentResponse>
}

private object FeedSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val FORBIDDEN = """{"code":40300,"errorMessage":"접근 권한이 없습니다."}"""
    const val FEED_NOT_FOUND = """{"code":40402,"errorMessage":"요청한 리소스를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
}
