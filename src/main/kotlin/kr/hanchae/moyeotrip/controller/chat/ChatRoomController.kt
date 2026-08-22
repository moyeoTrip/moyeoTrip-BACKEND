package kr.hanchae.moyeotrip.controller.chat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "채팅방", description = "여행 채팅방, 참가자, 대기열 및 메시지 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/api/v1/chat-rooms")
class ChatRoomController(
    private val chatRoomService: ChatRoomService,
) {
    @Operation(summary = "채팅방 생성", description = "생성한 사용자가 호스트이자 첫 참가자가 됩니다.")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createRoom(
        @LoginUserId userId: Long,
        @Valid @RequestPart("request") request: CreateChatRoomRequest,
        @RequestPart("thumbnail", required = false) thumbnail: MultipartFile?,
    ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.createRoom(userId, request, thumbnail))

    @Operation(
        summary = "내 채팅방 목록",
        description = "모집중·확정·종료 상태로 필터링하며 인원, 마감 D-day, 안 읽은 수와 최근 메시지를 반환합니다.",
    )
    @GetMapping("/my")
    fun getMyRooms(
        @LoginUserId userId: Long,
        @RequestParam(defaultValue = "ALL") filter: MyChatRoomFilter,
    ): List<MyChatRoomSummaryResponse> = chatRoomService.getMyRooms(userId, filter)

    @Operation(
        summary = "내 신청중 채팅방 목록",
        description = "호스트 승인 대기(PENDING)와 승인 후 자리 대기(WAITLISTED) 신청만 반환합니다.",
    )
    @GetMapping("/my-waiting")
    fun getMyWaitingRooms(
        @LoginUserId userId: Long,
    ): List<MyWaitingChatRoomResponse> = chatRoomService.getMyWaitingRooms(userId)

    @Operation(summary = "모임 검색", description = "채팅방명 검색을 지원하며 차단 관계인 사용자가 호스트 또는 참가자인 모임은 제외합니다.")
    @GetMapping("/search")
    fun searchRooms(
        @LoginUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<SearchChatRoomResponse> = chatRoomService.searchRooms(userId, keyword, limit)

    @Operation(summary = "채팅방 상세 조회")
    @GetMapping("/{roomId}")
    fun getRoom(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomDetailResponse = chatRoomService.getRoom(userId, roomId)

    @Operation(summary = "채팅방 찜 상태 토글", description = "호출할 때마다 찜 상태를 반전하고 변경된 상태를 반환합니다.")
    @PostMapping("/{roomId}/favorite")
    fun toggleRoomFavorite(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomFavoriteResponse = chatRoomService.toggleRoomFavorite(userId, roomId)

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

    @Operation(summary = "채팅방 참가 신청 취소", description = "호스트 승인 대기 또는 승인 후 대기열에 있는 본인의 신청을 취소합니다.")
    @DeleteMapping("/{roomId}/applications/me")
    fun cancelJoinApplication(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.cancelJoinApplication(userId, roomId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "채팅방 참가 신청 가능 여부", description = "모집 상태, 기존 참가·신청 여부, 성별과 만 나이 조건을 확인합니다.")
    @GetMapping("/{roomId}/join-eligibility")
    fun getJoinEligibility(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): JoinEligibilityResponse = chatRoomService.getJoinEligibility(userId, roomId)

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

    @Operation(
        summary = "채팅방 동행자 목록",
        description = "현재·최대 인원, 승인된 대기 인원과 각 동행자의 프로필·닉네임·완료 여행 횟수를 반환합니다.",
    )
    @GetMapping("/{roomId}/members")
    fun getMembers(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomMemberListResponse = chatRoomService.getMembers(userId, roomId)

    @Operation(summary = "내 강퇴 이력", description = "로그인한 본인이 강퇴된 사유만 최신순으로 반환합니다.")
    @GetMapping("/my-kick-histories")
    fun getMyKickHistories(
        @LoginUserId userId: Long,
    ): List<ChatRoomKickHistoryResponse> = chatRoomService.getMyKickHistories(userId)

    @Operation(summary = "멤버 강퇴", description = "필수 사유를 비공개 이력으로 저장하고 빈자리에 승인된 대기자를 승격합니다.")
    @DeleteMapping("/{roomId}/members/{memberId}")
    fun kickMember(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: KickChatRoomMemberRequest,
    ): ResponseEntity<Void> {
        chatRoomService.kickMember(userId, roomId, memberId, request.reason)
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
        chatRoomService.createNotice(userId, roomId, request.notice, request.pinned)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Operation(
        summary = "채팅방 공지 변경·삭제",
        description = "내용과 고정 상태를 변경합니다. notice와 pinned가 모두 null이면 공지를 삭제합니다.",
    )
    @PutMapping("/{roomId}/notices/{noticeId}")
    fun updateNotice(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable noticeId: Long,
        @Valid @RequestBody request: UpdateChatRoomNoticeRequest,
    ): ResponseEntity<Void> {
        chatRoomService.updateNotice(userId, roomId, noticeId, request.notice, request.pinned)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "채팅방 공지 이력", description = "고정 공지와 고정하지 않은 공지를 각각 생성일 내림차순으로 반환합니다.")
    @GetMapping("/{roomId}/notices")
    fun getNoticeHistory(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomNoticeHistoryResponse = chatRoomService.getNoticeHistory(userId, roomId)

    @Operation(summary = "채팅 메시지 전송", description = "현재 참가자만 메시지를 보낼 수 있습니다.")
    @PostMapping("/{roomId}/messages")
    fun sendMessage(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: SendChatMessageRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.sendMessage(userId, roomId, request))

    @Operation(summary = "채팅 사진 공유", description = "현재 참가자가 최대 20MB 이미지 한 장을 공유합니다.")
    @PostMapping("/{roomId}/messages/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun shareImage(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestPart("image") image: MultipartFile,
        @RequestPart("caption", required = false) caption: String?,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareImage(userId, roomId, image, caption))

    @Operation(summary = "채팅 관광 장소 공유")
    @PostMapping("/{roomId}/messages/tourism-contents")
    fun shareTourismContent(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: ShareTourismContentRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareTourismContent(userId, roomId, request))

    @Operation(summary = "채팅 만날 위치 공유", description = "호스트가 채팅방에 등록한 집합 위치 좌표와 상세 장소를 공유합니다.")
    @PostMapping("/{roomId}/messages/locations")
    fun shareLocation(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ResponseEntity<ChatMessageResponse> = ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareLocation(userId, roomId))

    @Operation(summary = "채팅 투표 개최", description = "선택지는 2~5개이며 anonymous를 생략하면 익명 투표입니다.")
    @PostMapping("/{roomId}/messages/polls")
    fun createPoll(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateChatPollRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.createPoll(userId, roomId, request))

    @Operation(summary = "채팅 투표 참여 또는 선택 변경")
    @PutMapping("/{roomId}/messages/{messageId}/poll-options/{optionId}/vote")
    fun votePoll(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable messageId: Long,
        @PathVariable optionId: Long,
    ): ChatMessageResponse = chatRoomService.votePoll(userId, roomId, messageId, optionId)

    @Operation(summary = "채팅 투표 참여 취소")
    @DeleteMapping("/{roomId}/messages/{messageId}/vote")
    fun cancelPollVote(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable messageId: Long,
    ): ChatMessageResponse = chatRoomService.cancelPollVote(userId, roomId, messageId)

    @Operation(summary = "채팅 정산 메모 공유", description = "송금 기능 없이 정산 내용을 메모 카드로 공유합니다.")
    @PostMapping("/{roomId}/messages/settlement-memos")
    fun shareSettlementMemo(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateSettlementMemoRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareSettlementMemo(userId, roomId, request))

    @Operation(summary = "채팅 메시지 커서 조회", description = "beforeMessageId보다 오래된 메시지를 조회하며 응답 메시지는 오래된 순서로 반환합니다.")
    @GetMapping("/{roomId}/messages")
    fun getMessages(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestParam(required = false) beforeMessageId: Long?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ChatMessagePageResponse = chatRoomService.getMessages(userId, roomId, beforeMessageId, limit)

    @Operation(summary = "현재 여행 로드맵", description = "확정된 여행 당일의 전체 장소 진행 상태와 현재·다음 일정을 반환합니다.")
    @GetMapping("/{roomId}/roadmap/current")
    fun getCurrentRoadmap(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): CurrentTravelRoadmapResponse = chatRoomService.getCurrentRoadmap(userId, roomId)
}
