package kr.hanchae.moyeotrip.controller.feed.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.feed.FeedReportReason
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "피드 목록 커서 조회 응답")
data class FeedPageResponse(
    @field:Schema(description = "조회한 피드 목록")
    val feeds: List<FeedResponse>,
    @field:Schema(description = "다음 페이지 조회에 사용할 마지막 피드 ID. 다음 페이지가 없으면 null", example = "95", nullable = true)
    val nextId: Long?,
)

@Schema(description = "여행 피드 상세 응답")
data class FeedResponse(
    @field:Schema(description = "피드 ID", example = "101")
    val feedId: Long,
    @field:Schema(description = "피드 작성자 정보")
    val author: FeedAuthorResponse,
    @field:Schema(description = "피드 본문", example = "주왕산 단풍이 정말 아름다웠어요!")
    val content: String,
    @field:Schema(description = "피드 공개 범위", example = "PUBLIC")
    val visibility: FeedVisibility,
    @field:Schema(description = "신고 누적으로 비공개 처리된 피드인지 여부", example = "false")
    val hiddenByReports: Boolean,
    @field:Schema(description = "최대 10장의 피드 첨부 사진 목록")
    val images: List<FeedImageResponse>,
    @field:Schema(description = "피드로 기록한 완료 여행 정보")
    val trip: FeedTripResponse,
    @field:Schema(description = "좋아요 수", example = "12")
    val likeCount: Long,
    @field:Schema(description = "댓글과 대댓글을 포함한 댓글 수", example = "3")
    val commentCount: Long,
    @field:Schema(description = "로그인 사용자의 좋아요 여부", example = "true")
    val liked: Boolean,
    @field:Schema(description = "피드 작성 일시", example = "2026-09-15T19:30:00")
    val createdAt: LocalDateTime,
)

@Schema(description = "피드 작성자 정보")
data class FeedAuthorResponse(
    @field:Schema(description = "작성자 사용자 ID", example = "12")
    val userId: Long,
    @field:Schema(description = "작성자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "작성자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
)

@Schema(description = "피드 첨부 사진 정보")
data class FeedImageResponse(
    @field:Schema(description = "피드 이미지 ID", example = "22")
    val imageId: Long,
    @field:Schema(description = "피드 이미지 URL")
    val imageUrl: String,
    @field:Schema(description = "피드 안의 이미지 표시 순서. 1부터 시작합니다.", example = "1")
    val sequence: Int,
)

@Schema(description = "피드에 기록된 여행 정보")
data class FeedTripResponse(
    @field:Schema(description = "완료한 여행 채팅방 ID", example = "101")
    val chatRoomId: Long,
    @field:Schema(description = "연결된 여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "여행 코스 제목", example = "주왕산 단풍길 코스")
    val courseTitle: String,
    @field:Schema(description = "여행 시작일", example = "2026-09-12", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일 여행이면 null", nullable = true, type = "string", format = "date")
    val endDate: LocalDate?,
    @field:Schema(description = "여행 코스의 방문 장소 목록")
    val places: List<FeedPlaceResponse>,
)

@Schema(description = "피드 여행 코스의 방문 장소 정보")
data class FeedPlaceResponse(
    @field:Schema(description = "TourismContent ID", example = "126508")
    val tourismContentId: Long,
    @field:Schema(description = "관광 장소명", example = "주산지")
    val title: String,
    @field:Schema(description = "관광 장소 위도. 좌표 미제공이면 null", nullable = true)
    val latitude: Double?,
    @field:Schema(description = "관광 장소 경도. 좌표 미제공이면 null", nullable = true)
    val longitude: Double?,
    @field:Schema(description = "여행 일차. 1부터 시작합니다.", example = "1")
    val dayNumber: Int,
    @field:Schema(description = "같은 일차 안의 방문 순서. 1부터 시작합니다.", example = "2")
    val sequence: Int,
    @field:Schema(description = "방문 예정 시각. 미지정이면 null", example = "14:00", nullable = true)
    val visitTime: LocalTime?,
)

@Schema(description = "피드 좋아요 토글 결과")
data class FeedLikeResponse(
    @field:Schema(description = "변경 후 로그인 사용자의 좋아요 여부", example = "true")
    val liked: Boolean,
    @field:Schema(description = "변경 후 피드의 전체 좋아요 수", example = "13")
    val likeCount: Long,
)

@Schema(description = "피드 신고 사유 선택지")
data class FeedReportReasonResponse(
    @field:Schema(description = "신고 요청에 전달할 사유 코드", example = "SPAM")
    val reason: FeedReportReason,
    @field:Schema(description = "화면에 표시할 신고 사유명", example = "스팸 또는 광고")
    val displayName: String,
)

@Schema(description = "피드 댓글 또는 대댓글 정보")
data class FeedCommentResponse(
    @field:Schema(description = "댓글 ID", example = "45")
    val commentId: Long,
    @field:Schema(description = "댓글 작성자 정보")
    val author: FeedAuthorResponse,
    @field:Schema(description = "댓글 본문", example = "다음에 저도 가보고 싶어요!")
    val content: String,
    @field:Schema(description = "댓글 작성 일시", example = "2026-09-15T20:00:00")
    val createdAt: LocalDateTime,
    @field:Schema(description = "이 댓글에 작성된 대댓글 목록")
    val replies: List<FeedCommentResponse> = emptyList(),
)
