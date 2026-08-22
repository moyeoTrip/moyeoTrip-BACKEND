package kr.hanchae.moyeotrip.controller.chat

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "채팅방", description = "여행 채팅방, 참가자, 대기열 및 메시지 API")
@SecurityRequirement(name = "Authorization")
interface ChatRoomAPISpec {
    @Operation(summary = "채팅방 생성", description = "생성한 사용자가 호스트이자 첫 참가자가 됩니다.")
    fun createRoom(
        @Parameter(hidden = true) userId: Long,
        request: CreateChatRoomRequest,
        thumbnail: MultipartFile?,
    ): ResponseEntity<Unit>

    @Operation(summary = "내 채팅방 목록", description = "모집중·확정·종료 상태로 필터링하며 인원, 마감 D-day, 안 읽은 수와 최근 메시지를 반환합니다.")
    fun getMyRooms(
        @Parameter(hidden = true) userId: Long,
        filter: MyChatRoomFilter,
    ): List<MyChatRoomSummaryResponse>

    @Operation(summary = "내 신청중 채팅방 목록", description = "호스트 승인 대기(PENDING)와 승인 후 자리 대기(WAITLISTED) 신청만 반환합니다.")
    fun getMyWaitingRooms(
        @Parameter(hidden = true) userId: Long,
    ): List<MyWaitingChatRoomResponse>

    @Operation(summary = "모임 검색", description = "채팅방명 검색을 지원하며 차단 관계인 사용자가 호스트 또는 참가자인 모임은 제외합니다.")
    fun searchRooms(
        @Parameter(hidden = true) userId: Long,
        keyword: String?,
        limit: Int,
    ): List<SearchChatRoomResponse>

