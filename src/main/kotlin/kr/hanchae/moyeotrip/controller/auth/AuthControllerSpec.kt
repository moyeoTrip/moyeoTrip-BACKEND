package kr.hanchae.moyeotrip.controller.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.auth.request.KakaoLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.UserCreateRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.config.swagger.SwaggerTag
import kr.hanchae.moyeotrip.controller.auth.response.KakaoLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = SwaggerTag.AUTH)
interface AuthControllerSpec {
    @Operation(
        summary = "카카오 로그인 API",
        description = "카카오 로그인을 합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "카카오 로그인 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = KakaoLoginResponse::class),
                        examples = [
                            ExampleObject(
                                name = "기존 회원",
                                description = "기존 유저 - 토큰 반환",
                                value = """{"accessToken": "ACCESS_TOKEN_EXAMPLE",
                                    "refreshToken": "REFRESH_TOKEN_EXAMPLE","isNewUser": false}""",
                            ),
                            ExampleObject(
                                name = "신규 회원",
                                description = "신규 유저 - 신규 유저 여부 반환",
                                value = """{"accessToken": null,"refreshToken": null,"isNewUser": true}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun kakaoLogin(
        @RequestBody @Validated kakaoLoginRequest: KakaoLoginRequest,
    ): KakaoLoginResponse

    @Operation(
        summary = "카카오 신규 유저 회원가입",
        description = "카카오 로그인 후 신규 유저가 닉네임을 입력해 회원가입을 진행합니다. providerAccessToken은 카카오 로그인 성공 후 받은 provider accessToken을 사용합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "회원가입 성공 - 서비스 토큰 반환",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [
                            ExampleObject(
                                name = "성공 예시",
                                value = """{"accessToken": "ACCESS_TOKEN_EXAMPLE", "refreshToken": "REFRESH_TOKEN_EXAMPLE"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 존재하는 닉네임 또는 카카오 계정",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ServiceTokensResponse::class),
                        examples = [
                            ExampleObject(
                                name = "실패 예시",
                                value = """{"code": 40901, "errorMessage": "이미 존재하는 유저입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createKakaoUser(
        @RequestBody @Validated userCreateRequest: UserCreateRequest,
    ): ServiceTokensResponse

    @Operation(
        summary = "리프레시 토큰으로 엑세스 토큰 재발급",
        description = "리프레시 토큰으로 엑세스 토큰을 재발급합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "엑세스 토큰 재발급 성공"),
        ],
    )
    fun refreshAccessToken(request: RefreshAccessTokenRequest): ServiceTokensResponse
}
