package kr.hanchae.moyeotrip.controller.chat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatPollRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateSettlementMemoRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.KickChatRoomMemberRequest
import kr.hanchae.moyeotrip.controller.chat.request.MyChatRoomFilter
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.request.ShareTourismContentRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateChatRoomStatusRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateMeetingInfoRequest
import kr.hanchae.moyeotrip.controller.chat.response.ApproveJoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessagePageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomFavoriteResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomKickHistoryResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomMemberListResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomNoticeHistoryResponse
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomNoticeResponse
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.CurrentTravelRoadmapResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.MapChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyChatRoomSummaryResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyWaitingChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.SearchChatRoomResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "채팅방", description = "여행 채팅방, 참가자, 대기열 및 메시지 API")
interface ChatRoomAPISpec {
    @Operation(
        summary = "채팅방 생성",
        description = """
            생성한 사용자가 호스트이자 첫 참가자가 됩니다.
            `DAY_TRIP`은 종료 날짜 없이 시작·종료 시각을, `OVERNIGHT`은 시작일 이후의 종료 날짜만 입력합니다.
            `PUBLIC` 코스는 courseId만, `CUSTOM` 코스는 customCourse만 입력합니다.
            thumbnail 파트는 20MB 이하 이미지 파일로 전송하며, 서버가 비율을 유지한 최대 FHD(1920×1080) WebP로 변환해 저장합니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅방 생성 성공",
                content = [Content(schema = Schema(implementation = CreateChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 본문·필수 썸네일·일정·나이 또는 커스텀 코스 구성 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 본문 또는 enum 값 오류", value = ChatRoomSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "필수 썸네일 오류", value = ChatRoomSwaggerExamples.CHAT_ROOM_THUMBNAIL_REQUIRED),
                            ExampleObject(name = "썸네일 파일 오류", value = ChatRoomSwaggerExamples.INVALID_CHAT_ROOM_THUMBNAIL),
                            ExampleObject(name = "당일·숙박 일정 입력 오류", value = ChatRoomSwaggerExamples.INVALID_TRIP_SCHEDULE),
                            ExampleObject(name = "최소 출발 인원 오류", value = ChatRoomSwaggerExamples.INVALID_MINIMUM_PARTICIPANTS),
                            ExampleObject(name = "과거 여행 시작일 오류", value = ChatRoomSwaggerExamples.PAST_CHAT_ROOM_START_DATE),
                            ExampleObject(name = "과거 모집 마감일 오류", value = ChatRoomSwaggerExamples.PAST_RECRUITMENT_DEADLINE_DATE),
                            ExampleObject(name = "모집 마감일 오류", value = ChatRoomSwaggerExamples.INVALID_RECRUITMENT_DEADLINE),
                            ExampleObject(name = "참가 나이 범위 오류", value = ChatRoomSwaggerExamples.INVALID_CHAT_ROOM_AGE_RESTRICTION),
                            ExampleObject(name = "커스텀 코스 일차·순서 구성 오류", value = ChatRoomSwaggerExamples.INVALID_TRAVEL_COURSE_SCHEDULE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자, 공개 코스, 커스텀 코스 장소 또는 코스 태그를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = ChatRoomSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "공개 여행 코스 없음", value = ChatRoomSwaggerExamples.TRAVEL_COURSE_NOT_FOUND),
                            ExampleObject(name = "커스텀 코스 관광지 없음", value = ChatRoomSwaggerExamples.TOURISM_CONTENT_NOT_FOUND),
                            ExampleObject(name = "커스텀 코스 태그 없음", value = ChatRoomSwaggerExamples.TRAVEL_COURSE_TAG_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "PUBLIC 코스와 CUSTOM 코스 입력 조합이 올바르지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.INVALID_TRAVEL_COURSE_SELECTION)],
                    ),
                ],
            ),
        ],
    )
    fun createRoom(
        @Parameter(hidden = true) userId: Long,
        @RequestBody(
            description = "채팅방 생성 정보와 필수 썸네일을 multipart/form-data로 전송합니다. request 파트는 application/json입니다.",
            required = true,
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = [Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)],
                ),
            ],
        )
        @Parameter(
            description = "채팅방 생성 JSON. 아래 예시는 바로 실행 가능한 1박 2일 커스텀 코스입니다.",
        )
        request: CreateChatRoomRequest,
        @Parameter(description = "필수 채팅방 썸네일 이미지 파일", required = true)
        thumbnail: MultipartFile,
    ): ResponseEntity<CreateChatRoomResponse>

    @Operation(summary = "내 채팅방 목록", description = "모집중·확정·종료 상태로 필터링하며 인원, 마감 D-day, 안 읽은 수와 최근 메시지를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 채팅방 목록 조회 성공",
                content = [Content(schema = Schema(implementation = MyChatRoomSummaryResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 또는 사용자 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "최근 메시지가 없는 활성 채팅방이 포함됨",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NO_MESSAGES)],
                    ),
                ],
            ),
        ],
    )
    fun getMyRooms(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "목록 상태 필터. ALL=모든 방, RECRUITING=모집 중, CONFIRMED=여행 확정, ENDED=종료된 여행", example = "ALL")
        filter: MyChatRoomFilter,
    ): List<MyChatRoomSummaryResponse>

    @Operation(summary = "내 신청중 채팅방 목록", description = "호스트 승인 대기(PENDING)와 승인 후 자리 대기(WAITLISTED) 신청만 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 신청중 채팅방 목록 조회 성공",
                content = [Content(schema = Schema(implementation = MyWaitingChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 또는 사용자 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getMyWaitingRooms(
        @Parameter(hidden = true) userId: Long,
    ): List<MyWaitingChatRoomResponse>

    @Operation(
        summary = "모임 검색",
        description =
            "참가 가능한 모집 중 모임만 반환합니다. 제목·소개·코스 제목·코스 태그·방문지 이름·주소(지역)로 검색하며, " +
                "차단 관계인 사용자가 호스트 또는 참가자인 모임은 제외합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "모임 검색 성공",
                content = [Content(schema = Schema(implementation = SearchChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 또는 검색 조건 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun searchRooms(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "제목·소개·코스·태그·방문지·지역 주소에 포함되어야 하는 검색어. 생략하면 전체 모집을 조회합니다.", example = "청송")
        keyword: String?,
        @Parameter(description = "반환할 최대 개수. 기본값은 20입니다.", example = "20")
        limit: Int,
    ): List<SearchChatRoomResponse>

    @Operation(
        summary = "지도 반경 내 모임 조회",
        description =
            "입력 좌표로부터 지정 반경 안에 집합 장소가 있는 모집 중 모임을 가까운 순서로 반환합니다. " +
                "집합 좌표가 없거나 모집이 마감된 모임, 차단 관계인 사용자가 포함된 모임과 이미 참여 중인 모임은 제외합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지도 반경 내 모임 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = MapChatRoomResponse::class)))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "위도, 경도 또는 반경이 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.INVALID_MAP_SEARCH_AREA)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getMapRooms(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "지도 중심 위도(-90~90)", example = "36.5684") latitude: Double,
        @Parameter(description = "지도 중심 경도(-180~180)", example = "128.7294") longitude: Double,
        @Parameter(description = "검색 반경(km). 0보다 커야 하며 상한은 없습니다.", example = "5") radiusKm: Double,
    ): List<MapChatRoomResponse>

    @Operation(summary = "모임 제목 검색", description = "참가 가능한 모집 중 모임 가운데 채팅방 제목에만 검색어가 포함된 모임을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "모임 제목 검색 성공",
                content = [Content(schema = Schema(implementation = SearchChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun searchRoomsByTitle(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "채팅방 제목에 포함될 검색어. 생략하면 전체 모집을 조회합니다.", example = "바다") keyword: String?,
        @Parameter(description = "반환할 최대 개수. 기본값은 20입니다.", example = "20") limit: Int,
    ): List<SearchChatRoomResponse>

    @Operation(summary = "코스 태그로 모임 검색", description = "참가 가능한 모집 중 모임 가운데 연결 코스에 지정한 태그가 있는 모임을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "코스 태그 모임 검색 성공",
                content = [Content(schema = Schema(implementation = SearchChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun searchRoomsByCourseTag(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "필터링할 여행 코스 태그 ID. GET /api/v1/travel-courses/tags의 tagId를 사용합니다.", example = "3") tagId: Long,
        @Parameter(description = "반환할 최대 개수. 기본값은 20입니다.", example = "20") limit: Int,
    ): List<SearchChatRoomResponse>

    @Operation(summary = "채팅방 상세 조회", description = "모집·여행 정보, 호스트, 참가자와 최신 고정 공지를 반환합니다. 종료 후 2주가 지난 채팅방은 조회할 수 없습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 상세 조회 성공",
                content = [Content(schema = Schema(implementation = ChatRoomDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "인증 실패, 채팅방 없음 또는 보존 기간 만료",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getRoom(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): ChatRoomDetailResponse

    @Operation(summary = "채팅방 찜 상태 토글", description = "호출할 때마다 찜 상태를 반전하고 변경된 상태를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 찜 상태 변경 성공",
                content = [Content(schema = Schema(implementation = ChatRoomFavoriteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 또는 채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = ChatRoomSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun toggleRoomFavorite(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "찜 상태를 바꿀 채팅방 ID", example = "101")
        roomId: Long,
    ): ChatRoomFavoriteResponse

    @Operation(summary = "내가 찜한 채팅방 목록", description = "로그인 사용자가 찜한 채팅방을 가장 최근에 찜한 순서로 반환합니다. 종료된 채팅방도 찜을 해제하기 전까지 목록에 포함됩니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "찜한 채팅방 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = SearchChatRoomResponse::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getFavoriteRooms(
        @Parameter(hidden = true) userId: Long,
    ): List<SearchChatRoomResponse>

    @Operation(summary = "집합 정보 수정", description = "여행 확정 전까지 채팅방 호스트가 집합 좌표, 상세 안내와 시간을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "집합 정보 수정 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "400",
                description = "집합 좌표를 위도·경도 중 하나만 입력했거나 집합일이 여행 시작일 이후임",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.INVALID_MEETING_INFORMATION)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "여행 확정 후라 집합 정보를 수정할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.MEETING_INFO_NOT_EDITABLE)],
                    ),
                ],
            ),
        ],
    )
    fun updateMeetingInfo(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "집합 정보를 수정할 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "수정할 집합 좌표, 안내 문구 및 집합 일시", required = true)
        request: UpdateMeetingInfoRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 참가 신청", description = "소개를 작성해 호스트의 승인을 기다립니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 참가 신청 성공",
                content = [Content(schema = Schema(implementation = JoinChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 본문이 유효하지 않거나 수동 승인 모임에 전할 말을 입력하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 본문 검증 실패", value = ChatRoomSwaggerExamples.BAD_REQUEST),
                            ExampleObject(
                                name = "수동 승인 모임의 신청 메시지 누락",
                                value = ChatRoomSwaggerExamples.CHAT_JOIN_APPLICATION_MESSAGE_REQUIRED,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 또는 채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = ChatRoomSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "성별 또는 만 나이 참가 조건을 충족하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_JOIN_CONDITION_NOT_MET)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 참가·신청했거나 모집이 종료됨",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "이미 참가 또는 신청함", value = ChatRoomSwaggerExamples.CHAT_ROOM_ALREADY_JOINED),
                            ExampleObject(name = "모집 종료", value = ChatRoomSwaggerExamples.CHAT_ROOM_CLOSED),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun applyToJoin(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "참가 신청할 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "수동 승인 모임에 전달할 선택적 소개 메시지", required = true)
        request: JoinChatRoomRequest,
    ): JoinChatRoomResponse

    @Operation(summary = "채팅방 참가 신청 취소", description = "호스트 승인 대기 또는 승인 후 대기열에 있는 본인의 신청을 취소합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "채팅방 참가 신청 취소 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 취소할 참가 신청을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "참가 신청 없음", value = ChatRoomSwaggerExamples.CHAT_JOIN_APPLICATION_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "인증·채팅방 또는 본인 참가 신청 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_JOINED)],
                    ),
                ],
            ),
        ],
    )
    fun cancelJoinApplication(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "참가 신청을 취소할 채팅방 ID", example = "101")
        roomId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "승인 대기 신청 목록", description = "호스트에게만 신청자의 프로필과 소개를 제공합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "승인 대기 신청 목록 조회 성공",
                content = [Content(schema = Schema(implementation = JoinApplicationResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "인증·권한 또는 채팅방 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
        ],
    )
    fun getApplications(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "승인 대기 신청을 조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): List<JoinApplicationResponse>

    @Operation(summary = "참가 신청 승인", description = "정원 내면 참가, 정원이 찼으면 승인된 대기열로 이동합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "참가 신청 승인 성공",
                content = [Content(schema = Schema(implementation = ApproveJoinApplicationResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 승인 대기 참가 신청을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "승인 대기 참가 신청 없음", value = ChatRoomSwaggerExamples.CHAT_JOIN_APPLICATION_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun approveApplication(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "참가 신청이 등록된 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "승인할 참가 신청 ID", example = "45")
        applicationId: Long,
    ): ApproveJoinApplicationResponse

    @Operation(summary = "참가 신청 거절", description = "호스트가 승인 대기 중인 참가 신청을 거절하고 신청 이력을 제거합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "참가 신청 거절 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 승인 대기 참가 신청을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "승인 대기 참가 신청 없음", value = ChatRoomSwaggerExamples.CHAT_JOIN_APPLICATION_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun rejectApplication(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "참가 신청이 등록된 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "거절할 참가 신청 ID", example = "45")
        applicationId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 나가기 또는 대기 취소", description = "참가자가 나가면 대기열 1순위가 자동 참가합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 나가기 또는 대기 취소 성공",
                content = [Content(schema = Schema(implementation = LeaveChatRoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "인증·채팅방 또는 참가·대기 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_JOINED)],
                    ),
                ],
            ),
        ],
    )
    fun leaveRoom(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "나가거나 대기열을 취소할 채팅방 ID", example = "101")
        roomId: Long,
    ): LeaveChatRoomResponse

    @Operation(summary = "채팅방 동행자 목록", description = "현재·최대 인원, 승인된 대기 인원과 각 동행자의 프로필·닉네임·완료 여행 횟수를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 동행자 목록 조회 성공",
                content = [Content(schema = Schema(implementation = ChatRoomMemberListResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "인증 또는 채팅방 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getMembers(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "동행자 목록을 조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): ChatRoomMemberListResponse

    @Operation(summary = "내 강퇴 이력", description = "로그인한 본인이 강퇴된 사유만 최신순으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 강퇴 이력 조회 성공",
                content = [Content(schema = Schema(implementation = ChatRoomKickHistoryResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getMyKickHistories(
        @Parameter(hidden = true) userId: Long,
    ): List<ChatRoomKickHistoryResponse>

    @Operation(
        summary = "멤버 강퇴",
        description = "필수 사유를 강퇴 이력으로 저장하고 빈자리에 승인된 대기자를 승격합니다. 사유는 강퇴된 당사자만 조회할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "멤버 강퇴 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "400",
                description = "강퇴 사유가 비어 있음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.KICK_REASON_BLANK)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 강퇴할 일반 멤버를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "강퇴할 일반 멤버 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_MEMBER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 호스트가 아니어서 멤버를 강퇴할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_HOST_REQUIRED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 강퇴할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun kickMember(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "강퇴를 처리할 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "강퇴할 일반 참가자 사용자 ID. 호스트는 강퇴할 수 없습니다.", example = "202")
        memberId: Long,
        @RequestBody(description = "필수 강퇴 사유", required = true)
        request: KickChatRoomMemberRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "여행 상태 변경", description = "호스트가 모집 중인 여행을 확정 또는 불발 처리합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "여행 상태 변경 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "409",
                description = "인증·권한·입력 검증 또는 채팅방 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.INVALID_CHAT_ROOM_STATUS)],
                    ),
                ],
            ),
        ],
    )
    fun changeStatus(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "여행 상태를 변경할 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "변경할 채팅방 여행 상태", required = true)
        request: UpdateChatRoomStatusRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 등록", description = "호스트가 공지를 등록하며 pinned를 true로 지정하면 상단 고정 공지로 표시합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅방 공지 등록 성공",
                content = [Content(schema = Schema(implementation = CreateChatRoomNoticeResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "공지 내용이 비어 있음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.NOTICE_CONTENT_BLANK)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 호스트가 아니어서 공지를 등록할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_HOST_REQUIRED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에는 공지를 등록할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun createNotice(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "공지를 등록할 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "공지 내용과 상단 고정 여부", required = true)
        request: CreateChatRoomNoticeRequest,
    ): ResponseEntity<CreateChatRoomNoticeResponse>

    @Operation(summary = "채팅방 공지 변경·삭제", description = "내용과 고정 상태를 변경합니다. notice와 pinned가 모두 null이면 공지를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "채팅방 공지 변경 또는 삭제 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "400",
                description = "공지 내용을 빈 문자열로 변경하려 함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.NOTICE_CONTENT_BLANK)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 공지를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "공지 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOTICE_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 호스트가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_HOST_REQUIRED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에는 공지를 변경하거나 삭제할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun updateNotice(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "공지의 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "수정할 공지 ID", example = "301")
        noticeId: Long,
        @RequestBody(description = "변경할 공지 내용과 고정 여부", required = true)
        request: UpdateChatRoomNoticeRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 삭제", description = "채팅방 호스트가 공지를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "채팅방 공지 삭제 성공"),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 호스트가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_HOST_REQUIRED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방 또는 공지를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "공지 없음", value = ChatRoomSwaggerExamples.CHAT_ROOM_NOTICE_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에는 공지를 삭제할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun deleteNotice(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "공지의 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "삭제할 공지 ID", example = "301")
        noticeId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 이력", description = "고정 공지와 고정하지 않은 공지를 각각 생성일 내림차순으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 공지 이력 조회 성공",
                content = [Content(schema = Schema(implementation = ChatRoomNoticeHistoryResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "인증 또는 채팅방 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getNoticeHistory(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "공지 이력을 조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): ChatRoomNoticeHistoryResponse

    @Operation(summary = "채팅 메시지 전송", description = "현재 참가자만 메시지를 보낼 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅 메시지 전송 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "멘션 대상 중 채팅방 참가자가 아닌 사용자가 있음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.MENTIONED_USER_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "답글 대상 메시지를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_REPLY_MESSAGE_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 메시지를 보낼 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun sendMessage(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "메시지를 보낼 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "메시지 본문, 멘션 대상과 선택적 답글 대상", required = true)
        request: SendChatMessageRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 사진 공유", description = "현재 참가자가 최대 20MB 이미지 한 장을 공유합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅 사진 공유 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "빈 파일, 20MB 초과 파일 또는 이미지가 아닌 파일",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.INVALID_CHAT_IMAGE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 사진을 공유할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun shareImage(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "사진을 공유할 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "20MB 이하 이미지 파일 한 장", required = true)
        image: MultipartFile,
        @Parameter(description = "선택 사진 설명. 생략하면 사진으로 표시됩니다.", example = "주왕산 정상 풍경")
        caption: String?,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 관광 장소 공유", description = "현재 참가자가 TourismContent ID로 선택한 관광 장소 카드를 메시지로 공유합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅 관광 장소 공유 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공유할 관광 콘텐츠를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.TOURISM_CONTENT_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 장소를 공유할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun shareTourismContent(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "장소를 공유할 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "공유할 TourismContent ID", required = true)
        request: ShareTourismContentRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 만날 위치 공유", description = "호스트가 채팅방에 등록한 집합 위치 좌표와 상세 장소를 공유합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅 만날 위치 공유 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "채팅방에 공유할 집합 좌표가 등록되지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_MEETING_LOCATION_NOT_SET)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 위치를 공유할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun shareLocation(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "호스트 등록 집합 위치를 공유할 채팅방 ID", example = "101")
        roomId: Long,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 투표 개최", description = "선택지는 2~5개이며 anonymous를 생략하면 익명 투표입니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅 투표 개최 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "투표 입력값이 유효하지 않거나 중복 선택지가 있음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.DUPLICATE_CHAT_POLL_OPTION)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 투표를 열 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun createPoll(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "투표를 열 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "투표 질문, 2~5개 선택지, 익명 여부", required = true)
        request: CreateChatPollRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 투표 참여 또는 선택 변경", description = "현재 참가자의 투표를 등록하거나 기존 선택지를 새 선택지로 변경합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅 투표 참여 또는 선택 변경 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "투표 메시지 또는 해당 투표의 선택지를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "투표 메시지 없음", value = ChatRoomSwaggerExamples.POLL_NOT_FOUND),
                            ExampleObject(name = "투표 선택지 없음", value = ChatRoomSwaggerExamples.POLL_OPTION_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 투표할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun votePoll(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "투표가 있는 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "POLL 타입 투표 메시지 ID", example = "501")
        messageId: Long,
        @Parameter(description = "선택할 해당 투표의 선택지 ID", example = "1001")
        optionId: Long,
    ): ChatMessageResponse

    @Operation(summary = "채팅 투표 참여 취소", description = "현재 참가자가 해당 투표에서 선택한 항목을 취소합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅 투표 참여 취소 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "투표 메시지를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.POLL_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 투표를 취소할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun cancelPollVote(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "투표가 있는 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "POLL 타입 투표 메시지 ID", example = "501")
        messageId: Long,
    ): ChatMessageResponse

    @Operation(summary = "채팅 정산 메모 공유", description = "송금 기능 없이 정산 내용을 메모 카드로 공유합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "채팅 정산 메모 공유 성공",
                content = [Content(schema = Schema(implementation = ChatMessageResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "종료된 방에서는 정산 메모를 공유할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_DISABLED)],
                    ),
                ],
            ),
        ],
    )
    fun shareSettlementMemo(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "정산 메모를 공유할 채팅방 ID", example = "101")
        roomId: Long,
        @RequestBody(description = "송금 기능 없는 정산 메모 본문", required = true)
        request: CreateSettlementMemoRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 메시지 커서 조회", description = "beforeMessageId보다 오래된 메시지를 조회하며 응답 메시지는 오래된 순서로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅 메시지 목록 조회 성공",
                content = [Content(schema = Schema(implementation = ChatMessagePageResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "인증·채팅방 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
        ],
    )
    fun getMessages(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "메시지를 조회할 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "이 ID보다 오래된 메시지부터 조회하는 커서. 첫 페이지는 생략합니다.", example = "501")
        beforeMessageId: Long?,
        @Parameter(description = "반환할 메시지 수. 1~100으로 보정되며 기본값은 50입니다.", example = "50")
        limit: Int,
    ): ChatMessagePageResponse

    @Operation(summary = "현재 여행 로드맵", description = "확정된 여행 당일의 전체 장소 진행 상태와 현재·다음 일정을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "현재 여행 로드맵 조회 성공",
                content = [Content(schema = Schema(implementation = CurrentTravelRoadmapResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "인증·채팅방·여행 확정 또는 여행 당일 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
        ],
    )
    fun getCurrentRoadmap(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "현재 여행 로드맵을 조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): CurrentTravelRoadmapResponse
}

private object ChatRoomSwaggerExamples {
    const val INVALID_MAP_SEARCH_AREA = """{"code":40040,"errorMessage":"위도, 경도 또는 검색 반경이 유효하지 않습니다."}"""
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val INVALID_TRAVEL_COURSE_SCHEDULE = """{"code":40007,"errorMessage":"여행 일차마다 방문지를 최소 2개 편성해야 합니다."}"""
    const val INVALID_TRIP_SCHEDULE = """{"code":40008,"errorMessage":"당일치기는 종료 날짜 없이 시간을, 1박 이상은 종료 날짜만 입력해야 합니다."}"""
    const val INVALID_CHAT_ROOM_AGE_RESTRICTION = """{"code":40009,"errorMessage":"최소 나이는 최대 나이보다 작거나 같아야 합니다."}"""
    const val INVALID_MINIMUM_PARTICIPANTS = """{"code":40034,"errorMessage":"최소 출발 인원은 3명 이상이며 최대 참가 인원 이하여야 합니다."}"""
    const val PAST_CHAT_ROOM_START_DATE = """{"code":40035,"errorMessage":"여행 시작일은 오늘 또는 미래 날짜여야 합니다."}"""
    const val INVALID_RECRUITMENT_DEADLINE = """{"code":40036,"errorMessage":"모집 마감일은 여행 시작일 이하여야 합니다."}"""
    const val PAST_RECRUITMENT_DEADLINE_DATE = """{"code":40038,"errorMessage":"모집 마감일은 오늘 또는 미래 날짜여야 합니다."}"""
    const val CHAT_ROOM_THUMBNAIL_REQUIRED = """{"code":40041,"errorMessage":"채팅방 썸네일 이미지는 필수입니다."}"""
    const val INVALID_CHAT_ROOM_THUMBNAIL = """{"code":40042,"errorMessage":"채팅방 썸네일은 비어 있지 않은 20MB 이하 이미지 파일이어야 합니다."}"""
    const val CHAT_JOIN_APPLICATION_MESSAGE_REQUIRED = """{"code":40010,"errorMessage":"수동 승인 모임은 호스트에게 전할 말을 입력해야 합니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val FORBIDDEN = """{"code":40300,"errorMessage":"접근 권한이 없습니다."}"""
    const val CHAT_ROOM_JOIN_CONDITION_NOT_MET = """{"code":40302,"errorMessage":"모임의 성별 또는 나이 조건을 충족하지 않습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val TRAVEL_COURSE_NOT_FOUND = """{"code":40403,"errorMessage":"공개된 여행 코스를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
    const val CHAT_ROOM_MEMBER_NOT_FOUND = """{"code":40406,"errorMessage":"채팅방 멤버를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NO_MESSAGES = """{"code":40407,"errorMessage":"채팅방에 메시지가 없습니다."}"""
    const val CHAT_JOIN_APPLICATION_NOT_FOUND = """{"code":40404,"errorMessage":"참가 신청을 찾을 수 없습니다."}"""
    const val CHAT_ROOM_ALREADY_JOINED = """{"code":40906,"errorMessage":"이미 참가했거나 대기 중인 채팅방입니다."}"""
    const val CHAT_ROOM_CLOSED = """{"code":40905,"errorMessage":"모집이 종료된 채팅방입니다."}"""
    const val CHAT_ROOM_NOT_JOINED = """{"code":40907,"errorMessage":"참가하거나 대기 중인 채팅방이 아닙니다."}"""
    const val INVALID_CHAT_ROOM_STATUS = """{"code":40910,"errorMessage":"변경할 수 없는 여행 상태입니다."}"""
    const val CHAT_DISABLED = """{"code":40911,"errorMessage":"종료된 방에서는 채팅할 수 없습니다."}"""
    const val CHAT_ROOM_NOT_PARTICIPANT = """{"code":40301,"errorMessage":"사용자가 채팅방에 참여하고 있지 않습니다."}"""
    const val RESOURCE_NOT_FOUND = """{"code":40402,"errorMessage":"요청한 리소스를 찾을 수 없습니다."}"""
    const val POLL_NOT_FOUND = """{"code":40414,"errorMessage":"해당 채팅방의 투표 메시지를 찾을 수 없습니다."}"""
    const val POLL_OPTION_NOT_FOUND = """{"code":40415,"errorMessage":"해당 투표의 선택지를 찾을 수 없습니다."}"""
    const val INVALID_MEETING_INFORMATION = """{"code":40019,"errorMessage":"집합 좌표는 위도와 경도를 함께 입력하고 집합일은 여행 시작일 이하여야 합니다."}"""
    const val KICK_REASON_BLANK = """{"code":40020,"errorMessage":"강퇴 사유는 공백일 수 없습니다."}"""
    const val NOTICE_CONTENT_BLANK = """{"code":40021,"errorMessage":"공지 내용은 공백일 수 없습니다."}"""
    const val MENTIONED_USER_NOT_PARTICIPANT = """{"code":40022,"errorMessage":"멘션한 사용자 중 채팅방 참여자가 아닌 사용자가 있습니다."}"""
    const val INVALID_CHAT_IMAGE = """{"code":40023,"errorMessage":"채팅 이미지는 비어 있지 않은 20MB 이하 이미지 파일만 공유할 수 있습니다."}"""
    const val CHAT_ROOM_MEETING_LOCATION_NOT_SET = """{"code":40024,"errorMessage":"호스트가 채팅방 집합 위치 좌표를 등록하지 않았습니다."}"""
    const val DUPLICATE_CHAT_POLL_OPTION = """{"code":40025,"errorMessage":"투표 선택지는 중복될 수 없습니다."}"""
    const val CHAT_ROOM_HOST_REQUIRED = """{"code":40307,"errorMessage":"채팅방 호스트만 이 작업을 할 수 있습니다."}"""
    const val CHAT_REPLY_MESSAGE_NOT_FOUND = """{"code":40416,"errorMessage":"답글을 달 원본 채팅 메시지를 찾을 수 없습니다."}"""
    const val TOURISM_CONTENT_NOT_FOUND = """{"code":40408,"errorMessage":"관광 콘텐츠를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOTICE_NOT_FOUND = """{"code":40411,"errorMessage":"채팅방 공지를 찾을 수 없습니다."}"""
    const val TRAVEL_COURSE_TAG_NOT_FOUND = """{"code":40412,"errorMessage":"여행 코스 태그를 찾을 수 없습니다."}"""
    const val INVALID_TRAVEL_COURSE_SELECTION = """{"code":40909,"errorMessage":"공개 코스 하나 또는 직접 구성한 코스 중 하나만 선택해야 합니다."}"""
    const val MEETING_INFO_NOT_EDITABLE = """{"code":40913,"errorMessage":"여행 확정 전까지만 집합 정보를 수정할 수 있습니다."}"""
}
