package kr.hanchae.moyeotrip.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.config.security.CustomUserDto
import kr.hanchae.moyeotrip.config.swagger.SwaggerTag
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoCustomTokenRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseCustomTokenResponse
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.LinkedProvidersResponse
import kr.hanchae.moyeotrip.controller.auth.response.NicknameCandidatesResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse
import org.springframework.http.ResponseEntity

@Tag(
    name = SwaggerTag.AUTH,
    description = "Firebase 기반 이메일·Apple·카카오 로그인, 회원가입, 인증 수단 연결 및 서비스 JWT 재발급 API",
)
interface AuthAPISpec {
    @Operation(
        summary = "닉네임 선택지 3개 생성",
        description = """
            `형용사 + 동물 이름 + 0000~9999의 4자리 숫자` 형식의 서로 다른 닉네임 3개를 서버에서 생성합니다.
            각 후보에는 형용사, 동물, 무작위 색상과 형용사에 대응하는 여행 성향 설명이 함께 포함됩니다.
            마음에 드는 후보가 없으면 이 API를 횟수 제한 없이 다시 호출해 새로운 3개를 받을 수 있습니다.
            selectionToken은 10분간 유효하며 회원가입 시 선택한 닉네임과 함께 제출해야 합니다.
        """,
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "닉네임 선택지 생성 성공",
                content = [
                    Content(
                        schema = Schema(implementation = NicknameCandidatesResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.NICKNAME_CANDIDATES)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "고유한 닉네임 후보를 생성하지 못함",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun generateNicknameCandidates(): NicknameCandidatesResponse

    @Operation(
        summary = "카카오 토큰을 Firebase Custom Token으로 교환",
        description = """
            카카오 SDK에서 받은 access token의 유효성과 app_id를 검증한 후 Firebase Custom Token을 발급합니다.
            클라이언트는 응답 토큰으로 Firebase signInWithCustomToken을 호출하고, 발급받은 Firebase ID Token으로 카카오 로그인을 진행합니다.
        """,
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Firebase Custom Token 발급 성공",
                content = [
                    Content(
                        schema = Schema(implementation = FirebaseCustomTokenResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.CUSTOM_TOKEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 본문 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 검증 실패", value = AuthSwaggerExamples.BAD_REQUEST),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 카카오 토큰 또는 다른 카카오 앱에서 발급된 토큰",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_KAKAO_APP)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "502",
                description = "Firebase Custom Token 발급 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.FIREBASE_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun createKakaoCustomToken(
        @RequestBody(description = "카카오 SDK가 발급한 access token", required = true)
        request: KakaoCustomTokenRequest,
    ): FirebaseCustomTokenResponse

    @Operation(
        summary = "Firebase 제공자 자동 판별 로그인",
        description = "Firebase ID Token의 sign_in_provider를 읽어 EMAIL, APPLE, KAKAO 중 하나로 판별합니다. 미가입이면 토큰 없이 isNewUser=true를 반환합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 결과. 기존 회원은 서비스 JWT, 신규 회원은 isNewUser=true 반환",
                content = [
                    Content(
                        schema = Schema(implementation = FirebaseLoginResponse::class),
                        examples = [
                            ExampleObject(
                                name = "기존 회원",
                                value = AuthSwaggerExamples.LOGIN_EXISTING,
                            ), ExampleObject(name = "신규 회원", value = AuthSwaggerExamples.LOGIN_NEW),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "지원하지 않는 Firebase 인증 제공자",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_PROVIDER)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Firebase ID Token이 유효하지 않거나 폐기됨",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ),
        ],
    )
    fun loginWithFirebase(
        @RequestBody(description = "Firebase SDK가 발급한 ID Token과 선택적 FCM Token", required = true)
        request: FirebaseLoginRequest,
    ): FirebaseLoginResponse

    @Operation(summary = "카카오 로그인", description = "Firebase Custom Token 로그인 이후 발급된 ID Token만 허용합니다. 다른 Firebase 제공자의 토큰은 거부합니다.")
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "카카오 로그인 결과",
                content = [
                    Content(
                        schema = Schema(implementation = FirebaseLoginResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.LOGIN_KAKAO)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "카카오 제공자 불일치",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_PROVIDER)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ),
        ],
    )
    fun loginWithKakao(
        @RequestBody(description = "카카오 Custom Token 로그인으로 받은 Firebase ID Token", required = true) request: FirebaseLoginRequest,
    ): FirebaseLoginResponse

    @Operation(summary = "이메일 로그인", description = "Firebase Email/Password 또는 Email Link 로그인으로 받은 ID Token을 검증합니다.")
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이메일 로그인 결과",
                content = [
                    Content(
                        schema = Schema(implementation = FirebaseLoginResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.LOGIN_EMAIL)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "이메일 제공자 불일치",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_PROVIDER)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ),
        ],
    )
    fun loginWithEmail(
        @RequestBody(description = "Firebase 이메일 로그인 ID Token", required = true) request: FirebaseLoginRequest,
    ): FirebaseLoginResponse

    @Operation(summary = "Apple 로그인", description = "Firebase Sign in with Apple로 받은 ID Token을 검증합니다.")
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Apple 로그인 결과",
                content = [
                    Content(
                        schema = Schema(implementation = FirebaseLoginResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.LOGIN_APPLE)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "Apple 제공자 불일치",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_PROVIDER)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ),
        ],
    )
    fun loginWithApple(
        @RequestBody(description = "Firebase Apple 로그인 ID Token", required = true) request: FirebaseLoginRequest,
    ): FirebaseLoginResponse

