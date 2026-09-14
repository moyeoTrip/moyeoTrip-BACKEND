package kr.hanchae.moyeotrip.controller.chat

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatPollRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomReportRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateSettlementMemoRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateUserReportRequest
import kr.hanchae.moyeotrip.controller.chat.request.JoinChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.request.KickChatRoomMemberRequest
import kr.hanchae.moyeotrip.controller.chat.request.MyChatRoomFilter
import kr.hanchae.moyeotrip.controller.chat.request.SendChatMessageRequest
import kr.hanchae.moyeotrip.controller.chat.request.ShareTourismContentRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.UpdateChatRoomRequest
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
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomReportReasonResponse
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomNoticeResponse
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.CurrentTravelRoadmapResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.JoinChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.LeaveChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.MapChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyChatRoomSummaryResponse
import kr.hanchae.moyeotrip.controller.chat.response.MyWaitingChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.RejectedJoinApplicationResponse
import kr.hanchae.moyeotrip.controller.chat.response.SearchChatRoomResponse
import kr.hanchae.moyeotrip.controller.chat.response.UserReportReasonResponse
import kr.hanchae.moyeotrip.controller.chat.response.WaitlistedJoinApplicationResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.service.search.PopularSearchKeywordService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/chat-rooms")
class ChatRoomController(
    private val chatRoomService: ChatRoomService,
    private val popularSearchKeywordService: PopularSearchKeywordService,
) : ChatRoomAPISpec {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun createRoom(
        @LoginUserId userId: Long,
        @Valid @RequestPart("request") request: CreateChatRoomRequest,
        // BE-07: 선택값이다. 생략하면 서버가 코스 대표 이미지(없으면 null)로 채운다.
        @RequestPart("thumbnail", required = false) thumbnail: MultipartFile?,
    ): ResponseEntity<CreateChatRoomResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.createRoom(userId, request, thumbnail))

    // BE-12: 사유 목록은 피드와 같은 형태로 열어 클라이언트가 하드코딩하지 않게 한다.
    // 리터럴 경로라 GET /{roomId} 보다 먼저 매칭된다(피드의 /report-reasons 와 같은 구조).
    @GetMapping("/report-reasons")
    override fun getRoomReportReasons(): List<ChatRoomReportReasonResponse> = chatRoomService.getRoomReportReasons()

    @GetMapping("/member-report-reasons")
    override fun getMemberReportReasons(): List<UserReportReasonResponse> = chatRoomService.getMemberReportReasons()

    @PostMapping("/{roomId}/reports")
    override fun reportRoom(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateChatRoomReportRequest,
    ): ResponseEntity<Void> {
        chatRoomService.reportRoom(userId, roomId, request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{roomId}/members/{memberId}/reports")
    override fun reportMember(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: CreateUserReportRequest,
    ): ResponseEntity<Void> {
        chatRoomService.reportMember(userId, roomId, memberId, request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/my")
    override fun getMyRooms(
        @LoginUserId userId: Long,
        @RequestParam(defaultValue = "ALL") filter: MyChatRoomFilter,
    ): List<MyChatRoomSummaryResponse> = chatRoomService.getMyRooms(userId, filter)

    @GetMapping("/my-waiting")
    override fun getMyWaitingRooms(
        @LoginUserId userId: Long,
    ): List<MyWaitingChatRoomResponse> = chatRoomService.getMyWaitingRooms(userId)

    @GetMapping("/search")
    override fun searchRooms(
        @LoginUserId userId: Long,
        @RequestParam(required = false) keyword: String?,
        // BE-02: 지도와 같은 태그 필터. 생략하면 지금까지와 동일하다.
        @RequestParam(required = false) tagId: Long?,
        // 무한 스크롤 커서 — 직전 페이지 마지막 roomId. 생략하면 첫 페이지다.
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<SearchChatRoomResponse> =
        chatRoomService.searchRooms(userId, keyword, tagId, cursor, limit).also {
            popularSearchKeywordService.record(keyword)
        }

    @GetMapping("/map")
    override fun getMapRooms(
        @LoginUserId userId: Long,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam radiusKm: Double,
        // BE-02: 목록과 같은 필터를 받아 같은 결과 집합을 준다.
        @RequestParam(required = false) tagId: Long?,
    ): List<MapChatRoomResponse> = chatRoomService.getMapRooms(userId, latitude, longitude, radiusKm, tagId)

    @GetMapping("/{roomId}")
    override fun getRoom(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomDetailResponse = chatRoomService.getRoom(userId, roomId)

    @PatchMapping("/{roomId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun updateRoom(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestPart("request") request: UpdateChatRoomRequest,
        @RequestPart("thumbnail", required = false) thumbnail: MultipartFile?,
    ): ResponseEntity<Void> {
        chatRoomService.updateRoom(userId, roomId, request, thumbnail)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{roomId}/favorite")
    override fun toggleRoomFavorite(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomFavoriteResponse = chatRoomService.toggleRoomFavorite(userId, roomId)

    @GetMapping("/my/favorites")
    override fun getFavoriteRooms(
        @LoginUserId userId: Long,
    ): List<SearchChatRoomResponse> = chatRoomService.getFavoriteRooms(userId)

    @PutMapping("/{roomId}/meeting-info")
    override fun updateMeetingInfo(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateMeetingInfoRequest,
    ): ResponseEntity<Void> {
        chatRoomService.updateMeetingInfo(userId, roomId, request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{roomId}/applications")
    override fun applyToJoin(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: JoinChatRoomRequest,
    ): JoinChatRoomResponse = chatRoomService.applyToJoin(userId, roomId, request)

    @DeleteMapping("/{roomId}/applications/me")
    override fun cancelJoinApplication(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.cancelJoinApplication(userId, roomId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{roomId}/applications")
    override fun getApplications(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): List<JoinApplicationResponse> = chatRoomService.getPendingApplications(userId, roomId)

    // 대기열 조회 — 호스트가 승인해도 정원이 차 있으면 대기열로 가는데, 그 사람이 어디에도 안 보였다.
    @GetMapping("/{roomId}/applications/waitlist")
    override fun getWaitlistedApplications(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): List<WaitlistedJoinApplicationResponse> = chatRoomService.getWaitlistedApplications(userId, roomId)

    // BE-17: 호스트의 「거절 기록」 조회
    @GetMapping("/{roomId}/applications/rejected")
    override fun getRejectedApplications(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): List<RejectedJoinApplicationResponse> = chatRoomService.getRejectedApplications(userId, roomId)

    @PostMapping("/{roomId}/applications/{applicationId}/approve")
    override fun approveApplication(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable applicationId: Long,
    ): ApproveJoinApplicationResponse = chatRoomService.approveApplication(userId, roomId, applicationId)

    @DeleteMapping("/{roomId}/applications/{applicationId}")
    override fun rejectApplication(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable applicationId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.rejectApplication(userId, roomId, applicationId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{roomId}/members/me")
    override fun leaveRoom(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): LeaveChatRoomResponse = chatRoomService.leaveRoom(userId, roomId)

    @GetMapping("/{roomId}/members")
    override fun getMembers(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomMemberListResponse = chatRoomService.getMembers(userId, roomId)

    @GetMapping("/my-kick-histories")
    override fun getMyKickHistories(
        @LoginUserId userId: Long,
    ): List<ChatRoomKickHistoryResponse> = chatRoomService.getMyKickHistories(userId)

    @DeleteMapping("/{roomId}/members/{memberId}")
    override fun kickMember(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: KickChatRoomMemberRequest,
    ): ResponseEntity<Void> {
        chatRoomService.kickMember(userId, roomId, memberId, request.reason)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{roomId}/status")
    override fun changeStatus(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestBody request: UpdateChatRoomStatusRequest,
    ): ResponseEntity<Void> {
        chatRoomService.changeStatus(userId, roomId, request.status)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{roomId}/notices")
    override fun createNotice(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateChatRoomNoticeRequest,
    ): ResponseEntity<CreateChatRoomNoticeResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CreateChatRoomNoticeResponse(chatRoomService.createNotice(userId, roomId, request.notice, request.pinned)))

    @PutMapping("/{roomId}/notices/{noticeId}")
    override fun updateNotice(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable noticeId: Long,
        @Valid @RequestBody request: UpdateChatRoomNoticeRequest,
    ): ResponseEntity<Void> {
        chatRoomService.updateNotice(userId, roomId, noticeId, request.notice, request.pinned)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{roomId}/notices/{noticeId}")
    override fun deleteNotice(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable noticeId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.deleteNotice(userId, roomId, noticeId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{roomId}/notices")
    override fun getNoticeHistory(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomNoticeHistoryResponse = chatRoomService.getNoticeHistory(userId, roomId)

    @PostMapping("/{roomId}/messages")
    override fun sendMessage(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: SendChatMessageRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.sendMessage(userId, roomId, request))

    @DeleteMapping("/{roomId}/messages/{messageId}")
    override fun deleteMessage(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable messageId: Long,
    ): ResponseEntity<Void> {
        chatRoomService.deleteMessage(userId, roomId, messageId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{roomId}/messages/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun shareImage(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestPart("image") image: MultipartFile,
        @RequestPart("caption", required = false) caption: String?,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareImage(userId, roomId, image, caption))

    @PostMapping("/{roomId}/messages/tourism-contents")
    override fun shareTourismContent(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: ShareTourismContentRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareTourismContent(userId, roomId, request))

    @PostMapping("/{roomId}/messages/locations")
    override fun shareLocation(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ResponseEntity<ChatMessageResponse> = ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareLocation(userId, roomId))

    @PostMapping("/{roomId}/messages/polls")
    override fun createPoll(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateChatPollRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.createPoll(userId, roomId, request))

    @PutMapping("/{roomId}/messages/{messageId}/poll-options/{optionId}/vote")
    override fun votePoll(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable messageId: Long,
        @PathVariable optionId: Long,
    ): ChatMessageResponse = chatRoomService.votePoll(userId, roomId, messageId, optionId)

    @DeleteMapping("/{roomId}/messages/{messageId}/vote")
    override fun cancelPollVote(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @PathVariable messageId: Long,
    ): ChatMessageResponse = chatRoomService.cancelPollVote(userId, roomId, messageId)

    @PostMapping("/{roomId}/messages/settlement-memos")
    override fun shareSettlementMemo(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: CreateSettlementMemoRequest,
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatRoomService.shareSettlementMemo(userId, roomId, request))

    @GetMapping("/{roomId}/messages")
    override fun getMessages(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestParam(required = false) beforeMessageId: Long?,
        @RequestParam(defaultValue = "50") limit: Int,
        // BE-27②: 폴링 클라이언트가 새 메시지만 받아 가는 정방향 커서
        @RequestParam(required = false) afterMessageId: Long?,
    ): ChatMessagePageResponse = chatRoomService.getMessages(userId, roomId, beforeMessageId, limit, afterMessageId)

    @GetMapping("/{roomId}/roadmap/current")
    override fun getCurrentRoadmap(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): CurrentTravelRoadmapResponse = chatRoomService.getCurrentRoadmap(userId, roomId)
}
