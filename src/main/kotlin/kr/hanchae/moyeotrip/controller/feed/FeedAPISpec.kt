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
                description = "사진 개수·형식 또는 피드 입력값이 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "사진 개수 오류", value = FeedSwaggerExamples.INVALID_IMAGE_COUNT),
                            ExampleObject(name = "사진 파일 오류", value = FeedSwaggerExamples.INVALID_IMAGE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "완료한 여행의 참가자가 아니어서 피드를 작성할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.COMPLETED_TRIP_FEED_REQUIRED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 또는 완료한 여행 채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = FeedSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "채팅방 없음", value = FeedSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "같은 여행에 이미 피드를 작성함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FeedSwaggerExamples.DUPLICATE_TRIP_FEED)],
                    ),
                ],
            ),
        ],
    )
    fun createFeed(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "피드 본문, 여행 채팅방 ID 및 공개 범위를 담은 request JSON 파트", required = true)
        request: CreateFeedRequest,
        @Parameter(description = "사진 파일 목록. 1~10장의 이미지 파일을 images 파트로 전송합니다.", required = true)
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
        @Parameter(description = "피드 탭. DISCOVER=차단 관계가 아닌 전체 공개 피드, FRIENDS=친구 피드", example = "DISCOVER")
        tab: FeedTab,
        @Parameter(description = "이 ID보다 오래된 피드부터 조회하는 커서. 첫 페이지는 생략합니다.", example = "100")
        beforeFeedId: Long?,
        @Parameter(description = "반환할 최대 피드 수. 기본값은 20입니다.", example = "20")
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
                        examples = [
                            ExampleObject(name = "차단 관계", value = FeedSwaggerExamples.USER_BLOCK_RELATIONSHIP),
                            ExampleObject(name = "공개 범위 제한", value = FeedSwaggerExamples.FEED_NOT_VISIBLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드 또는 로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "피드 없음", value = FeedSwaggerExamples.FEED_NOT_FOUND),
                            ExampleObject(name = "로그인 사용자 없음", value = FeedSwaggerExamples.USER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getFeed(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "상세 조회할 피드 ID", example = "100")
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
                        examples = [
                            ExampleObject(name = "차단 관계", value = FeedSwaggerExamples.USER_BLOCK_RELATIONSHIP),
                            ExampleObject(name = "공개 범위 제한", value = FeedSwaggerExamples.FEED_NOT_VISIBLE),
                        ],
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
        @Parameter(description = "좋아요 상태를 변경할 피드 ID", example = "100")
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
                        examples = [
                            ExampleObject(name = "차단 관계", value = FeedSwaggerExamples.USER_BLOCK_RELATIONSHIP),
                            ExampleObject(name = "공개 범위 제한", value = FeedSwaggerExamples.FEED_NOT_VISIBLE),
                        ],
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
        @Parameter(description = "댓글을 조회할 피드 ID", example = "100")
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
                        examples = [
                            ExampleObject(name = "차단 관계", value = FeedSwaggerExamples.USER_BLOCK_RELATIONSHIP),
                            ExampleObject(name = "공개 범위 제한", value = FeedSwaggerExamples.FEED_NOT_VISIBLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드, 부모 댓글 또는 로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "피드 없음", value = FeedSwaggerExamples.FEED_NOT_FOUND),
                            ExampleObject(name = "부모 댓글 없음", value = FeedSwaggerExamples.PARENT_COMMENT_NOT_FOUND),
                            ExampleObject(name = "로그인 사용자 없음", value = FeedSwaggerExamples.USER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createComment(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "댓글 또는 대댓글을 작성할 피드 ID", example = "100")
        feedId: Long,
        @Parameter(description = "댓글 본문과 선택적 부모 댓글 ID", required = true)
        request: CreateFeedCommentRequest,
    ): ResponseEntity<FeedCommentResponse>
}

private object FeedSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val INVALID_IMAGE_COUNT = """{"code":40026,"errorMessage":"피드 사진은 1장 이상 10장 이하로 등록해야 합니다."}"""
    const val INVALID_IMAGE = """{"code":40027,"errorMessage":"피드 사진은 비어 있지 않은 20MB 이하 이미지 파일이어야 합니다."}"""
    const val DUPLICATE_TRIP_FEED = """{"code":40916,"errorMessage":"같은 여행에는 피드를 한 번만 작성할 수 있습니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val COMPLETED_TRIP_FEED_REQUIRED = """{"code":40304,"errorMessage":"여행이 완료되지 않아 피드를 작성할 수 없습니다."}"""
    const val USER_BLOCK_RELATIONSHIP = """{"code":40305,"errorMessage":"차단 관계인 사용자와는 이 작업을 할 수 없습니다."}"""
    const val FEED_NOT_VISIBLE = """{"code":40306,"errorMessage":"이 피드는 현재 사용자에게 공개되지 않았습니다."}"""
    const val FEED_NOT_FOUND = """{"code":40417,"errorMessage":"피드를 찾을 수 없습니다."}"""
    const val PARENT_COMMENT_NOT_FOUND = """{"code":40418,"errorMessage":"답글을 달 원본 댓글을 찾을 수 없습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
}
