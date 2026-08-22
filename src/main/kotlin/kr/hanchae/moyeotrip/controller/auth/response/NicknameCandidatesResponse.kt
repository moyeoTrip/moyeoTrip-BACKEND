package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.NicknameColor

@Schema(description = "회원가입에 사용할 닉네임 후보와 일회성 선택 토큰")
data class NicknameCandidatesResponse(
    @field:Schema(
        description = "후보 선택을 검증하는 일회성 토큰. 회원가입 요청에 선택한 닉네임과 함께 전달합니다.",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val selectionToken: String,
    @field:ArraySchema(
        arraySchema = Schema(description = "서버가 생성한 서로 다른 닉네임 선택지 3개"),
        schema = Schema(implementation = NicknameCandidate::class),
        minItems = 3,
        maxItems = 3,
        uniqueItems = true,
    )
    val candidates: List<NicknameCandidate>,
    @field:Schema(
        description = "선택 토큰의 유효 시간(초)",
        example = "600",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val expiresInSeconds: Long,
)

@Schema(description = "선택 가능한 닉네임 후보 정보")
data class NicknameCandidate(
    @field:Schema(description = "형용사 + 동물 이름 + 0000~9999의 4자리 숫자로 구성된 선택 가능한 닉네임", example = "따스한 사슴 0000")
    val nickname: String,
    @field:Schema(description = "닉네임과 성향 설명의 기준이 되는 형용사", example = "따스한")
    val adjective: String,
    @field:Schema(description = "닉네임에 사용된 동물 이름", example = "사슴")
    val animal: String,
    @field:Schema(
        description = "프론트엔드 표현에 사용할 무작위 색상 코드",
        example = "RED",
        allowableValues = ["RED", "ORANGE", "YELLOW", "GREEN", "BLUE", "NAVY", "PURPLE", "PINK", "SKY_BLUE", "MINT"],
    )
    val color: NicknameColor,
    @field:Schema(description = "동물이나 숫자와 무관하게 형용사에 대응하는 여행 성향 설명", example = "처음 만난 사람에게도 다정하게 말을 건네요")
    val description: String,
)
