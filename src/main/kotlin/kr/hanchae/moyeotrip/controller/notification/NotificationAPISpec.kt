package kr.hanchae.moyeotrip.controller.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomKickHistoryResponse
import kr.hanchae.moyeotrip.controller.notification.request.UpdateChatRoomNotificationSettingRequest
import kr.hanchae.moyeotrip.controller.notification.request.UpdateNotificationSettingRequest
import kr.hanchae.moyeotrip.controller.notification.response.ChatRoomNotificationSettingResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationPageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationSettingResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse
import org.springframework.http.ResponseEntity

@Tag(name = "알림", description = "모임 및 채팅 알림 API")
interface NotificationAPISpec {
    @Operation(summary = "내 알림 목록", description = "lastId 이전의 알림을 최신순으로 조회합니다. unreadOnly가 true면 읽지 않은 알림만 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 알림 목록 조회 성공",
                content = [Content(schema = Schema(implementation = NotificationPageResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun getNotifications(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "이 ID보다 오래된 알림부터 조회하는 커서. 첫 페이지는 생략합니다.", example = "100")
        lastId: Long?,
        @Parameter(description = "반환할 알림 수. 기본값은 20입니다.", example = "20")
        size: Int,
        @Parameter(description = "true면 읽지 않은 알림만 반환합니다.", example = "false")
        unreadOnly: Boolean,
    ): NotificationPageResponse

    @Operation(summary = "강퇴 알림 상세 조회", description = "로그인 사용자에게 발송된 강퇴 알림의 채팅방 제목, 강퇴 사유와 강퇴 시각을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "강퇴 알림 상세 조회 성공",
                content = [Content(schema = Schema(implementation = ChatRoomKickHistoryResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "강퇴 알림이 아닌 알림으로 강퇴 이력을 조회함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "본인 소유의 강퇴 알림 또는 강퇴 이력을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "강퇴 알림 없음", value = NotificationSwaggerExamples.NOTIFICATION_NOT_FOUND),
                            ExampleObject(name = "강퇴 이력 없음", value = NotificationSwaggerExamples.NOTIFICATION_NOT_FOUND),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getKickHistory(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "강퇴 알림 상세를 조회할 알림 ID", example = "100")
        notificationId: Long,
    ): ChatRoomKickHistoryResponse

    @Operation(summary = "알림 읽음 처리", description = "로그인 사용자 소유의 알림 한 건을 읽음 상태로 변경합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "알림 읽음 처리 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "404",
                description = "본인 소유의 알림을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.NOTIFICATION_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun markRead(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "읽음 처리할 내 알림 ID", example = "100")
        notificationId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "알림 모두 읽음 처리", description = "로그인 사용자의 읽지 않은 알림을 모두 읽음 상태로 변경합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "알림 모두 읽음 처리 성공. 응답 본문 없음"),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
        ],
    )
    fun markAllRead(
        @Parameter(hidden = true) userId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "방해 금지 설정 조회", description = "방해 금지 사용 여부와 적용 시간·요일을 반환합니다. 기본 알림 수신 설정은 내 프로필 조회에서 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "방해 금지 설정 조회 성공",
                content = [Content(schema = Schema(implementation = NotificationSettingResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getSetting(
        @Parameter(hidden = true) userId: Long,
    ): NotificationSettingResponse

    @Operation(summary = "내 알림 설정 변경", description = "기본 알림 수신 설정과 방해 금지 사용 여부·시간·요일을 한 번에 변경합니다. 응답에는 방해 금지 상세 설정을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 알림 설정 변경 성공",
                content = [Content(schema = Schema(implementation = NotificationSettingResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "방해 금지 시간·요일 또는 알림 설정 입력값이 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
        ],
    )
    fun updateSetting(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "채팅·소셜·마케팅 알림과 방해 금지 시간·요일 설정", required = true)
        request: UpdateNotificationSettingRequest,
    ): NotificationSettingResponse

    @Operation(summary = "채팅방별 알림 설정 조회", description = "별도 설정이 없으면 기본값은 켜짐입니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방별 알림 설정 조회 성공",
                content = [Content(schema = Schema(implementation = ChatRoomNotificationSettingResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방의 참가자가 아니어서 알림 설정을 조회할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
        ],
    )
    fun getChatRoomSetting(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "알림 설정을 조회할 참여 중 채팅방 ID", example = "101")
        roomId: Long,
    ): ChatRoomNotificationSettingResponse

    @Operation(summary = "채팅방별 알림 켜기·끄기", description = "지정한 채팅방의 알림 수신 여부를 저장합니다. 별도 설정이 없으면 기본값은 켜짐입니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "채팅방별 알림 설정 변경 성공",
                content = [Content(schema = Schema(implementation = ChatRoomNotificationSettingResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방의 참가자가 아니어서 알림 설정을 변경할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = NotificationSwaggerExamples.CHAT_ROOM_NOT_PARTICIPANT)],
                    ),
                ],
            ),
        ],
    )
    fun updateChatRoomSetting(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "알림을 켜거나 끌 참여 중 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "해당 채팅방 알림 수신 여부", required = true)
        request: UpdateChatRoomNotificationSettingRequest,
    ): ChatRoomNotificationSettingResponse
}

private object NotificationSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val CHAT_ROOM_NOT_PARTICIPANT = """{"code":40301,"errorMessage":"사용자가 채팅방에 참여하고 있지 않습니다."}"""
    const val NOTIFICATION_NOT_FOUND = """{"code":40410,"errorMessage":"알림을 찾을 수 없습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
}
