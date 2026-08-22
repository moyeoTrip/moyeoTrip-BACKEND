package kr.hanchae.moyeotrip.controller.chat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import kr.hanchae.moyeotrip.controller.chat.response.CurrentTravelRoadmapResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinEligibilityResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveChatRoomResponse
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
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            ApiResponse(
                responseCode = "400",
                description = "요청 본문·일정·나이 또는 커스텀 코스 구성 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 본문 또는 enum 값 오류", value = ChatRoomSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "당일·숙박 일정 입력 오류", value = ChatRoomSwaggerExamples.INVALID_TRIP_SCHEDULE),
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
            description = "채팅방 생성 정보와 선택 썸네일을 multipart/form-data로 전송합니다. request 파트는 application/json입니다.",
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
        @Parameter(description = "선택 썸네일 이미지 파일")
        thumbnail: MultipartFile?,
    ): ResponseEntity<Unit>

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
        ],
    )
    fun getMyRooms(
        @Parameter(hidden = true) userId: Long,
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

    @Operation(summary = "모임 검색", description = "채팅방명 검색을 지원하며 차단 관계인 사용자가 호스트 또는 참가자인 모임은 제외합니다.")
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
        keyword: String?,
        limit: Int,
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
    fun toggleRoomFavorite(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ChatRoomFavoriteResponse

    @Operation(summary = "집합 정보 수정", description = "여행 확정 전까지 채팅방 호스트가 집합 좌표, 상세 안내와 시간을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "집합 정보 수정 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "409",
                description = "인증·권한·입력 검증 또는 수정 가능 상태 확인 실패",
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
        roomId: Long,
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
                responseCode = "409",
                description = "인증·입력 검증·참가 조건 또는 모집 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_ALREADY_JOINED)],
                    ),
                ],
            ),
        ],
    )
    fun applyToJoin(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: JoinChatRoomRequest,
    ): JoinChatRoomResponse

    @Operation(summary = "채팅방 참가 신청 취소", description = "호스트 승인 대기 또는 승인 후 대기열에 있는 본인의 신청을 취소합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "채팅방 참가 신청 취소 성공. 응답 본문 없음"),
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
        roomId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 참가 신청 가능 여부", description = "모집 상태, 기존 참가·신청 여부, 성별과 만 나이 조건을 확인합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방 참가 신청 가능 여부 조회 성공",
                content = [Content(schema = Schema(implementation = JoinEligibilityResponse::class))],
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
    fun getJoinEligibility(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): JoinEligibilityResponse

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
                description = "인증·권한·채팅방 또는 승인 대기 신청 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_JOIN_APPLICATION_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun approveApplication(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        applicationId: Long,
    ): ApproveJoinApplicationResponse

    @Operation(summary = "참가 신청 거절", description = "호스트가 승인 대기 중인 참가 신청을 거절하고 신청 이력을 제거합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "참가 신청 거절 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "인증·권한·채팅방 또는 승인 대기 신청 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_JOIN_APPLICATION_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun rejectApplication(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
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

    @Operation(summary = "멤버 강퇴", description = "필수 사유를 비공개 이력으로 저장하고 빈자리에 승인된 대기자를 승격합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "멤버 강퇴 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "403",
                description = "인증·권한·입력 검증·채팅방 또는 멤버 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
        ],
    )
    fun kickMember(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        memberId: Long,
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
        roomId: Long,
        request: UpdateChatRoomStatusRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 등록", description = "호스트가 공지를 등록하며 pinned를 true로 지정하면 상단 고정 공지로 표시합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "채팅방 공지 등록 성공"),
            ApiResponse(
                responseCode = "403",
                description = "인증·권한·입력 검증 또는 채팅방 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
        ],
    )
    fun createNotice(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: CreateChatRoomNoticeRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 변경·삭제", description = "내용과 고정 상태를 변경합니다. notice와 pinned가 모두 null이면 공지를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "채팅방 공지 변경 또는 삭제 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "인증·권한·입력 검증·채팅방 또는 공지 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOTICE_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun updateNotice(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        noticeId: Long,
        request: UpdateChatRoomNoticeRequest,
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
                description = "인증·입력 검증·채팅방 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
        ],
    )
    fun sendMessage(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
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
                description = "인증·파일 검증·채팅방 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
        ],
    )
    fun shareImage(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        image: MultipartFile,
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
                description = "인증·입력 검증·여행지·채팅방 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.TOURISM_CONTENT_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun shareTourismContent(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
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
                description = "인증·채팅방·집합 위치 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
        ],
    )
    fun shareLocation(
        @Parameter(hidden = true) userId: Long,
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
                description = "인증·입력 검증·채팅방 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
        ],
    )
    fun createPoll(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
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
                description = "인증·채팅방·투표·선택지 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.RESOURCE_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun votePoll(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        messageId: Long,
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
                description = "인증·채팅방·투표 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.RESOURCE_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun cancelPollVote(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
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
                description = "인증·입력 검증·채팅방 또는 참가 상태 확인 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = ChatRoomSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
        ],
    )
    fun shareSettlementMemo(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
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
        roomId: Long,
        beforeMessageId: Long?,
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
        roomId: Long,
    ): CurrentTravelRoadmapResponse
}

