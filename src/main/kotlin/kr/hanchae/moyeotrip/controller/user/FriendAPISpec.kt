package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.user.response.FriendListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "친구", description = "친구 요청과 친구 관계 관리 API")
interface FriendAPISpec {
    @Operation(summary = "친구 요청 보내기", description = "상대에게 친구 요청을 보냅니다. 거절된 요청에도 다시 신청할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "친구 요청 전송 성공",
                content = [Content(schema = Schema(implementation = FriendRequestResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "자기 자신에게 요청하거나 친구 관계·반대 방향 요청이 이미 존재함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "자기 자신에게 친구 요청", value = FriendSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "이미 친구인 사용자", value = FriendSwaggerExamples.ALREADY_FRIEND),
                            ExampleObject(name = "상대방이 보낸 요청이 이미 있음", value = FriendSwaggerExamples.REVERSE_REQUEST_EXISTS),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "상대와 차단 관계여서 친구 요청을 보낼 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 또는 친구 요청 대상을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = FriendSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "친구 요청 대상 없음", value = FriendSwaggerExamples.USER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun sendRequest(
        @Parameter(hidden = true) loginUserId: Long,
        @Parameter(description = "친구 요청을 받을 사용자 ID", example = "202")
        userId: Long,
    ): FriendRequestResponse

    @Operation(summary = "받은 친구 요청 수락", description = "받은 친구 요청을 수락하고 양방향 친구 관계를 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "친구 요청 수락 성공",
                content = [Content(schema = Schema(implementation = FriendResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "차단 관계여서 친구 요청을 수락할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "받은 친구 요청을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.REQUEST_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun acceptRequest(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "수락할 받은 친구 요청 ID", example = "45")
        requestId: Long,
    ): FriendResponse

    @Operation(summary = "받은 친구 요청 거절", description = "받은 친구 요청을 삭제합니다. 상대는 이후 다시 친구 요청을 보낼 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "친구 요청 거절 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "받은 친구 요청을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.REQUEST_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun rejectRequest(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "거절할 받은 친구 요청 ID", example = "45")
        requestId: Long,
    )

    @Operation(summary = "보낸 친구 요청 취소", description = "로그인 사용자가 보낸 대기 중 친구 요청을 취소합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "보낸 친구 요청 취소 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "보낸 친구 요청을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.REQUEST_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun cancelRequest(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "취소할 내가 보낸 친구 요청 ID", example = "45")
        requestId: Long,
    )

    @Operation(summary = "받은 친구 요청 목록", description = "로그인 사용자에게 도착한 대기 중 친구 요청을 최신순으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "받은 친구 요청 목록 조회 성공",
                content = [Content(schema = Schema(implementation = FriendRequestListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getReceivedRequests(
        @Parameter(hidden = true) userId: Long,
    ): FriendRequestListResponse

    @Operation(summary = "보낸 친구 요청 목록", description = "로그인 사용자가 보낸 대기 중 친구 요청을 최신순으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "보낸 친구 요청 목록 조회 성공",
                content = [Content(schema = Schema(implementation = FriendRequestListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getSentRequests(
        @Parameter(hidden = true) userId: Long,
    ): FriendRequestListResponse

    @Operation(summary = "친구 목록 조회", description = "친구 프로필과 최근 접속 정보를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "친구 목록 조회 성공",
                content = [Content(schema = Schema(implementation = FriendListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getFriends(
        @Parameter(hidden = true) userId: Long,
    ): FriendListResponse

    @Operation(summary = "친구 삭제", description = "지정한 사용자와의 양방향 친구 관계를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "친구 삭제 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "친구 관계를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = FriendSwaggerExamples.FRIEND_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun deleteFriend(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "친구 관계를 삭제할 상대 사용자 ID", example = "202")
        friendUserId: Long,
    )
}

private object FriendSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val ALREADY_FRIEND = """{"code":40000,"errorMessage":"이미 친구인 사용자입니다."}"""
    const val REVERSE_REQUEST_EXISTS = """{"code":40000,"errorMessage":"상대방이 보낸 친구 요청을 먼저 처리해 주세요."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val FORBIDDEN = """{"code":40300,"errorMessage":"접근 권한이 없습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val REQUEST_NOT_FOUND = """{"code":40402,"errorMessage":"요청한 리소스를 찾을 수 없습니다."}"""
    const val FRIEND_NOT_FOUND = """{"code":40401,"errorMessage":"친구 관계를 찾을 수 없습니다."}"""
}
