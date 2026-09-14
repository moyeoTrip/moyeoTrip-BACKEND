package kr.hanchae.moyeotrip.controller.terms

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermDetailResponse
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermSummaryResponse
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "약관", description = "약관·개인정보 처리방침 조회 API")
interface TermsAPISpec {
    @Operation(
        summary = "현재 회원가입 약관 목록 조회",
        description = "현재 활성화된 약관의 ID, 제목, 필수 동의 여부를 반환합니다. 회원가입 전 이 목록을 조회하고, 필수 약관 ID를 모두 agreedTermIds에 포함해야 합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "현재 활성 회원가입 약관 목록",
                content = [Content(schema = Schema(implementation = AgreementTermSummaryResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "약관 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TermsSwaggerExamples.INTERNAL_SERVER_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun getTerms(): List<AgreementTermSummaryResponse>

    @Operation(
        summary = "회원가입 약관 상세 조회",
        description = "선택한 약관의 제목, 필수 동의 여부, 버전과 Markdown 형식 본문을 반환합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "약관 상세",
                content = [Content(schema = Schema(implementation = AgreementTermDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "현재 활성 상태인 약관을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TermsSwaggerExamples.AGREEMENT_TERM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getTerm(
        @Parameter(description = "조회할 현재 활성 약관 ID", example = "1") termId: Long,
    ): AgreementTermDetailResponse

    @Operation(
        summary = "약관 현재 판본 상세 조회 (종류로)",
        description =
            "약관 종류로 현재 활성 판본의 Markdown 본문을 반환합니다. 설정 화면처럼 termId를 모르는 곳에서 쓰세요 " +
                "(termId는 판본이 바뀔 때마다 새로 발급되므로 클라이언트가 보관할 수 없습니다). " +
                "가입 목록에 오지 않는 PRIVACY_POLICY(개인정보 처리방침)도 이 API로 조회합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "약관 현재 판본 상세",
                content = [Content(schema = Schema(implementation = AgreementTermDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 종류의 활성 약관이 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TermsSwaggerExamples.AGREEMENT_TERM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getLatestTerm(
        @Parameter(
            description = "약관 종류. 제목이 아니라 이 값으로 찾습니다.",
            example = "PRIVACY_POLICY",
        ) code: AgreementTermCode,
    ): AgreementTermDetailResponse

    @Operation(
        summary = "약관 판본 이력 조회",
        description =
            "한 종류 약관의 지난 판본까지 최신순으로 반환합니다. 현재 활성 판본이 첫 번째입니다. " +
                "본문은 각 termId로 상세 조회하세요. 판본이 하나뿐이면 그 하나만 반환합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "약관 판본 이력 (최신순)",
                content = [Content(schema = Schema(implementation = AgreementTermSummaryResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 종류의 약관이 하나도 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TermsSwaggerExamples.AGREEMENT_TERM_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getTermHistory(
        @Parameter(
            description = "약관 종류. 제목이 아니라 이 값으로 찾습니다.",
            example = "SERVICE",
        ) code: AgreementTermCode,
    ): List<AgreementTermSummaryResponse>
}

private object TermsSwaggerExamples {
    const val AGREEMENT_TERM_NOT_FOUND = """{"code":40413,"errorMessage":"현재 활성 상태인 약관을 찾을 수 없습니다."}"""
    const val INTERNAL_SERVER_ERROR = """{"code":50000,"errorMessage":"서버에러입니다."}"""
}