    @Operation(summary = "Firebase 제공자 자동 판별 회원가입", description = "Firebase 제공자를 자동 판별하고, 닉네임 후보 API에서 발급한 선택 토큰을 검증한 뒤 회원과 인증 수단을 등록합니다.")
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회원가입 완료",
                content = [
                    Content(
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.SERVICE_TOKENS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패 또는 지원하지 않는 제공자",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 검증 실패", value = AuthSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "닉네임 선택 오류", value = AuthSwaggerExamples.INVALID_NICKNAME_SELECTION),
                        ],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "409",
                description = "닉네임, 이메일 또는 인증 수단 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "닉네임 중복",
                                value = AuthSwaggerExamples.DUPLICATE_NICKNAME,
                            ), ExampleObject(name = "인증 수단 중복", value = AuthSwaggerExamples.IDENTITY_LINKED),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun signupWithFirebase(
        @RequestBody(description = "Firebase ID Token, 닉네임 선택 토큰, 선택한 닉네임과 선택적 FCM Token", required = true) request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse>

    @Operation(summary = "카카오 회원가입", description = "카카오 Custom Token 로그인의 Firebase ID Token으로 신규 회원을 생성합니다.")
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "카카오 회원가입 완료",
                content = [
                    Content(
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.SERVICE_TOKENS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패 또는 카카오 제공자 불일치",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 검증 실패", value = AuthSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "닉네임 선택 오류", value = AuthSwaggerExamples.INVALID_NICKNAME_SELECTION),
                        ],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "409",
                description = "닉네임 또는 인증 수단 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.DUPLICATE_NICKNAME)],
                    ),
                ],
            ),
        ],
    )
    fun signupWithKakao(
        @RequestBody(description = "카카오 Firebase ID Token과 서버가 발급한 닉네임 선택 정보", required = true) request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse>

    @Operation(
        summary = "이메일 회원가입",
        description = "클라이언트에서 Firebase 이메일 계정을 생성한 후 받은 ID Token으로 모여트립 회원가입을 완료합니다. 비밀번호는 서버에 전달하거나 저장하지 않습니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "이메일 회원가입 완료",
                content = [
                    Content(
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.SERVICE_TOKENS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패 또는 이메일 제공자 불일치",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 검증 실패", value = AuthSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "닉네임 선택 오류", value = AuthSwaggerExamples.INVALID_NICKNAME_SELECTION),
                        ],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "409",
                description = "닉네임, 이메일 또는 인증 수단 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.IDENTITY_LINKED)],
                    ),
                ],
            ),
        ],
    )
    fun signupWithEmail(
        @RequestBody(description = "이메일 Firebase ID Token과 서버가 발급한 닉네임 선택 정보", required = true) request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse>

    @Operation(summary = "Apple 회원가입", description = "Firebase Apple 로그인으로 받은 ID Token을 이용해 모여트립 회원가입을 완료합니다.")
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Apple 회원가입 완료",
                content = [
                    Content(
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.SERVICE_TOKENS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패 또는 Apple 제공자 불일치",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "요청 검증 실패", value = AuthSwaggerExamples.BAD_REQUEST),
                            ExampleObject(name = "닉네임 선택 오류", value = AuthSwaggerExamples.INVALID_NICKNAME_SELECTION),
                        ],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Firebase ID Token",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "409",
                description = "닉네임, 이메일 또는 인증 수단 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.IDENTITY_LINKED)],
                    ),
                ],
            ),
        ],
    )
    fun signupWithApple(
        @RequestBody(description = "Apple Firebase ID Token과 서버가 발급한 닉네임 선택 정보", required = true) request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse>

    @Operation(
        summary = "Firebase 인증 수단 연결",
        description = "로그인된 사용자에게 Firebase EMAIL 또는 APPLE 인증 수단을 추가합니다. 연결 후 해당 수단으로 동일 계정에 로그인할 수 있습니다.",
    )
    @SecurityRequirement(name = "Authorization")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 완료 및 전체 연결 제공자 반환",
                content = [
                    Content(
                        schema = Schema(implementation = LinkedProvidersResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.LINKED_PROVIDERS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "지원하지 않는 Firebase 제공자",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_PROVIDER)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "서비스 JWT 또는 Firebase ID Token이 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "409",
                description = "제공자 중복 또는 다른 사용자에게 이미 연결된 인증 수단",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "다른 사용자에게 연결됨",
                                value = AuthSwaggerExamples.IDENTITY_LINKED,
                            ), ExampleObject(name = "동일 제공자 중복", value = AuthSwaggerExamples.PROVIDER_LINKED),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun linkFirebaseProvider(
        @Parameter(hidden = true) principal: CustomUserDto,
        @RequestBody(description = "새로 연결할 Firebase 계정의 ID Token", required = true) request: FirebaseLoginRequest,
    ): LinkedProvidersResponse

    @Operation(summary = "카카오 인증 수단 연결", description = "로그인된 사용자에게 카카오 계정을 추가합니다. 카카오 토큰의 app_id까지 검증하며, 연결 후 카카오로 동일 계정에 로그인할 수 있습니다.")
    @SecurityRequirement(name = "Authorization")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "카카오 연결 완료",
                content = [
                    Content(
                        schema = Schema(implementation = LinkedProvidersResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.LINKED_PROVIDERS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "요청 본문 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "서비스 JWT, 카카오 토큰 또는 카카오 app_id 검증 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "409",
                description = "카카오 제공자 중복 또는 다른 사용자에게 이미 연결됨",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.IDENTITY_LINKED)],
                    ),
                ],
            ),
        ],
    )
    fun linkKakaoProvider(
        @Parameter(hidden = true) principal: CustomUserDto,
        @RequestBody(description = "새로 연결할 카카오 계정의 access token", required = true) request: KakaoCustomTokenRequest,
    ): LinkedProvidersResponse

    @Operation(summary = "연결된 로그인 제공자 조회", description = "현재 로그인 사용자에게 연결된 EMAIL, APPLE, KAKAO 제공자 목록을 반환합니다.")
    @SecurityRequirement(name = "Authorization")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "연결 제공자 조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = LinkedProvidersResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.LINKED_PROVIDERS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "401",
                description = "서비스 access token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.UNAUTHORIZED)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "404",
                description = "로그인 사용자 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getLinkedProviders(
        @Parameter(hidden = true) principal: CustomUserDto,
    ): LinkedProvidersResponse

    @Operation(
        summary = "서비스 토큰 재발급",
        description = "유효한 refresh token을 검증하고 rotate ID를 교체한 새로운 access/refresh token 쌍을 발급합니다. 이전 refresh token은 재사용할 수 없습니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "서비스 토큰 재발급 성공",
                content = [
                    Content(
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.SERVICE_TOKENS)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "400",
                description = "refresh token 누락, 만료, 위조 또는 이미 회전됨",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_REFRESH_TOKEN)],
                    ),
                ],
            ), ApiResponse(
                responseCode = "404",
                description = "토큰 사용자가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun refreshAccessToken(
        @RequestBody(description = "현재 유효한 서비스 refresh token", required = true) request: RefreshAccessTokenRequest,
    ): ServiceTokensResponse
}

