package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.config.swagger.SwaggerTag
import kr.hanchae.moyeotrip.controller.user.request.ProfileImageSelectionRequest
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
import kr.hanchae.moyeotrip.controller.user.response.MyProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidatesResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageGenerationResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageSelectionResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionsResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(
    name = SwaggerTag.USER,
    description = "로그인 사용자의 프로필 이미지 등 사용자 정보 관리 API",
)
interface UserAPISpec {
    @Operation(
        summary = "내 프로필 조회",
        description = "자기소개, 여행 스타일, 관심 경북 지역, 생년월일, 성별과 기본 알림 수신 설정을 포함한 내 프로필을 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 프로필 조회 성공",
                content = [Content(schema = Schema(implementation = MyProfileResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getProfile(
        @Parameter(hidden = true) userId: Long,
    ): MyProfileResponse

    @Operation(
        summary = "내 프로필 수정",
        description = "자기소개, 여행 스타일 ID 목록, 관심 지역 ID 목록, 생년월일과 성별을 변경합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 프로필 수정 성공",
                content = [Content(schema = Schema(implementation = MyProfileResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "입력값, 최소 가입 연령, 여행 스타일 또는 관심 지역 ID가 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 본문 또는 선택 ID 오류", value = UserSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "최소 가입 연령 미달", value = UserSwaggerExamples.MINIMUM_SIGNUP_AGE_NOT_MET),
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
                        examples = [ExampleObject(value = UserSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun updateProfile(
        @Parameter(hidden = true) userId: Long,
        @RequestBody(description = "변경할 프로필 정보", required = true) request: UpdateProfileRequest,
    ): MyProfileResponse

    @Operation(
        summary = "프로필 선택 항목 조회",
        description = "프로필 수정 화면에 필요한 여행 스타일과 관심 지역 선택지를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "프로필 선택 항목 조회 성공",
                content = [Content(schema = Schema(implementation = ProfileOptionsResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "프로필 선택 항목 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.INTERNAL_SERVER_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun getProfileOptions(): ProfileOptionsResponse

    @Operation(
        summary = "회원 탈퇴",
        description = """
            로그인한 사용자의 서비스 계정을 즉시 영구 삭제합니다(하드 삭제).
            사용자 정보와 연결된 로그인 수단, 생성한 프로필 이미지 후보의 DB 레코드가 함께 삭제됩니다.
            DB 삭제가 커밋된 뒤 객체 저장소의 프로필 이미지와 Refresh Token 캐시도 정리됩니다.
            카카오·Apple·Google 등 외부 제공자의 계정 자체는 삭제하지 않습니다.
            성공 이후 기존 Access Token으로 요청하면 사용자를 찾을 수 없어 인증되지 않으며,
            기존 Refresh Token도 더 이상 갱신에 사용할 수 없습니다. 삭제한 데이터는 복구할 수 없습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "회원 탈퇴 및 서비스 계정 영구 삭제 성공. 응답 본문 없음",
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "이미 탈퇴했거나 로그인 사용자가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun withdraw(
        @Parameter(hidden = true) userId: Long,
    )

    @Operation(
        summary = "AI 프로필 이미지 후보 생성",
        description = """
            로그인 사용자의 닉네임 형용사·동물·색상으로 1:1 AI 프로필 이미지 후보 한 장을 생성합니다.
            생성된 이미지는 후보로만 영구 보관되며 현재 프로필 이미지가 자동으로 변경되지 않습니다.
            프롬프트는 서버가 전적으로 구성하고, 귀여운 비사실적 의인화 동물의 상반신 구도로 생성합니다.
            문자, 숫자, 로고와 워터마크는 금지합니다.
            성공한 생성은 사용자당 평생 최대 3회이며 동시 요청도 하나씩 직렬화됩니다.
            PROFILE_IMAGE_REQUIRED 상태의 사용자도 회원가입을 이어서 진행하기 위해 호출할 수 있습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "후보 이미지 생성 및 보관 성공. 현재 프로필에는 아직 적용되지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ProfileImageGenerationResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.PROFILE_IMAGE_GENERATED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "닉네임 설정 전 단계라 이미지를 생성할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_INFO_REQUIRED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "429",
                description = "사용자당 최대 생성 횟수 3회를 모두 사용함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.GENERATION_LIMIT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "502",
                description = "OpenAI GPT Image 이미지 생성 실패 또는 빈 이미지 응답",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.PROFILE_IMAGE_GENERATION_FAILED)],
                    ),
                ],
            ),
        ],
    )
    fun generateProfileImage(
        @Parameter(hidden = true) userId: Long,
    ): ProfileImageGenerationResponse

    @Operation(
        summary = "생성한 프로필 이미지 후보 조회",
        description = """
            현재 사용자가 지금까지 생성한 이미지 후보를 생성 순서대로 반환합니다.
            각 후보의 selected 값으로 현재 프로필 적용 여부를 알 수 있습니다.
            앱을 종료하거나 회원가입을 중단했다가 다시 로그인한 경우에도 기존 후보를 조회할 수 있습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "후보 목록 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = ProfileImageCandidatesResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.PROFILE_IMAGE_CANDIDATES)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "닉네임 설정 전 단계",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_INFO_REQUIRED)],
                    ),
                ],
            ),
        ],
    )
    fun getProfileImages(
        @Parameter(hidden = true) userId: Long,
    ): ProfileImageCandidatesResponse

    @Operation(
        summary = "프로필 이미지 후보 선택",
        description = """
            현재 사용자가 생성한 후보 중 하나를 실제 프로필 이미지로 선택합니다.
            다른 사용자의 이미지 ID는 선택할 수 없습니다.
            PROFILE_IMAGE_REQUIRED 상태에서는 이 API가 성공한 시점에 회원가입이 SIGNUP_COMPLETE로 전환됩니다.
            이후 다른 후보를 다시 선택해도 생성 횟수는 차감되지 않으며 기존 후보 이미지도 삭제되지 않습니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "프로필 이미지 선택 및 적용 성공",
                content = [
                    Content(
                        schema = Schema(implementation = ProfileImageSelectionResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.PROFILE_IMAGE_SELECTED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "이미지 ID 형식 오류",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자 또는 본인이 생성한 후보 이미지가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "로그인 사용자 없음", value = UserSwaggerExamples.USER_NOT_FOUND),
                            ExampleObject(name = "선택할 프로필 이미지 후보 없음", value = UserSwaggerExamples.PROFILE_IMAGE_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "닉네임 설정 전 단계",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = UserSwaggerExamples.USER_INFO_REQUIRED)],
                    ),
                ],
            ),
        ],
    )
    fun selectProfileImage(
        @Parameter(hidden = true) userId: Long,
        @RequestBody(
            description = "선택할 본인 소유의 프로필 이미지 후보 ID",
            required = true,
        ) request: ProfileImageSelectionRequest,
    ): ProfileImageSelectionResponse
}

private object UserSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val USER_INFO_REQUIRED = """{"code":40902,"errorMessage":"추가 정보 입력이 필요합니다.(닉네임, 성별, 생년월일)"}"""
    const val INTERNAL_SERVER_ERROR = """{"code":50000,"errorMessage":"서버에러입니다."}"""
    const val PROFILE_IMAGE_GENERATED =
        """{"candidate":{"profileImageId":12,"profileImageUrl":"https://cdn.example.com/user/profile/image/generated.png","selected":false},"generationCount":1,"remainingGenerationCount":2,"signupState":"PROFILE_IMAGE_REQUIRED"}"""
    const val PROFILE_IMAGE_CANDIDATES =
        """{"candidates":[{"profileImageId":12,"profileImageUrl":"https://cdn.example.com/user/profile/image/first.png","selected":false},{"profileImageId":15,"profileImageUrl":"https://cdn.example.com/user/profile/image/second.png","selected":true}],"generationCount":2,"remainingGenerationCount":1,"signupState":"SIGNUP_COMPLETE"}"""
    const val PROFILE_IMAGE_SELECTED =
        """{"selectedImage":{"profileImageId":15,"profileImageUrl":"https://cdn.example.com/user/profile/image/second.png","selected":true},"signupState":"SIGNUP_COMPLETE"}"""
    const val PROFILE_IMAGE_NOT_FOUND =
        """{"code":40401,"errorMessage":"선택할 수 있는 프로필 이미지를 찾을 수 없습니다."}"""
    const val MINIMUM_SIGNUP_AGE_NOT_MET = """{"code":40011,"errorMessage":"만 20세 이상만 가입할 수 있습니다."}"""
    const val GENERATION_LIMIT =
        """{"code":42900,"errorMessage":"프로필 이미지는 사용자당 최대 3번까지 생성할 수 있습니다."}"""
    const val PROFILE_IMAGE_GENERATION_FAILED =
        """{"code":50201,"errorMessage":"프로필 이미지 생성에 실패했습니다."}"""
}
