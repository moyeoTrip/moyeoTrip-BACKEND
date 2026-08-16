package kr.hanchae.moyeotrip.controller.chat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateChatRoomStatusRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateMeetingInfoRequest
import kr.hanchae.moyeotrip.controller.chat.response.ApproveJoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessagePageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatMessageResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomDetailResponse
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomNoticeResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyChatRoomSummaryResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyWaitingChatRoomResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "채팅방", description = "여행 채팅방, 참가자, 대기열 및 메시지 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/api/v1/chat-rooms")
class ChatRoomController(
    private val chatRoomService: ChatRoomService,
) {
    @Operation(summary = "채팅방 생성", description = "생성한 사용자가 호스트이자 첫 참가자가 됩니다.")
    @PostMapping
    fun createRoom(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.createRoom(userId, request))

    @Operation(summary = "내 채팅방 목록", description = "안 읽은 메시지 수와 최근 메시지 정보를 함께 반환합니다.")
    @GetMapping("/my")
    fun getMyRooms(
        @LoginUserId userId: Long,
    ): List<MyChatRoomSummaryResponse> = chatRoomService.getMyRooms(userId)

    @Operation(summary = "내 참가 대기 채팅방 목록", description = "승인 대기 여부와 승인 후 대기열 순번을 반환합니다.")
    @GetMapping("/my-waiting")
    fun getMyWaitingRooms(
        @LoginUserId userId: Long,
    ): List<MyWaitingChatRoomResponse> = chatRoomService.getMyWaitingRooms(userId)

    @Operation(summary = "채팅방 상세 조회")
    @GetMapping("/{roomId}")
    fun getRoom(
        @PathVariable roomId: Long,
    ): ChatRoomDetailResponse = chatRoomService.getRoom(roomId)

    @Operation(summary = "집합 정보 수정", description = "여행 확정 전까지 채팅방 호스트가 집합 좌표, 상세 안내와 시간을 수정합니다.")
    @PutMapping("/{roomId}/meeting-info")
    fun updateMeetingInfo(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateMeetingInfoRequest,
    ): ResponseEntity<Void> {
        chatRoomService.updateMeetingInfo(userId, roomId, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "채팅방 참가 신청", description = "소개를 작성해 호스트의 승인을 기다립니다.")
    @PostMapping("/{roomId}/applications")
    fun applyToJoin(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: JoinChatRoomRequest,
    ): JoinChatRoomResponse = chatRoomService.applyToJoin(userId, roomId, request)

    @Operation(summary = "승인 대기 신청 목록", description = "호스트에게만 신청자의 프로필과 소개를 제공합니다.")
    @GetMapping("/{roomId}/applications")
    fun getApplications(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): List<JoinApplicationResponse> = chatRoomService.getPendingApplications(userId, roomId)

    @Operation(summary = "참가 신청 승인", description = "정원 내면 참가, 정원이 찼으면 승인된 대기열로 이동합니다.")
    @PostMapping("/{roomId}/applications/{applicationId}/approve")
    fun approveApplication(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable applicationId: Long,
    ): ApproveJoinApplicationResponse = chatRoomService.approveApplication(userId, roomId, applicationId)

    @Operation(summary = "참가 신청 거절")
    @DeleteMapping("/{roomId}/applications/{applicationId}")
    fun rejectApplication(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable applicationId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.rejectApplication(userId, roomId, applicationId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "채팅방 나가기 또는 대기 취소", description = "참가자가 나가면 대기열 1순위가 자동 참가합니다.")
    @DeleteMapping("/{roomId}/members/me")
    fun leaveRoom(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): LeaveChatRoomResponse = chatRoomService.leaveRoom(userId, roomId)

    @Operation(summary = "멤버 강퇴", description = "호스트가 참가 멤버를 방에서 제외하고 빈자리에 승인된 대기자를 승격합니다.")
    @DeleteMapping("/{roomId}/members/{memberId}")
    fun kickMember(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.kickMember(userId, roomId, memberId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "여행 상태 변경", description = "호스트가 모집 중인 여행을 확정 또는 불발 처리합니다.")
    @PostMapping("/{roomId}/status")
    fun changeStatus(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestBody request: UpdateChatRoomStatusRequest,
    ): ResponseEntity<Void> {
        chatRoomService.changeStatus(userId, roomId, request.status)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "채팅방 공지 등록")
    @PostMapping("/{roomId}/notices")
    fun createNotice(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateChatRoomNoticeRequest,
    ): ResponseEntity<Void> {
        chatRoomService.createNotice(userId, roomId, request.notice)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Operation(summary = "채팅방 공지 변경·삭제", description = "공지 내용이 null이면 noticeId에 해당하는 공지를 삭제합니다.")
    @PutMapping("/{roomId}/notices/{noticeId}")
    fun updateNotice(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable noticeId: Long,
        @Valid @RequestBody request: UpdateChatRoomNoticeRequest,
    ): ResponseEntity<Void> {
        chatRoomService.updateNotice(userId, roomId, noticeId, request.notice)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "채팅방 공지 이력", description = "최신 공지부터 과거 공지와 공지 해제 기록을 반환합니다.")
    @GetMapping("/{roomId}/notices")
    fun getNoticeHistory(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): List<ChatRoomNoticeResponse> = chatRoomService.getNoticeHistory(userId, roomId)

    @Operation(summary = "채팅 메시지 전송", description = "현재 참가자만 메시지를 보낼 수 있습니다.")
    @PostMapping("/{roomId}/messages")
    fun sendMessage(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: SendChatMessageRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.sendMessage(userId, roomId, request))

    @Operation(summary = "채팅 메시지 커서 조회", description = "beforeMessageId보다 오래된 메시지를 조회하며 응답 메시지는 오래된 순서로 반환합니다.")
    @GetMapping("/{roomId}/messages")
    fun getMessages(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestParam(required = false) beforeMessageId: Long?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ChatMessagePageResponse = chatRoomService.getMessages(userId, roomId, beforeMessageId, limit)
}
