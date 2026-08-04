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
import kr.hanchae.moyeotrip.controller.auth.request.KakaoAuthorizationCodeRequest
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
    description = "Firebase ID Token으로 EMAIL·GOOGLE·APPLE·KAKAO를 통합 처리하는 로그인, 회원가입, 인증 수단 연결 및 서비스 JWT 재발급 API",
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
        summary = "카카오 Web 인가 코드를 Firebase Custom Token으로 교환",
        description = """
            Kakao JavaScript SDK v2의 `Kakao.Auth.authorize()`가 redirect URI로 전달한 인가 코드를 처리합니다.
            서버가 Kakao REST API 키와 선택적 client secret으로 인가 코드를 access token으로 교환하고,
            토큰의 app_id를 검증한 뒤 Firebase Custom Token을 반환합니다.

            redirectUri는 인가 요청에 사용한 값, Kakao Developers에 등록한 값, 서버의 허용 목록 값과 정확히 같아야 합니다.
            REST API 키와 client secret을 Web 클라이언트에 전달하지 마세요. 인가 코드는 일회성이며 재사용할 수 없습니다.
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
                description = "요청 검증 실패 또는 허용되지 않은 redirect URI",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_KAKAO_REDIRECT_URI)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 인가 코드, 또는 다른 Kakao 앱에서 발급된 토큰",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.INVALID_KAKAO_AUTHORIZATION_CODE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "502",
                description = "Kakao 인증 서버 통신 또는 Firebase Custom Token 발급 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = AuthSwaggerExamples.KAKAO_AUTH_UNAVAILABLE)],
                    ),
                ],
            ),
        ],
    )
    fun createKakaoCustomTokenFromAuthorizationCode(
        @RequestBody(
            description = "Kakao JS SDK v2가 발급한 일회성 인가 코드와 인가 요청에 사용한 redirect URI",
            required = true,
        ) request: KakaoAuthorizationCodeRequest,
    ): FirebaseCustomTokenResponse

    @Operation(
        summary = "통합 로그인",
        description = """
            EMAIL, GOOGLE, APPLE, KAKAO가 공통으로 사용하는 단일 로그인 API입니다.
            요청에 providerType을 보내지 마세요. 서버가 검증된 Firebase ID Token의 sign_in_provider를 읽어 제공자를 판별합니다.

            **일반 로그인 흐름**
            1. FE에서 Firebase SDK로 이메일·Google·Apple 로그인을 완료합니다.
            2. Firebase SDK가 발급한 ID Token을 이 API의 idToken으로 전달합니다.

            **Kakao 로그인 흐름**
            1. Kakao SDK의 Access Token을 /api/v1/auth/firebase/kakao/custom-token에 전달합니다.
            2. 응답받은 Custom Token으로 Firebase signInWithCustomToken을 호출합니다.
            3. Firebase ID Token을 발급받아 이 API의 idToken으로 전달합니다.

            Custom Token 자체를 이 API에 보내면 안 됩니다. Firebase ID Token의 서명·만료·폐기 여부를 검증한 뒤,
            미가입 인증 수단이면 isNewUser=true와 USER_INFO_REQUIRED를 반환합니다.
            가입된 사용자는 서비스 JWT와 현재 회원가입 진행 상태를 반환합니다.
        """,
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 결과. 기존 사용자에게는 서비스 JWT와 회원가입 진행 상태를 반환",
                content = [
                    Content(
                        schema = Schema(implementation = FirebaseLoginResponse::class),
                        examples = [
                            ExampleObject(
                                name = "기존 회원",
                                value = AuthSwaggerExamples.LOGIN_EXISTING,
                            ),
                            ExampleObject(name = "프로필 선택 필요", value = AuthSwaggerExamples.LOGIN_PROFILE_REQUIRED),
                            ExampleObject(name = "신규 회원", value = AuthSwaggerExamples.LOGIN_NEW),
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
    fun login(
        @RequestBody(description = "로그인 완료 후 Firebase SDK가 발급한 ID Token과 선택적 FCM Token. Kakao Custom Token 자체는 허용하지 않습니다.", required = true)
        request: FirebaseLoginRequest,
    ): FirebaseLoginResponse

    @Operation(
        summary = "통합 회원가입",
        description = """
            EMAIL, GOOGLE, APPLE, KAKAO가 공통으로 사용하는 단일 회원가입 API입니다.
            providerType은 요청하지 않으며, 서버가 검증된 Firebase ID Token에서 실제 제공자를 판별해 저장합니다.

            **FE 호출 순서**
            1. /api/v1/auth/login에서 isNewUser=true와 USER_INFO_REQUIRED를 확인합니다.
            2. /api/v1/auth/nickname-candidates에서 후보와 selectionToken을 발급받습니다.
            3. 선택한 닉네임·성별·생년월일과 Firebase ID Token을 이 API에 전달합니다.
            4. 응답의 PROFILE_IMAGE_REQUIRED에 따라 프로필 이미지 생성·선택 단계를 이어갑니다.

            Kakao도 Custom Token으로 Firebase 로그인한 뒤 받은 Firebase ID Token을 사용합니다.
            이미 다른 사용자에게 연결된 인증 수단이나 기존 이메일 계정을 자동 병합하지 않습니다.
        """,
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "닉네임 등록 완료 및 프로필 이미지 선택 단계 진입",
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
    fun signup(
        @RequestBody(
            description = "모든 제공자가 공통으로 사용하는 회원가입 정보. idToken에는 Firebase ID Token을 전달하며 Kakao Custom Token 자체는 허용하지 않습니다.",
            required = true,
        ) request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse>

    @Operation(
        summary = "로그인 제공자 추가",
        description = """
            현재 로그인된 사용자에게 EMAIL, GOOGLE, APPLE 또는 KAKAO 인증 수단을 추가하는 단일 API입니다.
            Authorization 헤더에는 현재 사용자의 서비스 Access Token을,
            요청 본문의 idToken에는 새로 연결할 계정의 Firebase ID Token을 전달합니다.

            **EMAIL·Google·Apple 연결**
            FE에서 새 제공자로 Firebase 로그인을 완료하고 발급받은 ID Token을 전달합니다.

            **Kakao 연결**
            1. Kakao Access Token을 /api/v1/auth/firebase/kakao/custom-token에서 Custom Token으로 교환합니다.
            2. Firebase signInWithCustomToken으로 로그인합니다.
            3. 발급받은 Firebase ID Token을 이 API에 전달합니다.

            서버는 Firebase 토큰에서 제공자를 직접 판별합니다. 같은 제공자를 현재 사용자에게 중복 연결하거나,
            이미 다른 사용자에게 연결된 제공자 계정을 가져오는 요청은 거부합니다.
        """,
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
                        examples = [
                            ExampleObject(name = "서비스 인증 실패", value = AuthSwaggerExamples.UNAUTHORIZED),
                            ExampleObject(name = "연결할 Firebase 토큰 검증 실패", value = AuthSwaggerExamples.INVALID_FIREBASE_TOKEN),
                        ],
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
    fun linkProvider(
        @Parameter(hidden = true) principal: CustomUserDto,
        @RequestBody(description = "새로 연결할 계정으로 Firebase 로그인한 뒤 발급받은 ID Token", required = true) request: FirebaseLoginRequest,
    ): LinkedProvidersResponse

    @Operation(summary = "연결된 로그인 제공자 조회", description = "현재 로그인 사용자에게 연결된 EMAIL, APPLE, KAKAO, GOOGLE 제공자 목록을 반환합니다.")
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
    const val SERVICE_TOKENS =
        """{"accessToken":"eyJhbGciOiJIUzI1NiJ9.access...","refreshToken":"eyJhbGciOiJIUzI1NiJ9.refresh...",""" +
            """"signupState":"PROFILE_IMAGE_REQUIRED"}"""
    const val LOGIN_EXISTING =
        """{"accessToken":"access-token","refreshToken":"refresh-token","isNewUser":false,""" +
            """"signupState":"SIGNUP_COMPLETE","providerType":"EMAIL"}"""
    const val LOGIN_PROFILE_REQUIRED =
        """{"accessToken":"access-token","refreshToken":"refresh-token","isNewUser":false,""" +
            """"signupState":"PROFILE_IMAGE_REQUIRED","providerType":"EMAIL"}"""
    const val LOGIN_NEW =
        """{"accessToken":null,"refreshToken":null,"isNewUser":true,""" +
            """"signupState":"USER_INFO_REQUIRED","providerType":"APPLE"}"""
    const val LINKED_PROVIDERS = """{"providers":["EMAIL","APPLE","KAKAO","GOOGLE"]}"""
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val INVALID_REFRESH_TOKEN = """{"code":40001,"errorMessage":"유효하지 않은 RefreshToken 입니다."}"""
    const val INVALID_PROVIDER = """{"code":40002,"errorMessage":"지원하지 않거나 유효하지 않은 Firebase 로그인 제공자입니다."}"""
    const val INVALID_NICKNAME_SELECTION = """{"code":40003,"errorMessage":"닉네임 선택이 만료되었거나 발급된 후보와 일치하지 않습니다."}"""
    const val UNAUTHORIZED = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}"""
    const val INVALID_FIREBASE_TOKEN = """{"code":40101,"errorMessage":"유효하지 않은 Firebase ID 토큰입니다."}"""
    const val INVALID_KAKAO_APP = """{"code":40103,"errorMessage":"다른 카카오 애플리케이션에서 발급된 액세스 토큰입니다."}"""
    const val INVALID_KAKAO_REDIRECT_URI = """{"code":40004,"errorMessage":"허용되지 않은 카카오 redirect URI입니다."}"""
    const val INVALID_KAKAO_AUTHORIZATION_CODE = """{"code":40104,"errorMessage":"유효하지 않거나 만료된 카카오 인가 코드입니다."}"""
    const val KAKAO_AUTH_UNAVAILABLE = """{"code":50202,"errorMessage":"카카오 인증 서버와 통신하지 못했습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
    const val DUPLICATE_NICKNAME = """{"code":40900,"errorMessage":"이미 사용중인 닉네임입니다."}"""
    const val IDENTITY_LINKED = """{"code":40903,"errorMessage":"이미 다른 사용자에게 연결된 로그인 수단입니다."}"""
    const val PROVIDER_LINKED = """{"code":40904,"errorMessage":"해당 로그인 제공자가 이미 연결되어 있습니다."}"""
    const val FIREBASE_ERROR = """{"code":50200,"errorMessage":"Firebase 인증 처리에 실패했습니다."}"""
}