    @Operation(summary = "채팅방 상세 조회", description = "모집·여행 정보, 호스트, 참가자와 최신 고정 공지를 반환합니다. 종료 후 2주가 지난 채팅방은 조회할 수 없습니다.")
    fun getRoom(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ChatRoomDetailResponse

    @Operation(summary = "채팅방 찜 상태 토글", description = "호출할 때마다 찜 상태를 반전하고 변경된 상태를 반환합니다.")
    fun toggleRoomFavorite(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ChatRoomFavoriteResponse

    @Operation(summary = "집합 정보 수정", description = "여행 확정 전까지 채팅방 호스트가 집합 좌표, 상세 안내와 시간을 수정합니다.")
    fun updateMeetingInfo(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: UpdateMeetingInfoRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 참가 신청", description = "소개를 작성해 호스트의 승인을 기다립니다.")
    fun applyToJoin(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: JoinChatRoomRequest,
    ): JoinChatRoomResponse

    @Operation(summary = "채팅방 참가 신청 취소", description = "호스트 승인 대기 또는 승인 후 대기열에 있는 본인의 신청을 취소합니다.")
    fun cancelJoinApplication(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 참가 신청 가능 여부", description = "모집 상태, 기존 참가·신청 여부, 성별과 만 나이 조건을 확인합니다.")
    fun getJoinEligibility(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): JoinEligibilityResponse

    @Operation(summary = "승인 대기 신청 목록", description = "호스트에게만 신청자의 프로필과 소개를 제공합니다.")
    fun getApplications(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): List<JoinApplicationResponse>

    @Operation(summary = "참가 신청 승인", description = "정원 내면 참가, 정원이 찼으면 승인된 대기열로 이동합니다.")
    fun approveApplication(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        applicationId: Long,
    ): ApproveJoinApplicationResponse

    @Operation(summary = "참가 신청 거절", description = "호스트가 승인 대기 중인 참가 신청을 거절하고 신청 이력을 제거합니다.")
    fun rejectApplication(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        applicationId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 나가기 또는 대기 취소", description = "참가자가 나가면 대기열 1순위가 자동 참가합니다.")
    fun leaveRoom(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): LeaveChatRoomResponse

    @Operation(summary = "채팅방 동행자 목록", description = "현재·최대 인원, 승인된 대기 인원과 각 동행자의 프로필·닉네임·완료 여행 횟수를 반환합니다.")
    fun getMembers(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ChatRoomMemberListResponse

    @Operation(summary = "내 강퇴 이력", description = "로그인한 본인이 강퇴된 사유만 최신순으로 반환합니다.")
    fun getMyKickHistories(
        @Parameter(hidden = true) userId: Long,
    ): List<ChatRoomKickHistoryResponse>

    @Operation(summary = "멤버 강퇴", description = "필수 사유를 비공개 이력으로 저장하고 빈자리에 승인된 대기자를 승격합니다.")
    fun kickMember(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        memberId: Long,
        request: KickChatRoomMemberRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "여행 상태 변경", description = "호스트가 모집 중인 여행을 확정 또는 불발 처리합니다.")
    fun changeStatus(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: UpdateChatRoomStatusRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 등록", description = "호스트가 공지를 등록하며 pinned를 true로 지정하면 상단 고정 공지로 표시합니다.")
    fun createNotice(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: CreateChatRoomNoticeRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 변경·삭제", description = "내용과 고정 상태를 변경합니다. notice와 pinned가 모두 null이면 공지를 삭제합니다.")
    fun updateNotice(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        noticeId: Long,
        request: UpdateChatRoomNoticeRequest,
    ): ResponseEntity<Void>

    @Operation(summary = "채팅방 공지 이력", description = "고정 공지와 고정하지 않은 공지를 각각 생성일 내림차순으로 반환합니다.")
    fun getNoticeHistory(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ChatRoomNoticeHistoryResponse

    @Operation(summary = "채팅 메시지 전송", description = "현재 참가자만 메시지를 보낼 수 있습니다.")
    fun sendMessage(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: SendChatMessageRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 사진 공유", description = "현재 참가자가 최대 20MB 이미지 한 장을 공유합니다.")
    fun shareImage(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        image: MultipartFile,
        caption: String?,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 관광 장소 공유", description = "현재 참가자가 TourismContent ID로 선택한 관광 장소 카드를 메시지로 공유합니다.")
    fun shareTourismContent(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: ShareTourismContentRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 만날 위치 공유", description = "호스트가 채팅방에 등록한 집합 위치 좌표와 상세 장소를 공유합니다.")
    fun shareLocation(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 투표 개최", description = "선택지는 2~5개이며 anonymous를 생략하면 익명 투표입니다.")
    fun createPoll(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: CreateChatPollRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 투표 참여 또는 선택 변경", description = "현재 참가자의 투표를 등록하거나 기존 선택지를 새 선택지로 변경합니다.")
    fun votePoll(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        messageId: Long,
        optionId: Long,
    ): ChatMessageResponse

    @Operation(summary = "채팅 투표 참여 취소", description = "현재 참가자가 해당 투표에서 선택한 항목을 취소합니다.")
    fun cancelPollVote(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        messageId: Long,
    ): ChatMessageResponse

    @Operation(summary = "채팅 정산 메모 공유", description = "송금 기능 없이 정산 내용을 메모 카드로 공유합니다.")
    fun shareSettlementMemo(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: CreateSettlementMemoRequest,
    ): ResponseEntity<ChatMessageResponse>

    @Operation(summary = "채팅 메시지 커서 조회", description = "beforeMessageId보다 오래된 메시지를 조회하며 응답 메시지는 오래된 순서로 반환합니다.")
    fun getMessages(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        beforeMessageId: Long?,
        limit: Int,
    ): ChatMessagePageResponse

    @Operation(summary = "현재 여행 로드맵", description = "확정된 여행 당일의 전체 장소 진행 상태와 현재·다음 일정을 반환합니다.")
    fun getCurrentRoadmap(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): CurrentTravelRoadmapResponse
}
