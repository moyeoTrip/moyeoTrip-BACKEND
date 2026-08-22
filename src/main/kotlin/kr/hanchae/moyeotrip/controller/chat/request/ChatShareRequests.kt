package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "관광 장소 카드 공유 요청")
data class ShareTourismContentRequest(
    @field:Schema(description = "공유할 TourismContent ID", example = "126508")
    val contentId: Long,
)

@Schema(description = "채팅 투표 생성 요청")
data class CreateChatPollRequest(
    @field:Schema(description = "투표 질문", example = "점심 메뉴는 무엇으로 할까요?")
    @field:NotBlank
    @field:Size(max = 200)
    val question: String,
    @field:Schema(description = "투표 선택지 목록. 2~5개를 입력합니다.", example = "[\"한식\", \"카페\", \"분식\"]")
    @field:Size(min = 2, max = 5)
    val options: List<
        @NotBlank
        @Size(max = 100)
        String,
    >,
    @field:Schema(description = "익명 투표 여부. 생략하면 익명입니다.", example = "true")
    val anonymous: Boolean = true,
)

@Schema(description = "정산 메모 카드 공유 요청")
data class CreateSettlementMemoRequest(
    @field:Schema(description = "송금 기능이 없는 정산 메모 내용", example = "점심 45,000원 / 5명 = 1인 9,000원")
    @field:NotBlank
    @field:Size(max = 1000)
    val memo: String,
)