private object ChatRoomSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val INVALID_TRAVEL_COURSE_SCHEDULE = """{"code":40007,"errorMessage":"여행 일차마다 방문지를 최소 2개 편성해야 합니다."}"""
    const val INVALID_TRIP_SCHEDULE = """{"code":40008,"errorMessage":"당일치기는 종료 날짜 없이 시간을, 1박 이상은 종료 날짜만 입력해야 합니다."}"""
    const val INVALID_CHAT_ROOM_AGE_RESTRICTION = """{"code":40009,"errorMessage":"최소 나이는 최대 나이보다 작거나 같아야 합니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val FORBIDDEN = """{"code":40300,"errorMessage":"접근 권한이 없습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val TRAVEL_COURSE_NOT_FOUND = """{"code":40403,"errorMessage":"공개된 여행 코스를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
    const val CHAT_JOIN_APPLICATION_NOT_FOUND = """{"code":40404,"errorMessage":"참가 신청을 찾을 수 없습니다."}"""
    const val CHAT_ROOM_ALREADY_JOINED = """{"code":40906,"errorMessage":"이미 참가했거나 대기 중인 채팅방입니다."}"""
    const val CHAT_ROOM_NOT_JOINED = """{"code":40907,"errorMessage":"참가하거나 대기 중인 채팅방이 아닙니다."}"""
    const val INVALID_CHAT_ROOM_STATUS = """{"code":40910,"errorMessage":"변경할 수 없는 여행 상태입니다."}"""
    const val CHAT_ROOM_NOT_PARTICIPANT = """{"code":40301,"errorMessage":"사용자가 채팅방에 참여하고 있지 않습니다."}"""
    const val RESOURCE_NOT_FOUND = """{"code":40402,"errorMessage":"요청한 리소스를 찾을 수 없습니다."}"""
    const val TOURISM_CONTENT_NOT_FOUND = """{"code":40408,"errorMessage":"관광 콘텐츠를 찾을 수 없습니다."}"""
    const val CHAT_ROOM_NOTICE_NOT_FOUND = """{"code":40411,"errorMessage":"채팅방 공지를 찾을 수 없습니다."}"""
    const val TRAVEL_COURSE_TAG_NOT_FOUND = """{"code":40412,"errorMessage":"여행 코스 태그를 찾을 수 없습니다."}"""
    const val INVALID_TRAVEL_COURSE_SELECTION = """{"code":40909,"errorMessage":"공개 코스 하나 또는 직접 구성한 코스 중 하나만 선택해야 합니다."}"""
    const val MEETING_INFO_NOT_EDITABLE = """{"code":40913,"errorMessage":"여행 확정 전까지만 집합 정보를 수정할 수 있습니다."}"""
}