private object AuthSwaggerExamples {
    const val NICKNAME_CANDIDATES =
        """{"selectionToken":"550e8400-e29b-41d4-a716-446655440000","candidates":[{"nickname":"따스한 사슴 1234","adjective":"따스한","animal":"사슴","color":"RED","description":"처음 만난 사람에게도 다정하게 말을 건네요"},{"nickname":"빠른 거북이 9999","adjective":"빠른","animal":"거북이","color":"BLUE","description":"빠른 템포로 여행을 알차게 즐겨요"},{"nickname":"다정한 수달 5271","adjective":"다정한","animal":"수달","color":"MINT","description":"함께하는 사람을 세심하게 살피고 마음을 나눠요"}],"expiresInSeconds":600}"""
    const val CUSTOM_TOKEN = """{"customToken":"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."}"""
    const val SERVICE_TOKENS = """{"accessToken":"eyJhbGciOiJIUzI1NiJ9.access...","refreshToken":"eyJhbGciOiJIUzI1NiJ9.refresh..."}"""
    const val LOGIN_EXISTING = """{"accessToken":"access-token","refreshToken":"refresh-token","isNewUser":false,"providerType":"EMAIL"}"""
    const val LOGIN_NEW = """{"accessToken":null,"refreshToken":null,"isNewUser":true,"providerType":"APPLE"}"""
    const val LOGIN_KAKAO = """{"accessToken":"access-token","refreshToken":"refresh-token","isNewUser":false,"providerType":"KAKAO"}"""
    const val LOGIN_EMAIL = """{"accessToken":"access-token","refreshToken":"refresh-token","isNewUser":false,"providerType":"EMAIL"}"""
    const val LOGIN_APPLE = """{"accessToken":"access-token","refreshToken":"refresh-token","isNewUser":false,"providerType":"APPLE"}"""
    const val LINKED_PROVIDERS = """{"providers":["EMAIL","APPLE","KAKAO"]}"""
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val INVALID_REFRESH_TOKEN = """{"code":40001,"errorMessage":"유효하지 않은 RefreshToken 입니다."}"""
    const val INVALID_PROVIDER = """{"code":40002,"errorMessage":"요청한 로그인 제공자와 Firebase 인증 정보가 일치하지 않습니다."}"""
    const val INVALID_NICKNAME_SELECTION = """{"code":40003,"errorMessage":"닉네임 선택이 만료되었거나 발급된 후보와 일치하지 않습니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val INVALID_FIREBASE_TOKEN = """{"code":40101,"errorMessage":"유효하지 않은 Firebase ID 토큰입니다."}"""
    const val INVALID_KAKAO_APP = """{"code":40103,"errorMessage":"다른 카카오 애플리케이션에서 발급된 액세스 토큰입니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val DUPLICATE_NICKNAME = """{"code":40900,"errorMessage":"이미 사용중인 닉네임입니다."}"""
    const val IDENTITY_LINKED = """{"code":40903,"errorMessage":"이미 다른 사용자에게 연결된 로그인 수단입니다."}"""
    const val PROVIDER_LINKED = """{"code":40904,"errorMessage":"해당 로그인 제공자가 이미 연결되어 있습니다."}"""
    const val FIREBASE_ERROR = """{"code":50200,"errorMessage":"Firebase 인증 처리에 실패했습니다."}"""
}
