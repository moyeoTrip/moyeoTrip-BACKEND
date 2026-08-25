package kr.hanchae.moyeotrip.controller.weather

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.weather.response.GyeongbukWeatherResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "날씨", description = "경상북도 여행용 현재 시각 기준 날씨 API")
interface WeatherAPISpec {
    @Operation(
        summary = "경상북도 날씨 조회",
        description =
            "기상청 초단기예보로 경상북도 포항시 격자를 먼저 조회합니다. 데이터가 없거나 조회에 실패하면 경상북도 대표 도시인 안동시 격자로 자동 재시도합니다. " +
                "미세먼지 상태는 AirKorea 경북 실시간 측정값을 보완해 판단하며, 측정값을 받지 못하면 날씨 상태만 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "경상북도 날씨 조회 성공",
                content = [Content(schema = Schema(implementation = GyeongbukWeatherResponse::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "기상청 기본·대체 격자 모두에서 날씨 데이터를 받지 못함",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = """{"code":50203,"errorMessage":"기상청 날씨 데이터를 현재 조회할 수 없습니다."}""")],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}""")],
                    ),
                ],
            ),
        ],
    )
    fun getGyeongbukWeather(): GyeongbukWeatherResponse
}
